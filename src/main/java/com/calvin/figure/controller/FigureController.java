package com.calvin.figure.controller;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.calvin.figure.CalUtility;
import com.calvin.figure.entity.Figure;
import com.calvin.figure.entity.QFigure;
import com.calvin.figure.repository.FigureRepository;
import com.calvin.figure.repository.FileRepository;
import com.calvin.figure.service.FigureService;
import com.calvin.figure.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("figures")
public class FigureController {

	private static final Logger logger = LoggerFactory.getLogger(FigureController.class);

	@Autowired
	private FigureRepository figureRepository;
	@Autowired
	private FileRepository fileRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private FigureService figureService;
	@Autowired
	private JPAQueryFactory jPAQueryFactory;
	@Autowired
	private CalUtility calUtility;

	@GetMapping
	public ResponseEntity<Map<String, Object>> find(@RequestParam(required = false) Integer offset,
			@RequestParam(required = false) Integer limit, @RequestParam(required = false) String keyword) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"figure:?:r" ?表示有任意一个满足即可
		userService.checkAny("figure", 0b01, auth);
		var q = QFigure.figure;
		var jPAQ = jPAQueryFactory.selectFrom(q);
		if (offset == null && limit != null)
			offset = 0;
		if (offset != null && limit == null)
			limit = 10;
		if (keyword != null && !keyword.isEmpty()) {
			String kw = "%" + keyword + "%";
			jPAQ.where(q.name.likeIgnoreCase(kw).or(q.remark.likeIgnoreCase(kw)));
		}
		boolean paged = offset != null && limit != null;
		if (paged) {
			jPAQ.offset(offset);
			jPAQ.limit(limit);
		}
		var rows = jPAQ.fetch();
		Set<String> perms = calUtility.getFields(auth, 0b01, "figure");
		CalUtility.copyFields(rows, perms);
		Map<String, Object> body = new HashMap<>();
		if (paged) {
			body.put("offset", offset + rows.size());
			body.put("limit", limit);
		}
		body.put("data", rows);
		return ResponseEntity.ok(body);
	}

	@GetMapping("{id}")
	public ResponseEntity<Map<String, Object>> findById(@PathVariable("id") Integer id) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		userService.checkAny("figure", 0b01, auth);
		var opt = figureRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		var value = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b01, "figure");
		CalUtility.copyFields(value, perms);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value);
		return ResponseEntity.ok(body);
	}

	private void validate(MultipartFile portrait, Figure value, JsonNode json) {
		Pattern re = Pattern.compile("^(-?)([0-9]{1,6})(?:/([0-9]{1,2})(?:/([0-9]{1,2}))?)?$");
		if (json == null || json.has("name")) {
			if (value.getName() == null || value.getName().isEmpty())
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【名字】字段必传");
		}
		if (json == null || json.has("birthday")) {
			if (value.getBirthday() == null || value.getBirthday().isEmpty())
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【生辰】字段必传");
			Matcher matcher = re.matcher(value.getBirthday());
			if (!matcher.find())
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【生辰】格式不对");
			if (matcher.group(3) != null) {
				var m = Integer.parseInt(matcher.group(3));
				if (m < 1 || m > 12)
					throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【生辰】月1-12");
			}
			if (matcher.group(4) != null) {
				var d = Integer.parseInt(matcher.group(4));
				if (d < 1 || d > 31)
					throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【生辰】日1-31");
			}
		}
		if (value.getDeathday() != null) {
			if (value.getDeathday().isEmpty())
				value.setDeathday(null);
			else {
				Matcher matcher = re.matcher(value.getDeathday());
				if (!matcher.find())
					throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【忌辰】格式不对");
				if (matcher.group(3) != null) {
					var m = Integer.parseInt(matcher.group(3));
					if (m < 1 || m > 12)
						throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【忌辰】月1-12");
				}
				if (matcher.group(4) != null) {
					var d = Integer.parseInt(matcher.group(4));
					if (d < 1 || d > 31)
						throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【忌辰】日1-31");
				}
			}
		}
		if (portrait != null) {
			if (!portrait.getContentType().startsWith("image/"))
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "肖像必须是图片");
			if (portrait.getSize() > 1048576)
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "肖像图片大小不能超过1MB");
		} else if (json == null || json.has("portrait")) {
			var portrait1 = value.getPortrait();
			if (portrait1 != null)
				if (value.getPortrait().getId() == null)
					throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "肖像文件id不能为空");
				else {
					var opt = fileRepository.findById(portrait1.getId());
					if (opt.isEmpty())
						throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "肖像文件不存在");
					else {
						var f = opt.get();
						if (!f.getContentType().startsWith("image/"))
							throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "肖像必须是图片");
						if (f.getSize() > 1048576)
							throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "肖像图片大小不能超过1MB");
						value.setPortrait(f);
					}
				}
		}
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> add(MultipartFile portrait, @RequestPart Figure value)
			throws IOException {
		validate(portrait, value, null);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证所有不能为空的字段权限
		userService.check("figure", "id", 0b10, auth);
		userService.check("figure", "name", 0b10, auth);
		userService.check("figure", "birthday", 0b10, auth);
		value.setId(null); // 防止通过这个接口进行修改。
		Set<String> perms = calUtility.getFields(auth, 0b10, "figure");
		CalUtility.copyFields(value, perms);
		var value1 = figureService.add(portrait, value);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/figures/" + value1.getId())).body(body);
	}

	@PutMapping("{id}")
	public ResponseEntity<Map<String, Object>> edit(@PathVariable("id") Integer id, MultipartFile portrait,
			@RequestPart Figure value) throws IOException {
		validate(portrait, value, null);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"role:*:w" *表示要全部满足
		userService.checkAll("figure", 0b10, auth);
		var opt = figureRepository.findById(id);
		if (opt.isEmpty())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		var target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "figure");
		CalUtility.copyFields(target, value, perms, Set.of("*"));
		var value1 = figureService.edit(portrait, target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/figures/" + id)).body(body);
	}

	@PatchMapping("{id}")
	public ResponseEntity<Map<String, Object>> partialEdit(@PathVariable("id") Integer id, MultipartFile portrait,
			@RequestPart JsonNode json) throws IOException {
		Figure value;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			value = objectMapper.readValue(json.toString(), Figure.class);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
		}
		validate(portrait, value, json);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证前端传过来的每一个字段名的权限。
		var it = json.fieldNames();
		Set<String> nulls = new HashSet<>();
		while (it.hasNext()) {
			String key = it.next();
			nulls.add(key);
			userService.check("figure", key, 0b10, auth);
		}
		var opt = figureRepository.findById(id);
		if (opt.isEmpty())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		var target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "figure");
		CalUtility.copyFields(target, value, perms, nulls);
		var value1 = figureService.edit(portrait, target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/figures/" + id)).body(body);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer id) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"figure:*:w" *表示要全部满足
		userService.checkAll("figure", 0b10, auth);
		figureRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("exist")
	public ResponseEntity<Map<String, Object>> exist(Figure value, String matchMode, Integer excludeId) {
		Example<Figure> example;
		if ("any".equals(matchMode)) {
			ExampleMatcher matcher = ExampleMatcher.matchingAny();
			example = Example.of(value, matcher);
		} else
			example = Example.of(value);
		boolean isExist;
		if (excludeId == null)
			isExist = figureRepository.exists(example);
		else {
			var rows = figureRepository.findAll(example);
			isExist = rows.size() == 1 && !rows.get(0).getId().equals(excludeId) || rows.size() > 1;
		}
		Map<String, Object> body = new HashMap<>();
		body.put("data", isExist);
		return ResponseEntity.ok(body);
	}
}

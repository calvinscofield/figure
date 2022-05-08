package com.calvin.figure.controller;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.calvin.figure.CalUtility;
import com.calvin.figure.entity.File;
import com.calvin.figure.entity.QFile;
import com.calvin.figure.entity.User;
import com.calvin.figure.repository.FileRepository;
import com.calvin.figure.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("files")
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	@Autowired
	private FileRepository fileRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private JPAQueryFactory jPAQueryFactory;
	@Autowired
	private CalUtility calUtility;

	@GetMapping
	public ResponseEntity<Map<String, Object>> find(@RequestParam(required = false) Integer offset,
			@RequestParam(required = false) Integer limit, @RequestParam(required = false) String keyword) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"file:?:r" ?表示有任意一个满足即可
		userService.checkAny("file", 0b01, auth);
		// retrieve@creator.id=$me
		QFile q = QFile.file;
		var jPAQ = jPAQueryFactory.selectFrom(q);
		if (offset == null && limit != null)
			offset = 0;
		if (offset != null && limit == null)
			limit = 10;
		if (keyword != null && !keyword.isEmpty()) {
			String kw = "%" + keyword + "%";
			jPAQ.where(q.filename.likeIgnoreCase(kw).or(q.name.likeIgnoreCase(kw)).or(q.contentType.likeIgnoreCase(kw))
					.or(q.remark.likeIgnoreCase(kw)));
		}
		//User me = (User) auth.getPrincipal();
		//jPAQ.where(q.creator.id.eq(me.getId()))
		boolean paged = offset != null && limit != null;
		if (paged) {
			jPAQ.offset(offset);
			jPAQ.limit(limit);
		}
		List<File> rows = jPAQ.fetch();
		Set<String> perms = calUtility.getFields(auth, 0b01, "file");
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
		userService.checkAny("file", 0b01, auth);
		Optional<File> opt = fileRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		File value = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b01, "file");
		CalUtility.copyFields(value, perms);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value);
		return ResponseEntity.ok(body);
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> add(MultipartFile[] file, File value) throws IOException {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (file == null)
			throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【file】必传");
		// 验证所有不能为空的字段权限
		userService.check("file", "id", 0b10, auth);
		userService.check("file", "url", 0b10, auth);
		userService.check("file", "contentType", 0b10, auth);
		List<File> values = new ArrayList<>();
		for (int i = 0; i < file.length; i++) {
			File value1 = new File();
			if (value != null) {
				value1.setName(value.getName());
				value1.setRemark(value.getRemark());
			}
			calUtility.saveFile(file[i], value1);
			values.add(value1);
		}
		Set<String> perms = calUtility.getFields(auth, 0b10, "file");
		CalUtility.copyFields(value, perms);
		Map<String, Object> body = new HashMap<>();
		body.put("data", fileRepository.saveAll(values));
		return ResponseEntity.ok(body);
	}

	@PutMapping("{id}")
	public ResponseEntity<Map<String, Object>> edit(@PathVariable("id") Integer id, @RequestBody File value) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"file:*:w" *表示要全部满足
		userService.checkAll("file", 0b10, auth);
		Optional<File> opt = fileRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		File target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "file");
		CalUtility.copyFields(target, value, perms, true);
		File file = fileRepository.findById(id).get();
		target.setUrl(file.getUrl());
		target.setContentType(file.getContentType());
		target.setSize(file.getSize());
		target.setFilename(file.getFilename());
		File value1 = fileRepository.save(target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/files/" + id)).body(body);
	}

	@PatchMapping("{id}")
	public ResponseEntity<Map<String, Object>> partialEdit(@PathVariable("id") Integer id, @RequestBody JsonNode json) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证前端传过来的每一个字段名的权限。
		var it = json.fieldNames();
		while (it.hasNext()) {
			userService.check("file", it.next(), 0b10, auth);
		}
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		File value;
		try {
			value = objectMapper.readValue(json.toString(), File.class);
			value.setUrl(null);
			value.setContentType(null);
			value.setSize(null);
			value.setFilename(null);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
		}
		Optional<File> opt = fileRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		File target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "file");
		CalUtility.copyFields(target, value, perms, false);
		File value1 = fileRepository.save(target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/files/" + id)).body(body);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer id) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		User me = (User) auth.getPrincipal();
		// 验证权限"file:*:w" *表示要全部满足
		userService.checkAll("file", 0b10, auth);
		// delete@creator.id=$me
		File file = fileRepository.findById(id).get();
		// if (!file.getCreator().getId().equals(me.getId())) {
		// 	throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "只能删除自己上传的");
		// }
		String url = file.getUrl();
		fileRepository.deleteById(id);
		calUtility.delFile(url);
		return ResponseEntity.noContent().build();
	}
}

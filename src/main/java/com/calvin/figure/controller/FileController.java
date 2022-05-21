package com.calvin.figure.controller;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import com.calvin.figure.CalUtility;
import com.calvin.figure.entity.File;
import com.calvin.figure.entity.QFile;
import com.calvin.figure.repository.FileRepository;
import com.calvin.figure.service.FileService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("files")
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	@Autowired
	private FileRepository fileRepository;
	@Autowired
	private FileService fileService;
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
		// User me = (User) auth.getPrincipal();
		// jPAQ.where(q.creator.id.eq(me.getId()))
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
	public ResponseEntity<Map<String, Object>> add(MultipartFile file, @RequestPart File value) throws IOException {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (file == null)
			throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【文件】必传");
		// 验证所有不能为空的字段权限
		userService.check("file", "id", 0b10, auth);
		Set<String> perms = calUtility.getFields(auth, 0b10, "file");
		CalUtility.copyFields(value, perms);
		Map<String, Object> body = new HashMap<>();
		body.put("data", fileService.add(file, value));
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
		CalUtility.copyFields(target, value, perms, Set.of("*"));
		File file = fileRepository.findById(id).get();
		target.setFilename(file.getFilename());
		target.setContentType(file.getContentType());
		target.setSize(file.getSize());
		target.setOriginalFilename(file.getOriginalFilename());
		File value1 = fileRepository.save(target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/files/" + id)).body(body);
	}

	@PatchMapping("{id}")
	public ResponseEntity<Map<String, Object>> partialEdit(@PathVariable("id") Integer id, @RequestBody JsonNode json) {
		File value;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			value = objectMapper.readValue(json.toString(), File.class);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
		}
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证前端传过来的每一个字段名的权限。
		var it = json.fieldNames();
		Set<String> nulls = new HashSet<>();
		while (it.hasNext()) {
			String key = it.next();
			nulls.add(key);
			userService.check("file", key, 0b10, auth);
		}
		Optional<File> opt = fileRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		value.setFilename(null);
		value.setContentType(null);
		value.setSize(null);
		value.setOriginalFilename(null);
		File target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "file");
		CalUtility.copyFields(target, value, perms, nulls);
		File value1 = fileRepository.save(target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/files/" + id)).body(body);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer id) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"file:*:w" *表示要全部满足
		userService.checkAll("file", 0b10, auth);
		fileService.constraint(id);
		var file = fileRepository.findById(id).get();
		String filename = file.getFilename();
		fileRepository.deleteById(id);
		fileService.delFile(filename);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("exist")
	public ResponseEntity<Map<String, Object>> exist(File value, String matchMode, Integer excludeId) {
		Example<File> example;
		if ("any".equals(matchMode)) {
			ExampleMatcher matcher = ExampleMatcher.matchingAny();
			example = Example.of(value, matcher);
		} else
			example = Example.of(value);
		boolean isExist;
		if (excludeId == null)
			isExist = fileRepository.exists(example);
		else {
			var rows = fileRepository.findAll(example);
			isExist = rows.size() == 1 && !rows.get(0).getId().equals(excludeId) || rows.size() > 1;
		}
		Map<String, Object> body = new HashMap<>();
		body.put("data", isExist);
		return ResponseEntity.ok(body);
	}

	@GetMapping("view/{id}")
	public void view(HttpServletResponse response, @PathVariable("id") Integer id,
			@RequestParam(required = false) Integer width, @RequestParam(required = false) Integer height) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		userService.check("file", "filename", 0b01, auth);
		Optional<File> opt = fileRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		File value = opt.get();
		try {
			java.io.File file = new java.io.File("uploads" + java.io.File.separator + value.getFilename());
			response.setHeader("Content-Disposition",
					"filename=" + URLEncoder.encode(value.getOriginalFilename(), "utf-8"));
			OutputStream os = response.getOutputStream();
			String ct = value.getContentType();
			if (ct.startsWith("image") && width != null || height != null) { // 如果是图片，需要处理缩略图。
				BufferedImage originalBI = ImageIO.read(file);
				int scaleWidth, scaleHeight;
				if (width != null) {
					if (height != null) {
						if (width * originalBI.getHeight() > originalBI.getWidth() * height) {
							scaleHeight = height;
							scaleWidth = originalBI.getWidth() * scaleHeight / originalBI.getHeight();
						} else {
							scaleWidth = width;
							scaleHeight = originalBI.getHeight() * scaleWidth / originalBI.getWidth();
						}
					} else {
						scaleWidth = width;
						scaleHeight = originalBI.getHeight() * scaleWidth / originalBI.getWidth();
					}
				} else {
					scaleHeight = height;
					scaleWidth = originalBI.getWidth() * scaleHeight / originalBI.getHeight();
				}
				response.setContentType("image/jpeg");
				BufferedImage scaledBI = new BufferedImage(scaleWidth, scaleHeight, BufferedImage.TYPE_INT_RGB);
				// 设置缩放图片画笔
				Graphics2D g = scaledBI.createGraphics();
				g.setComposite(AlphaComposite.Src);
				// 绘制缩放图片
				g.drawImage(originalBI, 0, 0, scaleWidth, scaleHeight, null);
				g.dispose();
				ImageIO.write(scaledBI, "jpg", os);
			} else {
				InputStream is = new FileInputStream(file);
				response.setContentType(value.getContentType());
				int b;
				while ((b = is.read()) != -1)
					os.write(b);
				is.close();
			}
			os.flush();
			os.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "查看失败：" + e.getMessage());
		}
	}
}

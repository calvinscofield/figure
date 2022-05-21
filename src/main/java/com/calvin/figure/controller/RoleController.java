package com.calvin.figure.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.calvin.figure.CalUtility;
import com.calvin.figure.entity.Permission;
import com.calvin.figure.entity.QRole;
import com.calvin.figure.entity.Role;
import com.calvin.figure.repository.PermissionRepository;
import com.calvin.figure.repository.RoleRepository;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("roles")
public class RoleController {

	private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private PermissionRepository permissionRepository;
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
		// 验证权限"role:?:r" ?表示有任意一个满足即可
		userService.checkAny("role", 0b01, auth);
		QRole q = QRole.role;
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
		List<Role> rows = jPAQ.fetch();
		Set<String> perms = calUtility.getFields(auth, 0b01, "role");
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
		userService.checkAny("role", 0b01, auth);
		Optional<Role> opt = roleRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		Role value = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b01, "role");
		CalUtility.copyFields(value, perms);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value);
		return ResponseEntity.ok(body);
	}

	private void validate(Role value, JsonNode json) {
		if (json == null || json.has("name")) {
			if (value.getName() == null || value.getName().isEmpty())
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【名称】字段必传");
			// administrator、anonymous作为保留角色名称，用户不能新建
			if (List.of("administrator", "anonymous").contains(value.getName().toLowerCase()))
				throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
						String.format("【%s】不能作为角色名", value.getName()));
		}
		if (value.getPermission() != null) {
			List<Permission> permissions = new ArrayList<>();
			for (var p : value.getPermission()) {
				if (p.getId() == null)
					throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【权限】字段id不能为空");
				else {
					var optP = permissionRepository.findById(p.getId());
					if (optP.isEmpty())
						throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【权限】记录不存在");
					permissions.add(optP.get());
				}
			}
			value.setPermission(permissions);
		}
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> add(@RequestBody Role value) {
		validate(value, null);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证所有不能为空的字段权限
		userService.check("role", "id", 0b10, auth);
		userService.check("role", "name", 0b10, auth);
		value.setId(null); // 防止通过这个接口进行修改。
		Set<String> perms = calUtility.getFields(auth, 0b10, "role");
		CalUtility.copyFields(value, perms);
		Role value1 = roleRepository.save(value);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/roles/" + value1.getId())).body(body);
	}

	@PutMapping("{id}")
	public ResponseEntity<Map<String, Object>> edit(@PathVariable("id") Integer id, @RequestBody Role value) {
		validate(value, null);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"role:*:w" *表示要全部满足
		userService.checkAll("role", 0b10, auth);
		Optional<Role> opt = roleRepository.findById(id);
		if (!opt.isPresent())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		Role target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "role");
		CalUtility.copyFields(target, value, perms, Set.of("*"));
		Role value1 = roleRepository.save(target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/roles/" + id)).body(body);
	}

	@PatchMapping("{id}")
	public ResponseEntity<Map<String, Object>> partialEdit(@PathVariable("id") Integer id, @RequestBody JsonNode json) {
		Role value;
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			value = objectMapper.readValue(json.toString(), Role.class);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
		}
		validate(value, json);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证前端传过来的每一个字段名的权限。
		var it = json.fieldNames();
		Set<String> nulls = new HashSet<>();
		while (it.hasNext()) {
			String key = it.next();
			nulls.add(key);
			userService.check("role", key, 0b10, auth);
		}
		Optional<Role> opt = roleRepository.findById(id);
		if (opt.isEmpty())
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
		Role target = opt.get();
		Set<String> perms = calUtility.getFields(auth, 0b10, "role");
		CalUtility.copyFields(target, value, perms, nulls);
		Role value1 = roleRepository.save(target);
		Map<String, Object> body = new HashMap<>();
		body.put("data", value1);
		return ResponseEntity.created(URI.create("/roles/" + id)).body(body);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer id) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		// 验证权限"role:*:w" *表示要全部满足
		userService.checkAll("role", 0b10, auth);
		roleRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("exist")
	public ResponseEntity<Map<String, Object>> exist(Role value, String matchMode, Integer excludeId) {
		Example<Role> example;
		if ("any".equals(matchMode)) {
			ExampleMatcher matcher = ExampleMatcher.matchingAny();
			example = Example.of(value, matcher);
		} else
			example = Example.of(value);
		boolean isExist;
		if (excludeId == null)
			isExist = roleRepository.exists(example);
		else {
			var rows = roleRepository.findAll(example);
			isExist = rows.size() == 1 && !rows.get(0).getId().equals(excludeId) || rows.size() > 1;
		}
		Map<String, Object> body = new HashMap<>();
		body.put("data", isExist);
		return ResponseEntity.ok(body);
	}
}

package com.calvin.figure.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.calvin.figure.entity.MetaTable;
import com.calvin.figure.entity.QMetaTable;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("metaTables")
public class MetaTableController {

	@Autowired
	private JPAQueryFactory jPAQueryFactory;

	@GetMapping
	public ResponseEntity<Map<String, Object>> find(@RequestParam(required = false) Integer offset,
			@RequestParam(required = false) Integer limit, @RequestParam(required = false) String keyword) {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (AuthorityAuthorizationManager.hasRole("ANONYMOUS").check(() -> auth, null).isGranted())
			throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "需要登录");
		QMetaTable q = QMetaTable.metaTable;
		var jPAQ = jPAQueryFactory.selectFrom(q);
		if (offset == null && limit != null)
			offset = 0;
		if (offset != null && limit == null)
			limit = 10;
		if (keyword != null && !keyword.isEmpty()) {
			String kw = "%" + keyword + "%";
			jPAQ.where(q.name.likeIgnoreCase(kw).or(q.displayName.likeIgnoreCase(kw)));
		}
		boolean paged = offset != null && limit != null;
		if (paged) {
			jPAQ.offset(offset);
			jPAQ.limit(limit);
		}
		List<MetaTable> rows = jPAQ.fetch();
		Map<String, Object> body = new HashMap<>();
		if (paged) {
			body.put("offset", offset + rows.size());
			body.put("limit", limit);
		}
		body.put("data", rows);
		return ResponseEntity.ok(body);
	}

}

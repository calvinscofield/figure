package com.calvin.figure.controller;

import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.calvin.figure.CalUtility;
import com.calvin.figure.entity.QUser;
import com.calvin.figure.entity.User;
import com.calvin.figure.repository.UserRepository;
import com.calvin.figure.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JPAQueryFactory jPAQueryFactory;
    @Autowired
    private CalUtility calUtility;

    private static final SecureRandom random = new SecureRandom();

    private static final char[] alphabet = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .toCharArray();

    private static String randomUsername() {
        final int size = 14;
        final int mask = (2 << (int) Math.floor(Math.log(alphabet.length - 1) / Math.log(2))) - 1;
        final int step = (int) Math.ceil(1.6 * mask * size / alphabet.length);
        final StringBuilder idBuilder = new StringBuilder();
        while (true) {
            final byte[] bytes = new byte[step];
            random.nextBytes(bytes);
            for (int i = 0; i < step; i++) {
                final int alphabetIndex = bytes[i] & mask;
                if (alphabetIndex < alphabet.length) {
                    idBuilder.append(alphabet[alphabetIndex]);
                    if (idBuilder.length() == size) {
                        return idBuilder.toString();
                    }
                }
            }
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> find(@RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String keyword) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证权限"user:?:r" ?表示有任意一个满足即可
        userService.checkAny("user", 0b01, auth);
        QUser q = QUser.user;
        var jPAQ = jPAQueryFactory.selectFrom(q);
        if (offset == null && limit != null)
            offset = 0;
        if (offset != null && limit == null)
            limit = 10;
        if (keyword != null && !keyword.isEmpty()) {
            String kw = "%" + keyword + "%";
            jPAQ.where(q.username.likeIgnoreCase(kw).or(q.name.likeIgnoreCase(kw)).or(q.email.likeIgnoreCase(kw))
                    .or(q.remark.likeIgnoreCase(kw)));
        }
        boolean paged = offset != null && limit != null;
        if (paged) {
            jPAQ.offset(offset);
            jPAQ.limit(limit);
        }
        List<User> rows = jPAQ.fetch();
        Iterator<User> it = rows.iterator();
        while (it.hasNext()) {
            it.next().setPassword(null);
        }
        Set<String> perms = calUtility.getFields(auth, 0b01, "user");
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
        userService.checkAny("user", 0b01, auth);
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        User value = opt.get();
        value.setPassword(null);
        Set<String> perms = calUtility.getFields(auth, 0b01, "user");
        CalUtility.copyFields(value, perms);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> add(@RequestBody User value) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (value.getUsername() == null || value.getUsername().isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【用户名】字段必传");
        if (value.getPassword() == null || value.getPassword().isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【密码】字段必传");

        // 验证所有不能为空的字段权限
        userService.check("user", "id", 0b10, auth);
        userService.check("user", "username", 0b10, auth);
        userService.check("user", "password", 0b10, auth);

        if (value.getName() != null && value.getName().isEmpty())
            value.setName(null);
        value.setId(null); // 防止通过这个接口进行修改。
        value.setPassword(passwordEncoder.encode(value.getPassword()));
        Set<String> perms = calUtility.getFields(auth, 0b10, "user");
        CalUtility.copyFields(value, perms);
        User value1 = userRepository.save(value);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/" + value1.getId())).body(body);
    }

    @PutMapping("{id}")
    public ResponseEntity<Map<String, Object>> edit(@PathVariable("id") Integer id, @RequestBody User value) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证权限"user:*:w" *表示要全部满足
        userService.checkAll("user", 0b10, auth);
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        User target = opt.get();
        if (userService.isAdministrator(target))
            throw new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, "不能修改内置管理员");
        if (value.getPassword() != null)
            value.setPassword(passwordEncoder.encode(value.getPassword()));
        Set<String> perms = calUtility.getFields(auth, 0b10, "user");
        CalUtility.copyFields(target, value, perms, true);
        User value1 = userRepository.save(target);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/" + id)).body(body);
    }

    @PatchMapping("{id}")
    public ResponseEntity<Map<String, Object>> partialEdit(@PathVariable("id") Integer id, @RequestBody JsonNode json) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证前端传过来的每一个字段名的权限。
        var it = json.fieldNames();
        while (it.hasNext()) {
            userService.check("user", it.next(), 0b10, auth);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        User value;
        try {
            value = objectMapper.readValue(json.toString(), User.class);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
        }
        Optional<User> opt = userRepository.findById(id);
        if (!opt.isPresent())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        User target = opt.get();
        if (userService.isAdministrator(target))
            throw new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, "不能修改内置管理员");
        if (value.getPassword() != null)
            value.setPassword(passwordEncoder.encode(value.getPassword()));
        Set<String> perms = calUtility.getFields(auth, 0b10, "user");
        CalUtility.copyFields(target, value, perms, false);
        User value1 = userRepository.save(target);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/" + id)).body(body);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证权限"user:*:w" *表示要全部满足
        userService.checkAll("user", 0b10, auth);
        if (userService.isAdministrator(id))
            throw new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, "不能删除内置管理员");
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("login")
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request, @RequestParam String username,
            @RequestParam String password, @RequestParam String version,
            @RequestParam(required = false) boolean error) {
        if (error) {
            var e = (AuthenticationException) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } else {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            User user1 = new User();
            user1.setUsername(user.getUsername());
            user1.setName(user.getName());
            user1.setPerms(user.getPerms());
            userService.sendLoginMail(username, calUtility.getClientIp(request));
            Map<String, Object> body = new HashMap<>();
            body.put("data", user1);
            return ResponseEntity.ok(body);
        }
    }

    @PostMapping("logout")
    public ResponseEntity<Map<String, Object>> logout() {
        User user = new User("anonymous");
        ObjectNode perms = userService.generateAnonymousPerms();
        user.setPerms(perms);
        Map<String, Object> body = new HashMap<>();
        body.put("data", user);
        return ResponseEntity.ok(body);
    }

    @PostMapping("register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User value) {
        if (value.getEmail() == null || value.getEmail().isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【邮箱】字段必传");
        if (value.getPassword() == null || value.getPassword().isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【密码】字段必传");
        User user = new User(randomUsername());
        user.setEmail(value.getEmail());
        user.setPassword(passwordEncoder.encode(value.getPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("me")
    public ResponseEntity<Map<String, Object>> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User();
        ObjectNode perms;
        if (AuthorityAuthorizationManager.hasRole("ANONYMOUS").check(() -> auth, null).isGranted()) {
            user.setUsername("anonymous");
            user.setName("匿名");
            perms = userService.generateAnonymousPerms();
        } else {
            User user1 = (User) auth.getPrincipal();
            user.setUsername(user1.getUsername());
            user.setName(user1.getName());
            perms = userService.generatePerms(user1.getId());
        }
        user.setPerms(perms);
        Map<String, Object> body = new HashMap<>();
        body.put("data", user);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("me")
    public ResponseEntity<Map<String, Object>> partialEditMe(@RequestBody JsonNode json,
            @RequestHeader("Password") String checkPassword) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证前端传过来的每一个字段名的权限。
        var it = json.fieldNames();
        while (it.hasNext()) {
            String key = it.next();
            userService.check("user", key, 0b10, auth);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        User value;
        try {
            value = objectMapper.readValue(json.toString(), User.class);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
        }
        // 以下信息都不让修改。
        value.setUsername(null);
        value.setRole(null);
        value.setExpiryTime(null);
        value.setCredentialsExpiryTime(null);
        value.setLocked(null);
        value.setEnabled(null);

        User me = (User) auth.getPrincipal();
        Optional<User> opt = userRepository.findById(me.getId());
        if (!opt.isPresent())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        User target = opt.get();
        // 修改密码要验证原密码
        if (value.getPassword() != null) {
            if (checkPassword == null)
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "需要验证密码");
            else {
                if (!passwordEncoder.matches(checkPassword, target.getPassword()))
                    throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "验证密码错误");
            }
            value.setPassword(passwordEncoder.encode(value.getPassword()));
        }
        Set<String> perms = calUtility.getFields(auth, 0b10, "user");
        CalUtility.copyFields(target, value, perms, false);
        User value1 = userRepository.save(target);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/me")).body(body);
    }

}

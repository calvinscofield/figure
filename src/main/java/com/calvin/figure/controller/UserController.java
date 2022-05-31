package com.calvin.figure.controller;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.calvin.figure.CalUtility;
import com.calvin.figure.entity.QUser;
import com.calvin.figure.entity.Role;
import com.calvin.figure.entity.User;
import com.calvin.figure.repository.FileRepository;
import com.calvin.figure.repository.RoleRepository;
import com.calvin.figure.repository.UserRepository;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JPAQueryFactory jPAQueryFactory;
    @Autowired
    private CalUtility calUtility;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final char[] ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .toCharArray();

    private static final char[] NUMBER = "0123456789".toCharArray();

    private static String random(char[] alphabet, int size) {
        final int mask = (2 << (int) Math.floor(Math.log(alphabet.length - 1) / Math.log(2))) - 1;
        final int step = (int) Math.ceil(1.6 * mask * size / alphabet.length);
        final StringBuilder idBuilder = new StringBuilder();
        while (true) {
            final byte[] bytes = new byte[step];
            RANDOM.nextBytes(bytes);
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
        var q = QUser.user;
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
        var rows = jPAQ.fetch();
        Set<String> perms = userService.getFields(auth, 0b01, "user");
        CalUtility.copyFields(rows, perms);
        var it = rows.iterator();
        while (it.hasNext()) {
            it.next().setPassword(null);
        }
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
        var opt = userRepository.findById(id);
        if (opt.isEmpty())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        var value = opt.get();
        Set<String> perms = userService.getFields(auth, 0b01, "user");
        CalUtility.copyFields(value, perms);
        value.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value);
        return ResponseEntity.ok(body);
    }

    private void validate(MultipartFile avatar, User value, JsonNode json) {
        if (json == null || json.has("username")) {
            if (value.getUsername() == null || value.getUsername().isEmpty())
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【用户名】字段必传");
            // anonymous作为保留用户名，用户不能新建
            if ("anonymous".equals(value.getUsername().toLowerCase()))
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
                        String.format("【%s】不能作为用户名", value.getUsername()));
        }
        if (value.getName() != null && value.getName().isEmpty())
            value.setName(null);
        if (json == null || json.has("password")) {
            if (value.getPassword() == null || value.getPassword().isEmpty())
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【密码】字段必传");
            value.setPassword(passwordEncoder.encode(value.getPassword()));
        }
        if (avatar != null) {
            if (!avatar.getContentType().startsWith("image/"))
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "头像必须是图片");
            if (avatar.getSize() > 1048576)
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "头像图片大小不能超过1MB");
        } else if (json == null || json.has("avatar")) {
            var avatar1 = value.getAvatar();
            if (avatar1 != null)
                if (value.getAvatar().getId() == null)
                    throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "头像文件id不能为空");
                else {
                    var opt = fileRepository.findById(avatar1.getId());
                    if (opt.isEmpty())
                        throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "头像文件不存在");
                    else {
                        var f = opt.get();
                        if (!f.getContentType().startsWith("image/"))
                            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "头像必须是图片");
                        if (f.getSize() > 1048576)
                            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "头像图片大小不能超过1MB");
                        value.setAvatar(f);
                    }
                }
        }
        if (value.getRole() != null) {
            List<Role> roles = new ArrayList<>();
            for (var role : value.getRole()) {
                if (role.getId() == null)
                    throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【角色】字段id不能为空");
                else {
                    var optRole = roleRepository.findById(role.getId());
                    if (optRole.isEmpty())
                        throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【角色】记录不存在");
                    roles.add(optRole.get());
                }
            }
            value.setRole(roles);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> add(MultipartFile avatar, @RequestPart User value) throws IOException {
        validate(avatar, value, null);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证所有不能为空的字段权限
        userService.check("user", "id", 0b10, auth);
        userService.check("user", "username", 0b10, auth);
        userService.check("user", "password", 0b10, auth);
        value.setId(null); // 防止通过这个接口进行修改。
        Set<String> perms = userService.getFields(auth, 0b10, "user");
        CalUtility.copyFields(value, perms);
        var value1 = userService.add(avatar, value);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/" + value1.getId())).body(body);
    }

    @PutMapping("{id}")
    public ResponseEntity<Map<String, Object>> edit(@PathVariable("id") Integer id, MultipartFile avatar,
            @RequestPart User value) throws IOException {
        validate(avatar, value, null);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证权限"user:*:w" *表示要全部满足
        userService.checkAll("user", 0b10, auth);
        var opt = userRepository.findById(id);
        if (opt.isEmpty())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        var target = opt.get();
        if (userService.isAdministrator(target))
            throw new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, "不能修改内置管理员");
        Set<String> perms = userService.getFields(auth, 0b10, "user");
        CalUtility.copyFields(target, value, perms, Set.of("*"));
        var value1 = userService.edit(avatar, target);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/" + id)).body(body);
    }

    @PatchMapping("{id}")
    public ResponseEntity<Map<String, Object>> partialEdit(@PathVariable("id") Integer id, MultipartFile avatar,
            @RequestPart JsonNode json) throws IOException {
        User value;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            value = objectMapper.readValue(json.toString(), User.class);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "传参有误");
        }
        validate(avatar, value, json);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // 验证前端传过来的每一个字段名的权限。
        var it = json.fieldNames();
        Set<String> nulls = new HashSet<>();
        while (it.hasNext()) {
            String key = it.next();
            nulls.add(key);
            userService.check("user", key, 0b10, auth);
        }
        var opt = userRepository.findById(id);
        if (opt.isEmpty())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        var target = opt.get();
        if (userService.isAdministrator(target))
            throw new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT, "不能修改内置管理员");
        Set<String> perms = userService.getFields(auth, 0b10, "user");
        CalUtility.copyFields(target, value, perms, nulls);
        var value1 = userService.edit(avatar, target);
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

    @GetMapping("exist")
    public ResponseEntity<Map<String, Object>> exist(User value, String matchMode, Integer excludeId) {
        Example<User> example;
        if ("any".equals(matchMode)) {
            ExampleMatcher matcher = ExampleMatcher.matchingAny();
            example = Example.of(value, matcher);
        } else
            example = Example.of(value);
        boolean isExist;
        if (excludeId == null)
            isExist = userRepository.exists(example);
        else {
            var rows = userRepository.findAll(example);
            isExist = rows.size() == 1 && !rows.get(0).getId().equals(excludeId) || rows.size() > 1;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("data", isExist);
        return ResponseEntity.ok(body);
    }

    @PostMapping("login")
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request, @RequestParam String version,
            @RequestParam(required = false) boolean error) {
        if (error) {
            var e = (AuthenticationException) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } else {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            User me = (User) auth.getPrincipal();
            User user = new User();
            user.setUsername(me.getUsername());
            user.setName(me.getName());
            user.setAvatar(me.getAvatar());
            user.setPerms(me.getPerms());
            userService.sendLoginMail(me.getUsername(), calUtility.getClientIp(request));
            Map<String, Object> body = new HashMap<>();
            body.put("data", user);
            return ResponseEntity.ok(body);
        }
    }

    @PostMapping("logout")
    public ResponseEntity<Map<String, Object>> logout() {
        User me = userService.generateAnonymousUser();
        var perms = userService.generatePerms(me);
        me.setPerms(perms);
        Map<String, Object> body = new HashMap<>();
        body.put("data", me);
        return ResponseEntity.ok(body);
    }

    @PostMapping("register")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> register(String email, String password, String code,
            HttpSession session) {
        if (email == null || email.isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【邮箱】必传");
        if (password == null || password.isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【密码】必传");
        if (code == null || code.isEmpty())
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "【验证码】必传");
        User user1 = new User();
        user1.setEmail(email);
        Example<User> example = Example.of(user1);
        boolean isExist = userRepository.exists(example);
        if (isExist)
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "邮箱已注册");
        Map<String, Object> emailCode = (Map<String, Object>) session.getAttribute("emailRegister");
        if (emailCode == null)
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "未申请验证码");
        String email1 = (String) emailCode.get("email");
        if (!email.equals(email1))
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "接收验证码的邮箱与注册邮箱不一致");
        String code1 = (String) emailCode.get("code");
        if (!code.equals(code1))
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "验证码不正确");
        Instant expiry = (Instant) emailCode.get("expiry");
        if (Instant.now().isAfter(expiry))
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "验证码已过期");
        User user = new User(random(ALPHABET, 14));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("me")
    public ResponseEntity<Map<String, Object>> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        User me1;
        if (AuthorityAuthorizationManager.hasRole("ANONYMOUS").check(() -> auth, null).isGranted()) {
            me1 = userService.generateAnonymousUser();
        } else {
            User me = (User) auth.getPrincipal();
            me1 = userRepository.findById(me.getId()).get();
        }
        var perms = userService.generatePerms(me1);
        User user = new User();
        user.setUsername(me1.getUsername());
        user.setName(me1.getName());
        user.setAvatar(me1.getAvatar());
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
        Set<String> nulls = new HashSet<>();
        while (it.hasNext()) {
            String key = it.next();
            nulls.add(key);
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
        value.setDisabled(null);

        User me = (User) auth.getPrincipal();
        var opt = userRepository.findById(me.getId());
        if (opt.isEmpty())
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "记录不存在");
        var target = opt.get();
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
        Set<String> perms = userService.getFields(auth, 0b10, "user");
        CalUtility.copyFields(target, value, perms, nulls);
        var value1 = userRepository.save(target);
        value1.setPassword(null);
        Map<String, Object> body = new HashMap<>();
        body.put("data", value1);
        return ResponseEntity.created(URI.create("/users/me")).body(body);
    }

    @PostMapping("emailLoginCode")
    public ResponseEntity<Map<String, Object>> emailLoginCode(HttpSession session, @RequestParam String email) {
        User user = new User();
        user.setEmail(email);
        Example<User> example = Example.of(user);
        boolean isExist = userRepository.exists(example);
        if (!isExist)
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "邮箱不存在");
        String code = random(NUMBER, 6);
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("code", code);
        map.put("expiry", Instant.now().plusSeconds(300));
        session.setAttribute("emailLogin", map);
        userService.sendLoginCodeMail(email, code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("emailRegisterCode")
    public ResponseEntity<Map<String, Object>> emailRegisterCode(HttpSession session, @RequestParam String email) {
        User user = new User();
        user.setEmail(email);
        Example<User> example = Example.of(user);
        boolean isExist = userRepository.exists(example);
        if (isExist)
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "邮箱已注册");
        String code = random(NUMBER, 6);
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("code", code);
        map.put("expiry", Instant.now().plusSeconds(300));
        session.setAttribute("emailRegister", map);
        userService.sendRegisterCodeMail(email, code);
        return ResponseEntity.noContent().build();
    }
}

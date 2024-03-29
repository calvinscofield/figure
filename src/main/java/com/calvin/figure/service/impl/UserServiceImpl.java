package com.calvin.figure.service.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;

import com.calvin.figure.entity.File;
import com.calvin.figure.entity.MetaField;
import com.calvin.figure.entity.MetaSystem;
import com.calvin.figure.entity.MetaTable;
import com.calvin.figure.entity.QUser;
import com.calvin.figure.entity.Role;
import com.calvin.figure.entity.User;
import com.calvin.figure.repository.MetaFieldRepository;
import com.calvin.figure.repository.MetaSystemRepository;
import com.calvin.figure.repository.MetaTableRepository;
import com.calvin.figure.repository.RoleRepository;
import com.calvin.figure.repository.UserRepository;
import com.calvin.figure.service.FileService;
import com.calvin.figure.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private MetaTableRepository metaTableRepository;
    @Autowired
    private MetaFieldRepository metaFieldRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private MetaSystemRepository metaSystemRepository;
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private JPAQueryFactory jPAQueryFactory;
    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void checkAny(String metaTableName, int type, Authentication auth) {
        if (AuthorityAuthorizationManager.hasRole("ADMINISTRATOR").check(() -> auth, null).isGranted())
            return;
        Assert.isTrue(type > 0, "权限类型必须大于0");
        var r = (type & 0b01) == 0b01;
        var w = (type & 0b10) == 0b10;
        try {
            List<String> authorities = new ArrayList<>();
            MetaTable metaTable = metaTableRepository.findByName(metaTableName).get();
            for (var metaField : metaTable.getMetaField()) {
                if (r)
                    authorities.add(String.format("%s:%s:%s", metaTableName, metaField.getName(), "r"));
                if (w)
                    authorities.add(String.format("%s:%s:%s", metaTableName, metaField.getName(), "w"));
            }
            if (!AuthorityAuthorizationManager.hasAnyAuthority(authorities.toArray(new String[0]))
                    .check(() -> auth, null).isGranted()) {
                throw new HttpClientErrorException(HttpStatus.FORBIDDEN,
                        String.format("没有【%s】【%s】权限", metaTable.getDisplayName(), r ? "读" : "" + (w ? "写" : "")));
            }
        } catch (NoSuchElementException e) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("没有配置资源【%s】", metaTableName));
        }
    }

    @Override
    public void check(String metaTableName, String metaFieldName, int type, Authentication auth) {
        if (AuthorityAuthorizationManager.hasRole("ADMINISTRATOR").check(() -> auth, null).isGranted())
            return;
        Assert.isTrue(type > 0, "权限类型必须大于0");
        var r = (type & 0b01) == 0b01;
        var w = (type & 0b10) == 0b10;
        List<String> authorities = new ArrayList<>();
        if (r)
            authorities.add(String.format("%s:%s:%s", metaTableName, metaFieldName, "r"));
        if (w)
            authorities.add(String.format("%s:%s:%s", metaTableName, metaFieldName, "w"));
        if (!AuthorityAuthorizationManager.hasAnyAuthority(authorities.toArray(new String[0]))
                .check(() -> auth, null).isGranted()) {
            MetaTable metaTable;
            MetaField metaField;
            try {
                metaTable = metaTableRepository.findByName(metaTableName).get();
            } catch (NoSuchElementException e) {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("没有配置资源【%s】", metaTableName));
            }
            try {
                metaField = metaFieldRepository.findByName(metaFieldName).get();
            } catch (NoSuchElementException e) {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("没有配置资源【%s】【%s】", metaTableName, metaFieldName));
            }
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN,
                    String.format("没有【%s】【%s】【%s】权限", metaTable.getDisplayName(), metaField.getDisplayName(),
                            r ? "读" : "" + (w ? "写" : "")));
        }
    }

    @Override
    public void checkAll(String metaTableName, int type, Authentication auth) {
        if (AuthorityAuthorizationManager.hasRole("ADMINISTRATOR").check(() -> auth, null).isGranted())
            return;
        Assert.isTrue(type > 0, "权限类型必须大于0");
        var r = (type & 0b01) == 0b01;
        var w = (type & 0b10) == 0b10;
        try {
            MetaTable metaTable = metaTableRepository.findByName(metaTableName).get();
            for (var metaField : metaTable.getMetaField()) {
                if (r && !AuthorityAuthorizationManager
                        .hasAuthority(String.format("%s:%s:%s", metaTableName, metaField.getName(), "r"))
                        .check(() -> auth, null).isGranted()) {
                    throw new HttpClientErrorException(HttpStatus.FORBIDDEN,
                            String.format("没有【%s】【%s】读权限", metaTable.getDisplayName(), metaField.getDisplayName()));
                }
                if (w && !AuthorityAuthorizationManager
                        .hasAuthority(String.format("%s:%s:%s", metaTableName, metaField.getName(), "w"))
                        .check(() -> auth, null).isGranted()) {
                    throw new HttpClientErrorException(HttpStatus.FORBIDDEN,
                            String.format("没有【%s】【%s】写权限", metaTable.getDisplayName(), metaField.getDisplayName()));
                }
            }
        } catch (NoSuchElementException e) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("没有配置资源【%s】", metaTableName));
        }

    }

    @Transactional
    @Override
    public void generateAuthorities(Collection<GrantedAuthority> authorities, User user) {
        List<MetaSystem> metaSystems = metaSystemRepository.findAll(PageRequest.of(0, 1)).getContent();
        // 超级管理员
        if (metaSystems.size() > 0 && user.getUsername().equals(metaSystems.get(0).getAdministrator())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        } else {
            for (var role : user.getRole()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                for (var perm : role.getPermission()) {
                    var metaTable = perm.getMetaTable();
                    var mTName = metaTable.getName();
                    var metaFields = metaTable.getMetaField();
                    var field = perm.getField();
                    if ("*".equals(field.asText())) {
                        for (var mF : metaFields) {
                            var mFName = mF.getName();
                            authorities.add(new SimpleGrantedAuthority(mTName + ":" + mFName + ":r"));
                            authorities.add(new SimpleGrantedAuthority(mTName + ":" + mFName + ":w"));
                        }
                    } else if (field.isObject()) {
                        for (var mF : metaFields) {
                            var mFName = mF.getName();
                            var it = field.fields();
                            while (it.hasNext()) {
                                var el = it.next();
                                if (el.getKey().equals(mFName)) {
                                    var rw = el.getValue().asInt();
                                    if ((rw & 0b01) == 0b01) {
                                        authorities.add(new SimpleGrantedAuthority(mTName + ":" + mFName + ":r"));
                                    }
                                    if ((rw & 0b10) == 0b10) {
                                        authorities.add(new SimpleGrantedAuthority(mTName + ":" + mFName + ":w"));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Transactional
    @Override
    public User generateAuthorities(Collection<GrantedAuthority> authorities, Integer id) {
        User user = userRepository.findById(id).get();
        generateAuthorities(authorities, user);
        return user;
    }

    @Transactional
    @Override
    public User generateAuthorities(Collection<GrantedAuthority> authorities) {
        User user = generateAnonymousUser();
        generateAuthorities(authorities, user);
        return user;
    }

    @Transactional
    @Override
    public ObjectNode generatePerms(User user) {
        var metaTables = metaTableRepository.findAll();
        ObjectNode permsT = JsonNodeFactory.instance.objectNode();
        List<MetaSystem> metaSystems = metaSystemRepository.findAll(PageRequest.of(0, 1)).getContent();
        // 超级管理员
        if (metaSystems.size() > 0 && user.getUsername().equals(metaSystems.get(0).getAdministrator())) {
            for (var mt : metaTables) {
                ObjectNode permsF = JsonNodeFactory.instance.objectNode();
                var mTName = mt.getName();
                var metaFields = mt.getMetaField();
                for (var mF : metaFields) {
                    permsF.put(mF.getName(), 0b11);
                }
                permsT.set(mTName, permsF);
            }
        } else {
            for (var mt : metaTables) {
                ObjectNode permsF = JsonNodeFactory.instance.objectNode();
                var mTName = mt.getName();
                var metaFields = mt.getMetaField();
                for (var mF : metaFields) {
                    permsF.put(mF.getName(), 0b00);
                }
                permsT.set(mTName, permsF);
            }
            for (var role : user.getRole()) {
                for (var perm : role.getPermission()) {
                    var metaTable = perm.getMetaTable();
                    var mTName = metaTable.getName();
                    var metaFields = metaTable.getMetaField();
                    var field = perm.getField();
                    var permsF = (ObjectNode) permsT.get(mTName);
                    if ("*".equals(field.asText())) {
                        for (var mF : metaFields) {
                            permsF.put(mF.getName(), 0b11);
                        }
                    } else if (field.isObject()) {
                        for (var mF : metaFields) {
                            var mFName = mF.getName();
                            var it = field.fields();
                            while (it.hasNext()) {
                                var el = it.next();
                                if (el.getKey().equals(mFName)) {
                                    permsF.put(mF.getName(), permsF.get(mF.getName()).asInt() | el.getValue().asInt());
                                    break;
                                }
                            }
                        }
                    }
                    permsT.set(mTName, permsF);
                }
            }
        }
        return permsT;
    }

    @Transactional
    @Override
    public ObjectNode generatePerms(Integer id) {
        User user = userRepository.findById(id).get();
        return generatePerms(user);
    }

    @Async
    @Override
    public void sendLoginMail(String username, String ip) {
        logger.debug("MultiThreadProcessService-sendMail" + Thread.currentThread() + "......start");
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo("johnmore@163.com");
            helper.setSubject("登录提醒");
            helper.setText(String.format("用户%s登录，时间：%s, IP：%s", username, Instant.now(), ip));
            javaMailSender.send(message);
        } catch (MessagingException | MailException e) {
            logger.error("发送邮件失败", e);
        }
        logger.debug("MultiThreadProcessService-sendMail" + Thread.currentThread() + "......end");
    }

    @Async
    @Override
    public void sendLoginCodeMail(String email, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("验证码");
            helper.setText(
                    String.format("【Figure账号】验证码：%s。您正在使用邮箱验证码登录功能，验证码5分钟内有效，提供给他人可能导致账号被盗。如非本人操作请忽略此邮件。", code));
            javaMailSender.send(message);
        } catch (MessagingException | MailException e) {
            logger.error("发送邮件失败", e);
        }
    }

    @Async
    @Override
    public void sendRegisterCodeMail(String email, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("验证码");
            helper.setText(
                    String.format("【Figure账号】验证码：%s。您正在使用邮箱验证码注册功能，验证码5分钟内有效，请勿提供给他人。如非本人操作请忽略此邮件。", code));
            javaMailSender.send(message);
        } catch (MessagingException | MailException e) {
            logger.error("发送邮件失败", e);
        }
    }

    @Override
    @Transactional
    public boolean isAdministrator(Integer id) {
        User user = userRepository.findById(id).get();
        return isAdministrator(user);
    }

    @Override
    public boolean isAdministrator(User user) {
        List<MetaSystem> metaSystems = metaSystemRepository.findAll(PageRequest.of(0, 1)).getContent();
        // 超级管理员
        return metaSystems.size() > 0 && user.getUsername().equals(metaSystems.get(0).getAdministrator());
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QUser q = QUser.user;
        var jPAQ = jPAQueryFactory.selectFrom(q);
        jPAQ.where(q.username.eq(username).or(q.email.eq(username)).or(q.phone.eq(username)));
        User user = jPAQ.fetchOne();
        if (user == null)
            throw new UsernameNotFoundException("用户不存在");
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        generateAuthorities(authorities, user);
        user.setAuthorities(authorities);
        user.setPerms(generatePerms(user));
        return user;
    }

    @Override
    @Transactional
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user;
        try {
            user = userRepository.findByEmail(email).get();
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            generateAuthorities(authorities, user);
            user.setAuthorities(authorities);
            user.setPerms(generatePerms(user));
        } catch (NoSuchElementException e) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public User add(MultipartFile avatar, User value) throws IOException {
        if (avatar != null) {
            File file = new File();
            var file1 = fileService.add(avatar, file);
            value.setAvatar(file1);
        }
        return userRepository.save(value);
    }

    @Override
    @Transactional
    public User edit(MultipartFile avatar, User value) throws IOException {
        if (avatar != null) {
            File file = new File();
            var file1 = fileService.add(avatar, file);
            value.setAvatar(file1);
        }
        return userRepository.save(value);
    }

    @Override
    @Transactional
    public User generateAnonymousUser() {
        User user = new User("anonymous");
        user.setName("匿名");
        var optRole = roleRepository.findByName("ANONYMOUS");
        List<Role> roles = new ArrayList<>();
        if (optRole.isPresent())
            roles.add(optRole.get());
        user.setRole(roles);
        return user;
    }

    @Override
    @Transactional
    public Set<String> getFields(Authentication auth, int type, String metaTableName) {
        Set<String> fields = new HashSet<>();
        if (AuthorityAuthorizationManager.hasRole("ADMINISTRATOR").check(() -> auth, null).isGranted()) {
            fields.add("*");
            return fields;
        }
        User user;
        if (AuthorityAuthorizationManager.hasRole("ANONYMOUS").check(() -> auth, null).isGranted()) {
            user = generateAnonymousUser();
        } else {
            User me = (User) auth.getPrincipal();
            user = userRepository.findById(me.getId()).get();
        }
        if (user.getRole() == null)
            return fields;
        for (var role : user.getRole()) {
            var perms = role.getPermission();
            if (perms == null)
                continue;
            for (var perm : perms) {
                if (!perm.getMetaTable().getName().equals(metaTableName))
                    continue;
                JsonNode field = perm.getField();
                if ("*".equals(field.asText())) {
                    fields.clear();
                    fields.add("*");
                    return fields;
                } else if (field.isObject()) {
                    var it = field.fields();
                    while (it.hasNext()) {
                        var it1 = it.next();
                        var rw = it1.getValue().asInt();
                        if ((rw & type) == type) {
                            fields.add(it1.getKey());
                        }
                    }
                }
            }
        }
        return fields;
    }
}
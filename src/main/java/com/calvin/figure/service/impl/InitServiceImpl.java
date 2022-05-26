package com.calvin.figure.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.transaction.Transactional;

import com.calvin.figure.entity.MetaField;
import com.calvin.figure.entity.MetaSystem;
import com.calvin.figure.entity.MetaTable;
import com.calvin.figure.entity.Permission;
import com.calvin.figure.entity.Role;
import com.calvin.figure.entity.User;
import com.calvin.figure.repository.MetaFieldRepository;
import com.calvin.figure.repository.MetaSystemRepository;
import com.calvin.figure.repository.MetaTableRepository;
import com.calvin.figure.repository.PermissionRepository;
import com.calvin.figure.repository.RoleRepository;
import com.calvin.figure.repository.UserRepository;
import com.calvin.figure.service.InitService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class InitServiceImpl implements InitService {

    @Autowired
    private MetaSystemRepository metaSystemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MetaTableRepository metaTableRepository;
    @Autowired
    private MetaFieldRepository metaFieldRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${version}")
    private String version;

    @Transactional
    @Override
    public void initUserRole() {
        // 系统启动时更新版本号和启动次数
        List<MetaSystem> metaSystems = metaSystemRepository.findAll(PageRequest.of(0, 1)).getContent();
        if (metaSystems.size() > 0) {
            MetaSystem metaSystem = metaSystems.get(0);
            metaSystem.setSystemVersion(version);
            metaSystem.setCount(metaSystem.getCount() + 1);
            metaSystemRepository.save(metaSystem);
        } else {
            MetaSystem metaSystem = new MetaSystem();
            metaSystem.setName("医疗器械管理系统");
            metaSystem.setCompanyName("医疗器械测试有限公司");
            metaSystem.setAdministrator("administrator");
            metaSystem.setSystemVersion(version);
            metaSystem.setCount(1);
            metaSystemRepository.save(metaSystem);
        }

        List<Permission> permissions = new ArrayList<>();
        TextNode field = JsonNodeFactory.instance.textNode("*");

        // metaSystem 系统元信息表
        if (!metaTableRepository.existsByName("metaSystem")) {
            var metaTable = metaTableRepository.save(new MetaTable("metaSystem", "系统元信息表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("companyName", metaTable, "公司名称"));
            metaFields.add(new MetaField("administrator", metaTable, "管理员"));
            metaFields.add(new MetaField("systemVersion", metaTable, "系统版本"));
            metaFields.add(new MetaField("count", metaTable, "启动次数"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setField(field);
            permission.setName("系统元信息表全部");
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }
        // metaTable 数据表元信息表
        if (!metaTableRepository.existsByName("metaTable")) {
            var metaTable = metaTableRepository.save(new MetaTable("metaTable", "数据表元信息表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("displayName", metaTable, "显示名称"));
            metaFields.add(new MetaField("metaField", metaTable, "字段元信息"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("数据表元信息表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }
        // metaField 字段元信息表
        if (!metaTableRepository.existsByName("metaField")) {
            var metaTable = metaTableRepository.save(new MetaTable("metaField", "字段元信息表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("displayName", metaTable, "显示名称"));
            metaFields.add(new MetaField("metaTable", metaTable, "数据表元信息"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("字段元信息表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }
        // user 用户表
        if (!metaTableRepository.existsByName("user")) {
            var metaTable = metaTableRepository.save(new MetaTable("user", "用户表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("username", metaTable, "用户名"));
            metaFields.add(new MetaField("password", metaTable, "密码"));
            metaFields.add(new MetaField("name", metaTable, "昵称"));
            metaFields.add(new MetaField("email", metaTable, "邮箱"));
            metaFields.add(new MetaField("phone", metaTable, "电话"));
            metaFields.add(new MetaField("avatar", metaTable, "头像"));
            metaFields.add(new MetaField("expiryTime", metaTable, "过期时间"));
            metaFields.add(new MetaField("locked", metaTable, "锁定"));
            metaFields.add(new MetaField("credentialsExpiryTime", metaTable, "凭证过期时间"));
            metaFields.add(new MetaField("disabled", metaTable, "禁用"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("role", metaTable, "角色"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("用户表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }
        // role 角色表
        if (!metaTableRepository.existsByName("role")) {
            var metaTable = metaTableRepository.save(new MetaTable("role", "角色表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("permission", metaTable, "权限"));
            metaFields.add(new MetaField("user", metaTable, "用户"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("角色表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }
        // permission 权限表
        if (!metaTableRepository.existsByName("permission")) {
            var metaTable = metaTableRepository.save(new MetaTable("permission", "权限表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("metaTable", metaTable, "数据表元信息"));
            metaFields.add(new MetaField("field", metaTable, "字段权限"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("role", metaTable, "角色"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("权限表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }
        // file 文件表
        if (!metaTableRepository.existsByName("file")) {
            var metaTable = metaTableRepository.save(new MetaTable("file", "文件表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("filename", metaTable, "文件名"));
            metaFields.add(new MetaField("originalFilename", metaTable, "原始文件名"));
            metaFields.add(new MetaField("contentType", metaTable, "类型"));
            metaFields.add(new MetaField("size", metaTable, "大小"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("文件表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }

        // figure 人物表
        if (!metaTableRepository.existsByName("figure")) {
            var metaTable = metaTableRepository.save(new MetaTable("figure", "人物表"));
            List<MetaField> metaFields = new ArrayList<>();
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("portrait", metaTable, "肖像"));
            metaFields.add(new MetaField("name", metaTable, "名字"));
            metaFields.add(new MetaField("fullname", metaTable, "全名"));
            metaFields.add(new MetaField("birthday", metaTable, "生辰"));
            metaFields.add(new MetaField("deathday", metaTable, "忌辰"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);

            Permission permission = new Permission();
            permission.setName("人物表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
        }

        if (permissions.size() > 0) {
            List<Permission> permissions1 = permissionRepository.saveAll(permissions);
            try {
                Role role = roleRepository.findByName("admin").get();
                role.getPermission().addAll(permissions1);
                roleRepository.save(role);
            } catch (NoSuchElementException e) {
                Role role = new Role();
                role.setName("admin");
                role.setPermission(permissions1);
                Role role1 = roleRepository.save(role);
                List<User> users = new ArrayList<>();
                User user = new User("administrator");
                user.setPassword(passwordEncoder.encode("administrator"));
                users.add(user);
                user = new User("admin");
                user.setPassword(passwordEncoder.encode("admin"));
                user.setRole(List.of(role1));
                users.add(user);
                userRepository.saveAll(users);
            }
        }
    }
}
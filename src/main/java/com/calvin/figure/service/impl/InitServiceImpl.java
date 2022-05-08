package com.calvin.figure.service.impl;

import java.util.ArrayList;
import java.util.List;

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

            List<MetaTable> metaTables = new ArrayList<>();
            metaTables.add(new MetaTable("metaSystem", "系统元信息表"));
            metaTables.add(new MetaTable("metaTable", "数据表元信息表"));
            metaTables.add(new MetaTable("metaField", "字段元信息表"));
            metaTables.add(new MetaTable("user", "用户表"));
            metaTables.add(new MetaTable("role", "角色表"));
            metaTables.add(new MetaTable("permission", "权限表"));
            metaTables.add(new MetaTable("file", "文件表"));

            List<MetaTable> metaTables1 = metaTableRepository.saveAll(metaTables);

            List<MetaField> metaFields = new ArrayList<>();
            List<Permission> permissions = new ArrayList<>();
            // metaSystem 系统元信息表
            MetaTable metaTable = metaTables1.get(0);
            Permission permission = new Permission();
            permission.setName("系统元信息表全部");
            TextNode field = JsonNodeFactory.instance.textNode("*");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
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
            // metaTable 数据表元信息表
            metaFields.clear();
            metaTable = metaTables1.get(1);
            permission = new Permission();
            permission.setName("数据表元信息表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("displayName", metaTable, "显示名称"));
            metaFields.add(new MetaField("metaField", metaTable, "字段元信息"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);
            // metaField 字段元信息表
            metaFields.clear();
            metaTable = metaTables1.get(2);
            permission = new Permission();
            permission.setName("字段元信息表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("displayName", metaTable, "显示名称"));
            metaFields.add(new MetaField("metaTable", metaTable, "数据表元信息"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);
            // user 用户表
            metaFields.clear();
            metaTable = metaTables1.get(3);
            permission = new Permission();
            permission.setName("用户表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("username", metaTable, "用户名"));
            metaFields.add(new MetaField("password", metaTable, "密码"));
            metaFields.add(new MetaField("name", metaTable, "昵称"));
            metaFields.add(new MetaField("email", metaTable, "邮箱"));
            metaFields.add(new MetaField("phone", metaTable, "电话"));
            metaFields.add(new MetaField("expiryTime", metaTable, "过期时间"));
            metaFields.add(new MetaField("locked", metaTable, "锁定"));
            metaFields.add(new MetaField("credentialsExpiryTime", metaTable, "凭证过期时间"));
            metaFields.add(new MetaField("enabled", metaTable, "启用"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("role", metaTable, "角色"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);
            // role 角色表
            metaFields.clear();
            metaTable = metaTables1.get(4);
            permission = new Permission();
            permission.setName("角色表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
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
            // permission 权限表
            metaFields.clear();
            metaTable = metaTables1.get(5);
            permission = new Permission();
            permission.setName("权限表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
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
            // file 文件表
            metaFields.clear();
            metaTable = metaTables1.get(6);
            permission = new Permission();
            permission.setName("文件表全部");
            permission.setField(field);
            permission.setMetaTable(metaTable);
            permissions.add(permission);
            metaFields.add(new MetaField("id", metaTable, "主键"));
            metaFields.add(new MetaField("url", metaTable, "网址"));
            metaFields.add(new MetaField("name", metaTable, "名称"));
            metaFields.add(new MetaField("filename", metaTable, "文件名"));
            metaFields.add(new MetaField("contentType", metaTable, "类型"));
            metaFields.add(new MetaField("size", metaTable, "大小"));
            metaFields.add(new MetaField("remark", metaTable, "备注"));
            metaFields.add(new MetaField("creator", metaTable, "创建者"));
            metaFields.add(new MetaField("modifier", metaTable, "修改者"));
            metaFields.add(new MetaField("createTime", metaTable, "创建时间"));
            metaFields.add(new MetaField("updateTime", metaTable, "修改时间"));
            metaFieldRepository.saveAll(metaFields);
            List<Permission> permissions1 = permissionRepository.saveAll(permissions);

            List<Role> roles = new ArrayList<>();
            Role role = new Role();
            role.setName("admin");
            role.setPermission(permissions1);
            roles.add(role);
            var roles1 = roleRepository.saveAll(roles);

            List<User> users = new ArrayList<>();
            User user = new User("administrator");
            user.setPassword(passwordEncoder.encode("administrator"));
            users.add(user);
            user = new User("admin");
            user.setPassword(passwordEncoder.encode("admin"));
            user.setRole(List.of(roles1.get(0)));
            users.add(user);
            
            userRepository.saveAll(users);
        }

    }

}
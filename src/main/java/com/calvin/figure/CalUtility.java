package com.calvin.figure;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.calvin.figure.entity.User;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpServerErrorException;

public class CalUtility {

    private static final Logger logger = LoggerFactory.getLogger(CalUtility.class);

    public Set<String> getFields(Authentication auth, int type, String metaTableName) {
        Set<String> fields = new HashSet<>();
        if (AuthorityAuthorizationManager.hasRole("ADMINISTRATOR").check(() -> auth, null).isGranted()) {
            fields.add("*");
            return fields;
        }
        User user = (User) auth.getPrincipal();
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

    // 根据权限，保留source的那些有权限的字段，把没有权限的字段设置成null。
    public static <T> void copyFields(T source, Set<String> perms) {
        Field[] fields = source.getClass().getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            switch (name) {
                case "serialVersionUID":
                case "version":
                case "creator":
                case "modifier":
                    break;
                default:
                    try {
                        if (!perms.contains("*") && !perms.contains(name)) {
                            field.setAccessible(true);
                            field.set(source, null);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                    break;
            }
        }
    }

    public static <T> void copyFields(List<T> sources, Set<String> perms) {
        Iterator<T> it = sources.iterator();
        while (it.hasNext()) {
            T source = it.next();
            copyFields(source, perms);
        }
    }

    // 根据权限，把有可读权限的字段从source设置到target上。nulls如果为null，不复制值为null的字段，如果nulls不为null，nulls所包含的值为null的字段也要复制。"*"可以代表任意字段
    public static <T> void copyFields(T target, T source, Set<String> perms, Set<String> nulls) {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            switch (name) {
                case "serialVersionUID":
                case "id":
                case "creator":
                case "modifier":
                case "createTime":
                case "updateTime":
                case "version":
                    break;
                default:
                    try {
                        if (perms.contains("*") || perms.contains(name)) {
                            field.setAccessible(true);
                            Object fieldVal = field.get(source);
                            if (nulls != null && (nulls.contains("*") || nulls.contains(name))) {
                                field.set(target, fieldVal);
                            } else if (fieldVal != null) {
                                field.set(target, fieldVal);
                            }
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        logger.error(e.getMessage(), e);
                        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                    break;
            }
        }
    }

    public String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null) {
            return request.getRemoteAddr();
        } else {
            return xff.contains(",") ? xff.split(",")[0] : xff;
        }
    }

}

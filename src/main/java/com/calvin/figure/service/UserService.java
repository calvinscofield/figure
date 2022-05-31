package com.calvin.figure.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import com.calvin.figure.entity.User;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    void check(String metaTableName, String metaFieldName, int type, Authentication auth);

    void checkAny(String metaTableName, int type, Authentication auth);

    void checkAll(String metaTableName, int type, Authentication auth);

    void generateAuthorities(Collection<GrantedAuthority> authorities, User user);

    User generateAuthorities(Collection<GrantedAuthority> authorities, Integer id);

    User generateAuthorities(Collection<GrantedAuthority> authorities);

    User generateAnonymousUser();

    ObjectNode generatePerms(User user);

    ObjectNode generatePerms(Integer id);

    void sendLoginMail(String username, String ip);

    void sendLoginCodeMail(String email, String code);

    void sendRegisterCodeMail(String email, String code);

    boolean isAdministrator(User user);

    boolean isAdministrator(Integer id);

    UserDetails loadUserByEmail(String email) throws UsernameNotFoundException;

    User add(MultipartFile avatar, User value) throws IOException;

    User edit(MultipartFile avatar, User value) throws IOException;

    Set<String> getFields(Authentication auth, int type, String metaTableName);
}

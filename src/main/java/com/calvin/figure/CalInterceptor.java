package com.calvin.figure;

import java.util.ArrayList;
import java.util.Collection;

import javax.security.auth.login.AccountExpiredException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.calvin.figure.entity.User;
import com.calvin.figure.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

public class CalInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(CalInterceptor.class);

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            // 所有OPTIONS请求不做处理
        } else if (uri.equals("/users/me") && HttpMethod.GET.matches(method)) {
            // GET /users/me也不做处理
        } else {
            User user;
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (AuthorityAuthorizationManager.hasRole("ANONYMOUS").check(() -> auth, null).isGranted()) {
                user = userService.generateAuthorities(authorities);
                // throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "需要登录");
            } else {
                User me = (User) auth.getPrincipal();
                user = userService.generateAuthorities(authorities, me.getId());
            }
            // 每次都从数据库里获取用户最新的权限数据更新到Token里，这样就可以实现动态权限
            if (!user.isAccountNonLocked())
                throw new LockedException("用户帐号已被锁定");
            if (!user.isEnabled())
                throw new DisabledException("用户已失效");
            if (!user.isAccountNonExpired())
                throw new AccountExpiredException("用户帐号已过期");
            if (!user.isCredentialsNonExpired())
                throw new CredentialsExpiredException("用户凭证已过期");
            var field = AbstractAuthenticationToken.class.getDeclaredField("authorities");
            field.setAccessible(true);
            field.set(auth, authorities);
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

}

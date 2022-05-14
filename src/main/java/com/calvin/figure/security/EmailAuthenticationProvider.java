package com.calvin.figure.security;

import java.time.Instant;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.calvin.figure.service.UserService;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class EmailAuthenticationProvider implements AuthenticationProvider {

    private UserService userService;

    @Override
    @SuppressWarnings({"unchecked"})
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmailAuthenticationToken authenticationToken = (EmailAuthenticationToken) authentication;
        String email = (String) authenticationToken.getPrincipal();
        String code = (String) authenticationToken.getCredentials();
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession();
        Map<String, Object> emailCode = (Map<String, Object>) session.getAttribute("emailLogin");
        if (emailCode == null)
            throw new BadCredentialsException("未申请验证码");
        String email1 = (String) emailCode.get("email");
        if (!email.equals(email1))
            throw new BadCredentialsException("接收验证码的邮箱与登录邮箱不一致");
        String code1 = (String) emailCode.get("code");
        if (!code.equals(code1))
            throw new BadCredentialsException("验证码不正确");
        Instant expiry = (Instant) emailCode.get("expiry");
        if (Instant.now().isAfter(expiry))
            throw new BadCredentialsException("验证码已过期");
        UserDetails user = userService.loadUserByEmail(email);
        EmailAuthenticationToken successAuthentication = new EmailAuthenticationToken(user, code,
                user.getAuthorities());
        successAuthentication.setDetails(authenticationToken.getDetails());
        return successAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // 判断 authentication 是不是 EmailAuthenticationToken 的子类或子接口
        return EmailAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

}

package com.calvin.figure.security;

import com.calvin.figure.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class EmailAuthenticationSecurityConfig
        extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
    @Autowired
    private UserService userService;
    @Autowired
    private EmailAuthenticationSuccessHandler emailAuthenticationSuccessHandler;
    @Autowired
    private EmailAuthenticationFailureHandler emailAuthenticationFailureHandler;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        EmailAuthenticationFilter emailAuthenticationFilter = new EmailAuthenticationFilter();
        emailAuthenticationFilter.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
        emailAuthenticationFilter.setAuthenticationSuccessHandler(emailAuthenticationSuccessHandler);
        emailAuthenticationFilter.setAuthenticationFailureHandler(emailAuthenticationFailureHandler);

        EmailAuthenticationProvider emailAuthenticationProvider = new EmailAuthenticationProvider();
        emailAuthenticationProvider.setUserService(userService);

        http.authenticationProvider(emailAuthenticationProvider)
                .addFilterAfter(emailAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

package com.calvin.figure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.ForwardLogoutSuccessHandler;

@EnableWebSecurity
public class CalWebSecurityConfigurer {

    @Autowired
    private EmailAuthenticationSecurityConfig emailAuthenticationSecurityConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .apply(emailAuthenticationSecurityConfig).and()
                .authorizeRequests()
                .anyRequest().permitAll()
                .and().formLogin()
                .loginPage("/users/login")
                .successForwardUrl("/users/login")
                .failureForwardUrl("/users/login?error=true")
                .and().logout().logoutUrl("/users/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler(new ForwardLogoutSuccessHandler("/users/logout"));
        return http.build();
    }

}

package com.calvin.figure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.ForwardLogoutSuccessHandler;

@EnableWebSecurity
public class CalSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll()
                .and().formLogin()
                .loginPage("/users/login")
                .successForwardUrl("/users/login")
                .failureForwardUrl("/users/login?error=true")
                .and().logout().logoutUrl("/users/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler(new ForwardLogoutSuccessHandler("/users/logout"));
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return userService;
    }

}

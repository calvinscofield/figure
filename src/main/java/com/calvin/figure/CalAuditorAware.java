package com.calvin.figure;

import java.util.Optional;

import com.calvin.figure.entity.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CalAuditorAware implements AuditorAware<User> {

    @Override
    public Optional<User> getCurrentAuditor() {
        User user = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && User.class.isInstance(auth.getPrincipal())) {
            user = (User) auth.getPrincipal();
        }
        return Optional.ofNullable(user);
    }
}

package com.gm.api.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gm.core.domain.user.model.User;

@Getter
public class CustomUserPrincipal implements UserDetails {

    private static final String ROLE_USER = "ROLE_USER";

    private final UUID userId;
    private final User user;

    public CustomUserPrincipal(UUID userId, User user) {
        this.userId = userId;
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return userId.toString(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
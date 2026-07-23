package com.gm.api.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.gm.core.domain.user.model.User;

@Getter
public class CustomUserPrincipal implements UserDetails, OAuth2User {

    private static final String ROLE_USER = "ROLE_USER";

    private final UUID userId;
    private final User user;
    private final Map<String, Object> attributes;

    public CustomUserPrincipal(UUID userId, User user, Map<String, Object> attributes) {
        this.userId = userId;
        this.user = user;
        this.attributes = attributes;
    }

    /**
     * JWT 인증에서 사용할 생성자
     */
    public CustomUserPrincipal(UUID userId, User user) {
        this(userId, user, Map.of());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }

    /**
     * OAuth2에서 받은 원본 사용자 정보
     */
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    /**
     * Spring Security OAuth2에서 사용하는 사용자 이름. JWT subject와 동일한 UUID를 반환한다.
     */
    @Override
    public String getName() { return userId.toString(); }

    /**
     * UserDetails 구현
     */
    @Override
    public String getUsername() { return userId.toString(); }

    @Override
    public String getPassword() { return null; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
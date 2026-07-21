package com.gm.core.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private final UserRepository userRepository;

    /**
     * 회원 식별자로 회원을 조회한다.
     *
     * @param id 회원 UUID
     * @return 조회된 회원
     */
    public User findById(UUID id) {
        log.info("user 조회: Id={}", id);
        return userRepository.findById(id).orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    /**
     * 소셜 로그인 제공자와 제공자 회원 식별자로 회원을 조회한다.
     * 기존 회원이 없으면 신규 회원을 생성한다.
     *
     * @param provider 소셜 로그인 제공자
     * @param providerId 소셜 로그인 제공자의 회원 식별자
     * @param name 이름
     * @param email 이메일
     * @param phone 휴대폰 번호
     * @return 기존 회원 또는 새로 생성된 회원
     */
    public User findOrCreate(
            String name,
            String provider,
            String providerId,
            String phone,
            String email
    ) {
        return userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createUser(
                        name,
                        provider,
                        providerId,
                        phone,
                        email
                ));
    }

    private User createUser(
            String name,
            String provider,
            String providerId,
            String phone,
            String email
    ) {
        log.info(
                "신규 소셜 회원 생성: provider={}, providerId={}", provider, providerId
        );

        User user = new User(
                name,
                name,
                DEFAULT_STATUS,
                provider,
                providerId,
                phone,
                email,
                false
        );

        return userRepository.save(user);
    }
}
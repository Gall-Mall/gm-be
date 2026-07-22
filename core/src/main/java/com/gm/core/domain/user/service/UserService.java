package com.gm.core.domain.user.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.repository.UserRepository;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.model.UserResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 회원 식별자로 회원을 조회한다.
     *
     * @param id 회원 UUID
     * @return 조회된 회원
     */
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        log.debug("user 조회: Id: {}", id);

        return userRepository.findById(id).orElseThrow(() ->
                        new UserException(UserErrorCode.USER_NOT_FOUND)
                );
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
    @Transactional
    public User findOrCreate(
            String name,
            Provider provider,
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

    /**
     * OAuth2 로그인에서 사용할 회원(UUID + User)을 조회한다.
     * 기존 회원이 없으면 신규 회원을 생성한다.
     *
     * @param name 이름
     * @param provider 소셜 로그인 제공자
     * @param providerId 소셜 로그인 제공자의 회원 식별자
     * @param phone 휴대폰 번호
     * @param email 이메일
     * @return 회원 UUID와 도메인 회원 정보
     */
    @Transactional
    public UserResult findOrCreateWithId(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        return userRepository
                .findResultByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createUserResult(
                        name,
                        provider,
                        providerId,
                        phone,
                        email
                ));
    }

    private User createUser(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        return userRepository.save(
                User.create(
                        name,
                        UserStatus.ACTIVE,
                        provider,
                        providerId,
                        phone,
                        email
                )
        );
    }

    /**
     * OAuth2 로그인용 회원(UUID + User)을 생성한다.
     */
    private UserResult createUserResult(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        return userRepository.saveResult(
                User.create(
                        name,
                        UserStatus.ACTIVE,
                        provider,
                        providerId,
                        phone,
                        email
                )
        );
    }
}
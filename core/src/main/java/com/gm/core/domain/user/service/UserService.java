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
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.repository.UserRepository;

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
                        new UserException(UserErrorCode.USER_NOT_FOUND));
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
     * OAuth2 로그인에 사용할 회원 식별자와 회원 정보를 조회한다.
     * 기존 회원이 없으면 ONBOARDING 상태의 신규 회원을 생성한다.
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

    /**
     * ONBOARDING 상태의 신규 회원을 생성하고 저장한다.
     *
     * @return 저장된 회원
     */
    private User createUser(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        User newUser = createNewUser(
                name,
                provider,
                providerId,
                phone,
                email
        );

        return userRepository.save(newUser);
    }

    /**
     * OAuth2 로그인용 신규 회원을 생성하고 저장한 뒤 회원 UUID와 도메인 회원 정보를 함께 반환한다.
     *
     * @return 저장된 회원의 UUID와 도메인 회원 정보
     */
    private UserResult createUserResult(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        User newUser = createNewUser(
                name,
                provider,
                providerId,
                phone,
                email
        );

        return userRepository.saveResult(newUser);
    }

    /**
     * ONBOARDING 상태의 신규 회원 도메인 객체를 생성한다.
     * 실제 DB 저장은 호출한 메서드에서 수행한다.
     */
    private User createNewUser(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        return User.create(
                name,
                provider,
                providerId,
                phone,
                email
        );
    }
}
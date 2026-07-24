package com.gm.core.domain.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gm.core.domain.user.model.*;
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
    private final UserAllergenService userAllergenService;
    private final UserCategoryService userCategoryService;
    private final UserMenuService userMenuService;

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
            UserStatus userStatus,
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
        return userRepository.save(
                User.create(
                        name,
                        provider,
                        providerId,
                        phone,
                        email
                )
        );
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

    /**
     *  온보딩 제출
     */
    @Transactional
    public void submitOnboarding(UUID userId, Onboarding onboarding) {
        validateOnboardingSetting(onboarding.userSetting());
        saveUserSetting(userId,onboarding.userSetting());
        completeOnboarding(userId, onboarding.termsAgreed());
    }


    /**
     *  유저 세팅 변경
     */
    @Transactional
    public void changeUserSetting(UUID userId, UserSetting userSetting) {
        validateUserSetting(userSetting);
        deleteUserSettings(userId);
        saveUserSetting(userId, userSetting);
    }

    /**
     *  유저 세팅 조회
     */
    public UserSetting getUserSetting(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        Map<UserMenuPreference, List<UUID>> userMenuPreferenceIds = getUserMenuPreferenceIds(userId);
        Map<UserCategoryPreference, List<UUID>> userCategoryPreferenceIds = getUserCategoryPreferenceIds(userId);

        return new UserSetting(
                getUserAllergenIds(userId),
                userMenuPreferenceIds.get(UserMenuPreference.LIKE),
                userMenuPreferenceIds.get(UserMenuPreference.EXCLUDE),
                userCategoryPreferenceIds.get(UserCategoryPreference.LIKE),
                userCategoryPreferenceIds.get(UserCategoryPreference.DISLIKE),
                user.customAllergenText(),
                user.preferenceText(),
                user.excludeFoodText()
        );
    }

    /**
     *  UserSetting 유효성 검증
     */
    private void validateUserSetting(UserSetting userSetting) {
        validateInputText(userSetting);
        validateOnboardingSetting(userSetting);
    }

    private void validateOnboardingSetting(UserSetting userSetting) {
        validateCategoryOverlap(userSetting.preferredCategoryIds(), userSetting.excludedCategoryIds());
        validateMenuOverlap(userSetting.preferredMenuIds(), userSetting.excludedMenuIds());
    }

    private void validateInputText(UserSetting userSetting) {
        if(userSetting.allergenText() == null || userSetting.preferredText() == null || userSetting.excludedText() == null) {
            throw new UserException(UserErrorCode.INVALID_USER_INPUT);
        }
    }

    /**
     *  UserSetting 삭제
     */
    private void deleteUserSettings(UUID userId) {
        userAllergenService.deleteUserAllergens(userId);
        userCategoryService.deleteUserCategoryPreferences(userId);
        userMenuService.deleteUserMenuPreferences(userId);
    }

    /**
     *  UserSetting 저장
     */
    private void saveUserSetting(UUID userId, UserSetting userSetting) {

        userRepository.saveUserInputText(
                userId,
                userSetting.allergenText(),
                userSetting.preferredText(),
                userSetting.excludedText());

        saveUserAllergen(userId, userSetting.allergenIds());
        saveUserPreferredMenu(userId, userSetting.preferredMenuIds(), userSetting.excludedMenuIds());
        saveUserPreferredCategory(userId, userSetting.preferredCategoryIds(), userSetting.excludedCategoryIds());
    }

    /**
     *
     * 온보딩 시 유저의 terms_agreed 를 true 로 user_status를 active로 변경한다
     */
    private void completeOnboarding(UUID userId, boolean termsAgreed) {
        if(!termsAgreed) {
            throw new UserException(UserErrorCode.TERMS_AGREED_APPROVE);
        }
        userRepository.updateUserStatus(userId);
    }

    /**
     * 유저가 선택한 알러지 ID 조회
     */
    private List<UUID> getUserAllergenIds(UUID userId) {
        return userAllergenService.getUserAllergens(userId).stream().map(UserAllergen::allergenId).toList();
    }

    /**
     * 유저가 선택한 메뉴ID 조회
     */
    private Map<UserMenuPreference, List<UUID>> getUserMenuPreferenceIds(UUID userId) {
        HashMap<UserMenuPreference,List<UUID>> userMenuPreferenceIdMap =  new HashMap<>();
        List<UserMenu> userMenuPreferences = userMenuService.getUserMenuPreferences(userId);

        List<UUID> prefer = userMenuPreferences.stream().filter(i -> i.preference() == UserMenuPreference.LIKE).map(UserMenu::menuId).toList();
        List<UUID> exclude = userMenuPreferences.stream().filter(i -> i.preference() == UserMenuPreference.EXCLUDE).map(UserMenu::menuId).toList();

        userMenuPreferenceIdMap.put(UserMenuPreference.LIKE, prefer);
        userMenuPreferenceIdMap.put(UserMenuPreference.EXCLUDE, exclude);

        return userMenuPreferenceIdMap;
    }

    /**
     * 유저가 선택한 카테고리 ID 조회
     */
    private Map<UserCategoryPreference, List<UUID>> getUserCategoryPreferenceIds(UUID userId) {

        HashMap<UserCategoryPreference,List<UUID>> userMenuPreferenceIdMap =  new HashMap<>();
        List<UserCategory> userCategoryPreferences = userCategoryService.getUserCategoryPreferences(userId);

        List<UUID> like = userCategoryPreferences.stream().filter(i -> i.preference() == UserCategoryPreference.LIKE).map(UserCategory::categoryId).toList();
        List<UUID> dislike = userCategoryPreferences.stream().filter(i -> i.preference() == UserCategoryPreference.DISLIKE).map(UserCategory::categoryId).toList();

        userMenuPreferenceIdMap.put(UserCategoryPreference.LIKE, like);
        userMenuPreferenceIdMap.put(UserCategoryPreference.DISLIKE, dislike);

        return userMenuPreferenceIdMap;
    }

    /**
     * 유저가 선택한 알러지 정보를 유저에게 저장
     */
    private void saveUserAllergen(UUID userId, List<UUID> allergenIds) {
        userAllergenService.saveUserAllergens(userId, allergenIds);
    }

    /**
     * 유저가 선택한 메뉴를 저장
     */
    private void saveUserPreferredMenu(UUID userId, List<UUID> preferredMenuIds, List<UUID> excludedMenuIds) {
        userMenuService.saveUserMenuPreference(userId, preferredMenuIds, excludedMenuIds);
    }

    /**
     * 유저가 선택한 카테고리를 저장
     */
    private void saveUserPreferredCategory(UUID userId, List<UUID> preferredCategoryIds, List<UUID> disLikeCategoryIds) {
        userCategoryService.saveUserCategoryPreference(userId, preferredCategoryIds, disLikeCategoryIds);
    }

    /**
     * 선호 비선호 카테고리 중복에러
     */
    private void validateCategoryOverlap(List<UUID> preferredIds, List<UUID> excludedIds) {
        for (UUID excludedId : excludedIds) {
            if (preferredIds.contains(excludedId)) {
                throw new UserException(UserErrorCode.CATEGORY_PREFERENCE_CONFLICT);
            }
        }
    }

    /**
     * 선호 비선호 메뉴 중복 에러
     */
    private void validateMenuOverlap(List<UUID> preferredIds, List<UUID> excludedIds) {
        for (UUID excludedId : excludedIds) {
            if (preferredIds.contains(excludedId)) {
                throw new UserException(UserErrorCode.MENU_PREFERENCE_CONFLICT);
            }
        }
    }
}
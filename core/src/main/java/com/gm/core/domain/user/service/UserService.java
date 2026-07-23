package com.gm.core.domain.user.service;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
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
        /** 인증 요청마다 발생하는 로그이므로 'info → debug'로 수정했습니다. */
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
                        email,
                        false,
                        "",
                        ""
                )
        );
    }

    /**
     *  온보딩 제출
     */
    @Transactional
    public void submitOnboarding(UUID userId, Onboarding onboarding) {
        updateTermsAgreed(userId, onboarding.termsAgreed());
        changeUserSetting(userId, onboarding.userSetting());
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
        String allergenText = getCustomAllergenText(userId);
        String preferredText = getPreferenceText(userId);
        String excludedText = getCustomAllergenText(userId);

        return new UserSetting(
                getUserAllergenIds(userId),
                getUserPreferredMenuIds(userId),
                getUserExcludedMenuIds(userId),
                getUserPreferredCategoryIds(userId),
                getUserExcludedCategoryIds(userId),
                allergenText,
                preferredText,
                excludedText
        );
    }

    /**
     *  유저의 TermsAgreed를 수정
     */
    private void updateTermsAgreed(UUID userId, Boolean termsAgreed) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        userRepository.save(user.updateTermsAgreed(termsAgreed));
    }

    /**
     *  유저의 AllergenText 조회
     */
    private String getCustomAllergenText(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return user.customAllergenText();
    }

    /**
     *  유저의 Preference 조회
     */
    private String getPreferenceText(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return user.preferenceText();
    }

    private void validateUserSetting(UserSetting userSetting) {
        validateCategoryOverlap(userSetting.preferredCategoryIds(), userSetting.excludedCategoryIds());
        validateMenuOverlap(userSetting.preferredMenuIds(), userSetting.excludedMenuIds());
    }

    private void deleteUserSettings(UUID userId) {
        userAllergenService.deleteUserAllergens(userId);
        userCategoryService.deleteUserCategoryPreferences(userId);
        userMenuService.deleteUserMenuPreferences(userId);
    }

    private void saveUserSetting(UUID userId, UserSetting userSetting) {
        saveUserAllergen(
                userId,
                userSetting.allergenIds(),
                userSetting.allergenText()
        );
        saveUserPreferredMenu(
                userId,
                userSetting.preferredMenuIds(),
                userSetting.preferredText()
        );
        saveUserExcludedMenu(
                userId,
                userSetting.preferredMenuIds(),
                userSetting.excludedText()
        );
        saveUserPreferredCategory(
                userId,
                userSetting.preferredCategoryIds(),
                userSetting.preferredText()
        );
        saveUserExcludedCategory(
                userId,
                userSetting.excludedCategoryIds(),
                userSetting.excludedText()
        );
    }

    /**
     * 유저가 선택한 알러지 ID
     */
    private List<UUID> getUserAllergenIds(UUID userId) {
        return userAllergenService.getUserAllergens(userId).stream().map(UserAllergen::allergenId).toList();
    }

    /**
     * 유저가 선택한 선호메뉴 ID
     */
    private List<UUID> getUserPreferredMenuIds(UUID userId) {
        List<UserMenu> list = userMenuService.getUserMenuPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserPreference.LIKE)
                .toList();
        return list.stream().map(UserMenu::menuId).toList();
    }

    /**
     * 유저가 선택한 비선호메뉴ID
     */
    private List<UUID> getUserExcludedMenuIds(UUID userId) {
        return userMenuService.getUserMenuPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserPreference.EXCLUDE)
                .map(UserMenu::menuId)
                .toList();
    }

    /**
     * 유저가 선택한 선호카테고리ID
     */
    private List<UUID> getUserPreferredCategoryIds(UUID userId) {
        return userCategoryService.getUserCategoryPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserPreference.LIKE)
                .map(UserCategory::categoryId)
                .toList();
    }

    /**
     * 유저가 선택한 비선호카테고리ID
     */
    private List<UUID> getUserExcludedCategoryIds(UUID userId) {
        return userCategoryService.getUserCategoryPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserPreference.EXCLUDE)
                .map(UserCategory::categoryId)
                .toList();
    }

    /**
     * 유저가 선택한 알러지 정보(선택 알러지 + 자유텍스트)를 유저에게 저장
     */
    private void saveUserAllergen(UUID userId, List<UUID> allergenIds, String userInputText) {
         if (!userInputText.isBlank()) {
             saveUserCustomAllergenText(userId);
         }
        userAllergenService.saveUserAllergens(userId, allergenIds);
    }

    /**
     * 유저가 선택한 선호 메뉴를 저장
     */
    private void saveUserPreferredMenu(UUID userId, List<UUID> menuIds, String userInputText) {
         if (!userInputText.isBlank()) {
             savePreferenceText(userId);
         }
        userMenuService.saveUserMenuPreference(
                userId,
                menuIds,
                UserPreference.LIKE
        );
    }

    /**
     * 유저가 선택한 비선호 메뉴를 저장
     */
    private void saveUserExcludedMenu(UUID userId, List<UUID> menuIds, String userInputText) {
         if (!userInputText.isBlank()) {
             saveUserCustomAllergenText(userId);
         }
        userMenuService.saveUserMenuPreference(
                userId,
                menuIds,
                UserPreference.EXCLUDE
        );
    }
    /**
     * 유저가 선택한 선호 카테고리 저장
     */
    private void saveUserPreferredCategory(UUID userId, List<UUID> categoryIds, String userInputText) {
         if (!userInputText.isBlank()) {
             savePreferenceText(userId);
         }
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserPreference.EXCLUDE);
    }

    /**
     * 유저가 선택한 비선호 카테고리 저장
     */
    private void saveUserExcludedCategory(UUID userId, List<UUID> categoryIds, String userInputText) {
         if (!userInputText.isBlank()) {
             saveUserCustomAllergenText(userId);
         }
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserPreference.EXCLUDE);
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

    private void savePreferenceText(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        User savedUser = user.updatePreferenceText(user.preferenceText());
        userRepository.save(savedUser);

    }

    private void saveUserCustomAllergenText(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        User savedUser = user.updateCustomAllergenText(user.customAllergenText());
        userRepository.save(savedUser);

    }
}
package com.gm.core.domain.user.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.*;
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
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        String allergenText = user.customAllergenText();
        String preferredText = user.preferenceText();
        String excludedText = user.excludeFoodText();

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

    /**
     *  유저의 excludeFood 조회
     */
    private String getExcludeFoodText(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return user.excludeFoodText();
    }

    /**
     *  UserSetting 유효성 검증
     */
    private void validateUserSetting(UserSetting userSetting) {
        validateCategoryOverlap(userSetting.preferredCategoryIds(), userSetting.excludedCategoryIds());
        validateMenuOverlap(userSetting.preferredMenuIds(), userSetting.excludedMenuIds());
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
                userSetting.excludedMenuIds(),
                userSetting.excludedText()
        );
        saveUserPreferredCategory(
                userId,
                userSetting.preferredCategoryIds()
        );
        saveUserExcludedCategory(
                userId,
                userSetting.excludedCategoryIds()
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
                .filter(i -> i.preference() == UserMenuPreference.LIKE)
                .toList();
        return list.stream().map(UserMenu::menuId).toList();
    }

    /**
     * 유저가 선택한 비선호메뉴ID
     */
    private List<UUID> getUserExcludedMenuIds(UUID userId) {
        return userMenuService.getUserMenuPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserMenuPreference.EXCLUDE)
                .map(UserMenu::menuId)
                .toList();
    }

    /**
     * 유저가 선택한 선호카테고리ID
     */
    private List<UUID> getUserPreferredCategoryIds(UUID userId) {
        return userCategoryService.getUserCategoryPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserCategoryPreference.LIKE)
                .map(UserCategory::categoryId)
                .toList();
    }

    /**
     * 유저가 선택한 비선호카테고리ID
     */
    private List<UUID> getUserExcludedCategoryIds(UUID userId) {
        return userCategoryService.getUserCategoryPreferences(userId)
                .stream()
                .filter(i -> i.preference() == UserCategoryPreference.DISLIKE)
                .map(UserCategory::categoryId)
                .toList();
    }

    /**
     * 유저가 선택한 알러지 정보(선택 알러지 + 자유텍스트)를 유저에게 저장
     */
    private void saveUserAllergen(UUID userId, List<UUID> allergenIds, String userInputText) {
         if (userInputText != null && !userInputText.isBlank()) {
             saveUserCustomAllergenText(userId, userInputText);
         }
        userAllergenService.saveUserAllergens(userId, allergenIds);
    }

    /**
     * 유저가 선택한 선호 메뉴를 저장
     */
    private void saveUserPreferredMenu(UUID userId, List<UUID> menuIds, String userInputText) {
         if (userInputText != null && !userInputText.isBlank()) {
             savePreferenceText(userId, userInputText);
         }
        userMenuService.saveUserMenuPreference(
                userId,
                menuIds,
                UserMenuPreference.LIKE
        );
    }

    /**
     * 유저가 선택한 비선호 메뉴를 저장
     */
    private void saveUserExcludedMenu(UUID userId, List<UUID> menuIds, String userInputText) {
         if (userInputText != null && !userInputText.isBlank()) {
             saveUserExcludeText(userId, userInputText);
         }
        userMenuService.saveUserMenuPreference(
                userId,
                menuIds,
                UserMenuPreference.EXCLUDE
        );
    }
    /**
     * 유저가 선택한 선호 카테고리 저장
     */
    private void saveUserPreferredCategory(UUID userId, List<UUID> categoryIds) {
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserCategoryPreference.LIKE);
    }

    /**
     * 유저가 선택한 비선호 카테고리 저장
     */
    private void saveUserExcludedCategory(UUID userId, List<UUID> categoryIds) {
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserCategoryPreference.DISLIKE);
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

    private void saveUserCustomAllergenText(UUID userId, String userInputText) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        User savedUser = user.updateCustomAllergenText(userInputText);
        userRepository.save(savedUser);
    }

    private void savePreferenceText(UUID userId, String userInputText) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        User savedUser = user.updatePreferenceText(userInputText);
        userRepository.save(savedUser);

    }

    private void saveUserExcludeText(UUID userId, String userInputText) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        User savedUser = user.updateExcludeFoodText(userInputText);
        userRepository.save(savedUser);
    }
}
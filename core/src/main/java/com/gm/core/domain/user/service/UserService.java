package com.gm.core.domain.user.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.menu.repository.AllergenRepository;
import com.gm.core.domain.menu.repository.CategoryRepository;
import com.gm.core.domain.menu.repository.MenuRepository;
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
    private final AllergenRepository allergenRepository;
    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

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
        validateTermsAgreement(onboarding.termsAgreed());
        validateOnboardingSetting(onboarding.userSetting());
        validateMasterIds(onboarding.userSetting());
        completeOnboarding(userId);
        deleteUserSettings(userId);
        saveUserSetting(userId,onboarding.userSetting());
    }


    /**
     *  유저 세팅 변경
     */
    @Transactional
    public void changeUserSetting(UUID userId, UserSetting userSetting) {
        validateUserSetting(userSetting);
        validateMasterIds(userSetting);
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

    private void validateMasterIds(UserSetting userSetting) {
        validateAllergenIds(userSetting.allergenIds());
        validateMenuIds(userSetting.preferredMenuIds(), userSetting.excludedMenuIds());
        validateCategoryIds(userSetting.preferredCategoryIds(), userSetting.excludedCategoryIds());
    }

    private void validateAllergenIds(List<UUID> allergenIds) {
        if (allergenIds.isEmpty()) {
            return;
        }

        Set<UUID> requestedIds = new HashSet<>(allergenIds);
        Set<UUID> existingIds = allergenRepository.findExistingIds(requestedIds);
        if (!existingIds.containsAll(requestedIds)) {
            throw new UserException(UserErrorCode.ALLERGEN_NOT_FOUND);
        }
    }

    private void validateMenuIds(List<UUID> preferredIds, List<UUID> excludedIds) {
        Set<UUID> requestedIds = new HashSet<>(preferredIds);
        requestedIds.addAll(excludedIds);
        if (requestedIds.isEmpty()) {
            return;
        }

        Set<UUID> existingIds = menuRepository.findExistingIds(requestedIds);
        if (!existingIds.containsAll(requestedIds)) {
            throw new UserException(UserErrorCode.MENU_NOT_FOUND);
        }
    }

    private void validateCategoryIds(List<UUID> preferredIds, List<UUID> excludedIds) {
        Set<UUID> requestedIds = new HashSet<>(preferredIds);
        requestedIds.addAll(excludedIds);
        if (requestedIds.isEmpty()) {
            return;
        }

        Set<UUID> existingIds = categoryRepository.findExistingIds(requestedIds);
        if (!existingIds.containsAll(requestedIds)) {
            throw new UserException(UserErrorCode.CATEGORY_NOT_FOUND);
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

    /** 필수 약관 동의 여부를 확인한다. */
    private void validateTermsAgreement(boolean termsAgreed) {
        if(!termsAgreed) {
            throw new UserException(UserErrorCode.TERMS_NOT_AGREED);
        }
    }

    private void completeOnboarding(UUID userId) {
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

        HashMap<UserCategoryPreference,List<UUID>> userCategoryPreferenceIdMap =  new HashMap<>();
        List<UserCategory> userCategoryPreferences = userCategoryService.getUserCategoryPreferences(userId);

        List<UUID> like = userCategoryPreferences.stream().filter(i -> i.preference() == UserCategoryPreference.LIKE).map(UserCategory::categoryId).toList();
        List<UUID> dislike = userCategoryPreferences.stream().filter(i -> i.preference() == UserCategoryPreference.DISLIKE).map(UserCategory::categoryId).toList();

        userCategoryPreferenceIdMap.put(UserCategoryPreference.LIKE, like);
        userCategoryPreferenceIdMap.put(UserCategoryPreference.DISLIKE, dislike);

        return userCategoryPreferenceIdMap;
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
        Set<UUID> preferredIdSet = new HashSet<>(preferredIds);
        if (excludedIds.stream().anyMatch(preferredIdSet::contains)) {
            throw new UserException(UserErrorCode.CATEGORY_PREFERENCE_CONFLICT);
        }
    }

    /**
     * 선호 비선호 메뉴 중복 에러
     */
    private void validateMenuOverlap(List<UUID> preferredIds, List<UUID> excludedIds) {
        Set<UUID> preferredIdSet = new HashSet<>(preferredIds);
        if (excludedIds.stream().anyMatch(preferredIdSet::contains)) {
            throw new UserException(UserErrorCode.MENU_PREFERENCE_CONFLICT);
        }
    }
}

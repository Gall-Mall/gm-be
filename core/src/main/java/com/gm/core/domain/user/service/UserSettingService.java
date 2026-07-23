package com.gm.core.domain.user.service;

import com.gm.core.domain.menu.menu.service.MenuService;
import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final UserAllergenService userAllergenService;
    private final MenuService menuService;
    private final UserCategoryService userCategoryService;
    private final UserService userService;
    private final UserMenuService userMenuService;


    /**
     *  온보딩 제출
     */
    @Transactional
    public void submitOnboarding(UUID userId, Onboarding onboarding) {
//        userService.updateTermsAgreed(userId, onboarding.termsAgreed());
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
        String allergenText = ""; // userService.getCustomAllergenText(userId);
        String preferredText = ""; // userService.getPreferredText(userId);
        String excludedText = ""; // userService.getCustomAllergenText(userId);

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
        // if (!userInputText.isBlank()) {
        //     userService.saveUserCustomAllergenText();
        // }
        userAllergenService.saveUserAllergens(userId, allergenIds);
    }

    /**
     * 유저가 선택한 선호 메뉴를 저장
     */
    private void saveUserPreferredMenu(UUID userId, List<UUID> menuIds, String userInputText) {
        // if (!userInputText.isBlank()) {
        //     userService.savePreferenceText();
        // }
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
        // if (!userInputText.isBlank()) {
        //     userService.savePreferenceText();
        // }
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
        // if (!userInputText.isBlank()) {
        //     userService.savePreferenceText();
        // }
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserPreference.EXCLUDE);
    }

    /**
     * 유저가 선택한 비선호 카테고리 저장
     */
    private void saveUserExcludedCategory(UUID userId, List<UUID> categoryIds, String userInputText) {
        // if (!userInputText.isBlank()) {
        //     userService.savePreferenceText();
        // }
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
}

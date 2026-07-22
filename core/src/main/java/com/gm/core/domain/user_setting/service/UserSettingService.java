package com.gm.core.domain.user_setting.service;

import com.gm.core.domain.menu.menu.service.MenuService;
import com.gm.core.domain.menu.menu.model.Menu;
import com.gm.core.domain.user.service.UserService;
import com.gm.core.domain.user_setting.exception.UserSettingErrorCode;
import com.gm.core.domain.user_setting.exception.UserSettingException;
import com.gm.core.domain.user_setting.model.UserSetting;
import com.gm.core.domain.user_setting.user_preference.user_allergen.model.UserAllergen;
import com.gm.core.domain.user_setting.user_preference.user_allergen.service.UserAllergenService;
import com.gm.core.domain.user_setting.user_preference.user_category.model.UserCategory;
import com.gm.core.domain.user_setting.user_preference.user_category.service.UserCategoryService;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserMenu;
import com.gm.core.domain.user_setting.user_preference.user_menu.service.UserMenuService;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 유저세팅조회
 * 유저세팅변경
 */
@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final UserAllergenService userAllergenService;
    private final MenuService menuService;
    private final UserCategoryService userCategoryService;
    private final UserService userService;
    private final UserMenuService userMenuService;

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
        saveUserPreferred(
                userId,
                userSetting.preferredCategoryIds(),
                userSetting.preferredMenuIds(),
                userSetting.preferredText()
        );
        saveUserExcluded(
                userId,
                userSetting.excludedCategoryIds(),
                userSetting.excludedMenuIds(),
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
     * 유저가 선택한 선호 카테고리와 선호 메뉴를 저장
     */
    private void saveUserPreferred(UUID userId, List<UUID> categoryIds, List<UUID> menuIds, String userInputText) {
        // if (!userInputText.isBlank()) {
        //     userService.savePreferenceText();
        // }
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserPreference.LIKE);
        userMenuService.saveUserMenuPreference(
                userId,
                toDistinctMenuIds(categoryIds, menuIds),
                UserPreference.LIKE
        );
    }

    /**
     * 유저가 선택한 비선호 카테고리와 비선호 메뉴를 저장
     */
    private void saveUserExcluded(UUID userId, List<UUID> categoryIds, List<UUID> menuIds, String userInputText) {
        // if (!userInputText.isBlank()) {
        //     userService.savePreferenceText();
        // }
        userCategoryService.saveUserCategoryPreference(userId, categoryIds, UserPreference.EXCLUDE);
        userMenuService.saveUserMenuPreference(
                userId,
                toDistinctMenuIds(categoryIds, menuIds),
                UserPreference.EXCLUDE
        );
    }

    /**
     * 카테고리에 속한 메뉴 ID와 입력받은 메뉴 ID의 중복 제거
     */
    private List<UUID> toDistinctMenuIds(List<UUID> categoryIds, List<UUID> menuIds) {
        Set<UUID> mergedMenuIds = new HashSet<>(menuIds);

        for (UUID categoryId : categoryIds) {
            List<Menu> menusInCategory = menuService.findMenusByCategoryId(categoryId);
            for (Menu menu : menusInCategory) {
                mergedMenuIds.add(menu.id());
            }
        }
        return mergedMenuIds.stream().toList();
    }

    /**
     * 선호 비선호 카테고리 중복에러
     */
    private void validateCategoryOverlap(List<UUID> preferredIds, List<UUID> excludedIds) {
        for (UUID excludedId : excludedIds) {
            if (preferredIds.contains(excludedId)) {
                throw new UserSettingException(UserSettingErrorCode.CATEGORY_PREFERENCE_CONFLICT);
            }
        }
    }

    /**
     * 선호 비선호 메뉴 중복 에러
     */
    private void validateMenuOverlap(List<UUID> preferredIds, List<UUID> excludedIds) {
        for (UUID excludedId : excludedIds) {
            if (preferredIds.contains(excludedId)) {
                throw new UserSettingException(UserSettingErrorCode.MENU_PREFERENCE_CONFLICT);
            }
        }
    }
}

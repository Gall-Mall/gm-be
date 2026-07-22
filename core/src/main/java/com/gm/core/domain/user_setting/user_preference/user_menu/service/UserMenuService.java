package com.gm.core.domain.user_setting.user_preference.user_menu.service;

import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserMenu;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import com.gm.core.domain.user_setting.user_preference.user_menu.repository.UserMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserMenuService {
    /**
     * 유저가 선택한 메뉴들의 조회
     * 유저가 선택한 메뉴들의 삭제
     * 유저가 메뉴를 선택하면 user_menu_preference에 추가
     */

    private final UserMenuRepository userMenuRepository;

    public List<UserMenu> getUserMenuPreferences(UUID userId) {
        return userMenuRepository.findByUserId(userId);
    }

    public void deleteUserMenuPreferences(UUID userId) {
        userMenuRepository.deleteByUserId(userId);
    }

    public void saveUserMenuPreference(UUID userId, List<UUID> menuIds, UserPreference preference) {
        userMenuRepository.addUserMenuPreference(userId, menuIds, preference);
    }
}

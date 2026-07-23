package com.gm.core.domain.user.service;

import com.gm.core.domain.user.model.UserMenu;
import com.gm.core.domain.user.model.UserMenuPreference;
import com.gm.core.domain.user.repository.UserMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserMenuService {

    private final UserMenuRepository userMenuRepository;

    /**
     * 유저가 선택한 메뉴들의 조회
     */
    public List<UserMenu> getUserMenuPreferences(UUID userId) {
        return userMenuRepository.findByUserId(userId);
    }

    /**
     * 유저가 선택한 메뉴들의 삭제
     */
    public void deleteUserMenuPreferences(UUID userId) {
        userMenuRepository.deleteByUserId(userId);
    }

    /**
     * 유저가 메뉴를 선택하면 user_menu_preference에 추가
     */
    public void saveUserMenuPreference(UUID userId, List<UUID> menuIds, UserMenuPreference preference) {
        userMenuRepository.addUserMenuPreference(userId, menuIds, preference);
    }
}

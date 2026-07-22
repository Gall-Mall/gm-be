package com.gm.core.domain.menu.menu.service;

import com.gm.core.domain.menu.menu.model.Menu;
import com.gm.core.domain.menu.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {
    /**
     * menu 테이블을 모두 조회
     * 카테고리ID를 기준으로 메뉴 조회
     */
    private final MenuRepository menuRepository;

    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    public List<Menu> findMenusByCategoryId(UUID categoryId) {
        return menuRepository.findMenusByCategoryId(categoryId);
    }
}

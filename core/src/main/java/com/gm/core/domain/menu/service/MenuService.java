package com.gm.core.domain.menu.service;

import com.gm.core.domain.menu.model.Menu;
import com.gm.core.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * menu 테이블을 모두 조회
     */
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    /**
     * 카테고리ID를 기준으로 유저 선택메뉴 조회
     */
    public List<Menu> findMenusByCategoryId(UUID categoryId) {
        return menuRepository.findMenusByCategoryId(categoryId);
    }
}

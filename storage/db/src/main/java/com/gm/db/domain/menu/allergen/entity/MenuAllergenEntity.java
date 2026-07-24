package com.gm.db.domain.menu.allergen.entity;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 메뉴-알레르기 매핑을 {@code menu_allergen} 테이블에 매핑한다.
 *
 * <p>식약처 22종 표준 알레르기만 담기며, 결정론적 알레르기 제외 필터의 유일한 성분 데이터다.
 */
@Entity
@Table(
        name = "menu_allergen",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_menu_allergen",
                columnNames = {"menu_id", "allergen_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuAllergenEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID menuId;

    @Column(nullable = false)
    private UUID allergenId;

    public MenuAllergenEntity(UUID menuId, UUID allergenId) {
        this.menuId = menuId;
        this.allergenId = allergenId;
    }
}

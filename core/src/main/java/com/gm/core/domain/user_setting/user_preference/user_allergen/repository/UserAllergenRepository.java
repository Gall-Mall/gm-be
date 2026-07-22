package com.gm.core.domain.user_setting.user_preference.user_allergen.repository;

import com.gm.core.domain.user_setting.user_preference.user_allergen.model.UserAllergen;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserAllergenRepository {

    void deleteByUserId(UUID userId);

    void addUserAllergens(UUID userId, List<UUID> allergenIds);

    List<UserAllergen> findByUserId(UUID userId);
}

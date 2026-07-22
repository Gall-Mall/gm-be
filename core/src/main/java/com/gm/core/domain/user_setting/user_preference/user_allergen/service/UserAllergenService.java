package com.gm.core.domain.user_setting.user_preference.user_allergen.service;

import com.gm.core.domain.user_setting.user_preference.user_allergen.model.UserAllergen;
import com.gm.core.domain.user_setting.user_preference.user_allergen.repository.UserAllergenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAllergenService {
    /**
     * 유저가 선택한 알러지 조회
     * 유저가 선택한 알러지 삭제
     * 입력받은 알러지를 저장
     */
    private final UserAllergenRepository userAllergenRepository;

    public List<UserAllergen> getUserAllergens(UUID userId) {
        return userAllergenRepository.findByUserId(userId);
    }

    public void deleteUserAllergens(UUID userId) {
        userAllergenRepository.deleteByUserId(userId);
    }

    public void saveUserAllergens(UUID userId, List<UUID> allergenIds) {
        userAllergenRepository.addUserAllergens(userId, allergenIds);
    }
}

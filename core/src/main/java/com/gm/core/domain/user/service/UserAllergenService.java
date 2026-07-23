package com.gm.core.domain.user.service;

import com.gm.core.domain.user.model.UserAllergen;
import com.gm.core.domain.user.repository.UserAllergenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAllergenService {

    private final UserAllergenRepository userAllergenRepository;

    /**
     * 유저가 선택한 알러지 조회
     */
    public List<UserAllergen> getUserAllergens(UUID userId) {
        return userAllergenRepository.findByUserId(userId);
    }

    /**
     * 유저가 선택한 알러지 삭제
     */
    public void deleteUserAllergens(UUID userId) {
        userAllergenRepository.deleteByUserId(userId);
    }

    /**
     * 입력받은 알러지를 저장
     */
    public void saveUserAllergens(UUID userId, List<UUID> allergenIds) {
        userAllergenRepository.addUserAllergens(userId, allergenIds);
    }
}

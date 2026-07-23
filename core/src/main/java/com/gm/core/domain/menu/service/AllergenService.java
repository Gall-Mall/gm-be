package com.gm.core.domain.menu.service;

import com.gm.core.domain.menu.model.Allergen;
import com.gm.core.domain.menu.repository.AllergenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllergenService {

    private final AllergenRepository allergenRepository;

    public List<Allergen> findAll() {
        return allergenRepository.findAll();
    }
}

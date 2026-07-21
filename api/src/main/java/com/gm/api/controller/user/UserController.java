package com.gm.api.controller.user;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.user.dto.response.UserResponse;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

}
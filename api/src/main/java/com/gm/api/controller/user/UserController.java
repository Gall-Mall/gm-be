package com.gm.api.controller.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.user.dto.response.UserResponse;
import com.gm.api.security.CustomUserPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEnvelope<UserResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(UserResponse.from(principal.getUser()));
    }
}
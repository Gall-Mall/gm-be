package com.gm.core.domain.user.model;

import java.util.UUID;

public record UserResult(UUID userId, User user) {}
package com.gm.redis.domain.invite;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.invite.repository.InviteRepository;

/**
 * {@link InviteRepository}의 Redis 구현이다. 초대 코드는 {@code invite:{code}} 키로
 * groupId를 값으로 저장하며, TTL이 지나면 자동으로 사라진다.
 */
@Repository
@RequiredArgsConstructor
public class RedisInviteRepository implements InviteRepository {

    private static final String KEY_PREFIX = "invite:";

    private final StringRedisTemplate redisTemplate;

    /**
     * SETNX(존재하지 않을 때만 저장)로 원자적으로 저장한다. 두 요청이 동시에 같은 코드를
     * 생성하더라도 하나만 성공하도록 보장한다.
     */
    @Override
    public boolean save(String inviteCode, UUID groupId, Duration ttl) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + inviteCode, groupId.toString(), ttl);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public Optional<UUID> findGroupIdByCode(String inviteCode) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + inviteCode))
                .map(UUID::fromString);
    }
}

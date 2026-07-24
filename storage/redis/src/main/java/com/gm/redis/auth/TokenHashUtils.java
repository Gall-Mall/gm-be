package com.gm.redis.auth;

import java.util.HexFormat;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 토큰을 SHA-256 방식으로 해시하고 비교하는 유틸리티 클래스다.
 * <p>Refresh Token 원문을 Redis에 저장하지 않고 해시값만 저장하여, Redis 데이터가 노출되더라도 원본 토큰이 직접 노출되는 위험을 줄인다.</p>
 */
public final class TokenHashUtils {

    private static final String HASH_ALGORITHM = "SHA-256";

    private TokenHashUtils() { throw new IllegalStateException("유틸리티 클래스는 인스턴스화할 수 없습니다."); }

    /**
     * 전달받은 토큰을 SHA-256 해시 문자열로 변환한다.
     *
     * @param token 해시할 토큰 원문
     * @return 소문자 16진수 형태의 SHA-256 해시값
     * @throws IllegalArgumentException 토큰이 null이거나 공백이면 발생
     * @throws IllegalStateException    SHA-256 알고리즘을 사용할 수 없으면 발생
     */
    public static String hash(String token) {
        validateToken(token);

        MessageDigest messageDigest = createMessageDigest();
        byte[] hashBytes = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hashBytes);
    }

    /**
     * 원문 토큰과 저장된 해시값이 일치하는지 확인한다.
     * <p>{@link MessageDigest#isEqual(byte[], byte[])}을 사용하여 단순 문자열 비교보다 타이밍 공격에 덜 민감하게 비교한다.</p>
     *
     * @param rawToken     비교할 토큰 원문
     * @param expectedHash 저장소에 보관된 SHA-256 해시값
     * @return 두 값이 일치하면 {@code true}
     */
    public static boolean matches(String rawToken, String expectedHash) {
        if (rawToken == null || rawToken.isBlank() || expectedHash == null || expectedHash.isBlank()) {
            return false;
        }

        String actualHash = hash(rawToken);

        return MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private static void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("해시할 토큰은 null이거나 공백일 수 없습니다.");
        }
    }
}
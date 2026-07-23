package com.gm.core.domain.invite.service;

import java.security.SecureRandom;

/**
 * 초대 코드로 사용할 6자리 Base62(0-9, a-z, A-Z) 랜덤 문자열을 생성한다.
 */
final class InviteCodeGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodeGenerator() {
    }

    /**
     * 6자리 Base62 랜덤 초대 코드를 생성한다. 각 자리는 SecureRandom으로 독립적으로 추출한다.
     *
     * @return 생성된 초대 코드
     */
    static String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}

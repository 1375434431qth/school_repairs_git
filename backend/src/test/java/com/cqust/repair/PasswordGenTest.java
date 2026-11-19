package com.cqust.repair;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成初始化账号的 BCrypt 密文（密码统一 123456）。
 * 运行：mvn -q -Dtest=PasswordGenTest test
 */
class PasswordGenTest {

    @Test
    void generate() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("123456");
        System.out.println("BCRYPT_HASH=" + hash);
        System.out.println("MATCHES=" + encoder.matches("123456", hash));
    }
}

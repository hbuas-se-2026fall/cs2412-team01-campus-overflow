package com.campusoverflow.identity.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class FieldEncryptorTest {

    /** 32 字节固定测试密钥（仅用于单测，生产密钥来自环境变量 CO_FIELD_KEY）。 */
    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final FieldEncryptor encryptor = new FieldEncryptor(KEY);

    @Test
    void encryptThenDecryptReturnsOriginalText() {
        String plain = "student-2023001217@campus.edu";

        String cipher = encryptor.encrypt(plain);

        assertThat(cipher).isNotEqualTo(plain);
        assertThat(encryptor.decrypt(cipher)).isEqualTo(plain);
    }

    @Test
    void supportsChineseAndEmoji() {
        String plain = "张博 / 学生 · 😀";

        assertThat(encryptor.decrypt(encryptor.encrypt(plain))).isEqualTo(plain);
    }

    @Test
    void sameInputProducesDifferentCiphertexts() {
        String plain = "13800000000";

        String first = encryptor.encrypt(plain);
        String second = encryptor.encrypt(plain);

        // 随机 IV：同一明文两次加密必须不同，否则可被用于相等性推断。
        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo(plain);
        assertThat(encryptor.decrypt(second)).isEqualTo(plain);
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] raw = Base64.getDecoder().decode(encryptor.encrypt("sensitive"));
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void ciphertextFromAnotherKeyIsRejected() {
        String otherKey = Base64.getEncoder()
                .encodeToString("fedcba9876543210fedcba9876543210".getBytes());
        String cipher = new FieldEncryptor(otherKey).encrypt("secret");

        assertThatThrownBy(() -> encryptor.decrypt(cipher))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsKeyOfWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new FieldEncryptor(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 字节");
    }

    @Test
    void digestIsDeterministicAndCaseInsensitive() {
        String lower = encryptor.digest("alice@campus.edu");
        String upper = encryptor.digest("ALICE@campus.edu");

        assertThat(lower).isEqualTo(upper);
        assertThat(lower).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(encryptor.digest("bob@campus.edu")).isNotEqualTo(lower);
    }

    @Test
    void digestIsStableAcrossInstancesWithSameKey() {
        assertThat(new FieldEncryptor(KEY).digest("2023001217"))
                .isEqualTo(encryptor.digest("2023001217"));
    }
}

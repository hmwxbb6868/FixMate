package com.fixmate.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** SHA-256预处理避免BCrypt的72字节限制；存储前缀区分原始BCrypt与新格式。 */
public final class StaffPasswordEncoder implements PasswordEncoder {
    private static final String PREFIX = "{bcrypt-sha256}";
    private final BCryptPasswordEncoder bcrypt;

    public StaffPasswordEncoder() { this(12); }
    public StaffPasswordEncoder(int strength) { bcrypt = new BCryptPasswordEncoder(strength); }

    @Override public String encode(CharSequence value) { return PREFIX + bcrypt.encode(prehash(value)); }
    @Override public boolean matches(CharSequence value, String encoded) {
        if (value == null || encoded == null) return false;
        return encoded.startsWith(PREFIX)
                ? bcrypt.matches(prehash(value), encoded.substring(PREFIX.length()))
                : value.toString().getBytes(StandardCharsets.UTF_8).length <= 72 && bcrypt.matches(value, encoded);
    }
    private String prehash(CharSequence value) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}

package com.magiccode.backend.service;

import com.magiccode.backend.model.JwtSecret;
import com.magiccode.backend.repository.JwtSecretRepository;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class JwtKeyStore {

    private static final long SINGLETON_ID = 1L;

    private final JwtSecretRepository jwtSecretRepository;

    // 缓存起来，避免每次请求都查 DB
    private volatile Key signingKey;

    public Key getSigningKey() {
        Key local = signingKey;
        if (local != null) {
            return local;
        }

        synchronized (this) {
            if (signingKey == null) {
                String base64 = loadOrCreateBase64Secret();
                byte[] keyBytes = Decoders.BASE64.decode(base64);

                // HS256 建议至少 256-bit(32 bytes)
                if (keyBytes.length < 32) {
                    throw new IllegalStateException(
                            "jwt_secret.secret_base64 长度不足，HS256 至少需要 256-bit (32 bytes) 密钥"
                    );
                }

                signingKey = Keys.hmacShaKeyFor(keyBytes);
            }
            return signingKey;
        }
    }

    private String loadOrCreateBase64Secret() {
        return jwtSecretRepository.findById(SINGLETON_ID)
                .map(JwtSecret::getSecretBase64)
                .orElseGet(() -> {
                    String generated = generateBase64Secret();

                    try {
                        jwtSecretRepository.save(
                                JwtSecret.builder()
                                        .id(SINGLETON_ID)
                                        .secretBase64(generated)
                                        .updatedAt(Instant.now())
                                        .build()
                        );
                        return generated;
                    } catch (DataIntegrityViolationException ex) {
                        // 多实例并发启动时，可能先被其他实例插入了；再读一次
                        return jwtSecretRepository.findById(SINGLETON_ID)
                                .map(JwtSecret::getSecretBase64)
                                .orElse(generated);
                    }
                });
    }

    private static String generateBase64Secret() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            keyGenerator.init(256); // 256-bit
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法生成 HmacSHA256 密钥", e);
        }
    }
}


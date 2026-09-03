package com.application.taskmanager.notification.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Slf4j
public class VapidJwtHelper {

    private static final byte[] PKCS8_P256_HEADER = new byte[] {
            (byte) 0x30, (byte) 0x81, (byte) 0x87, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x30, (byte) 0x13,
            (byte) 0x06, (byte) 0x07, (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0xce, (byte) 0x3d, (byte) 0x02,
            (byte) 0x01, (byte) 0x06, (byte) 0x08, (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0xce, (byte) 0x3d,
            (byte) 0x03, (byte) 0x01, (byte) 0x07, (byte) 0x04, (byte) 0x6d, (byte) 0x30, (byte) 0x6b, (byte) 0x02,
            (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x20
    };

    private static final byte[] PKCS8_P256_FOOTER_PREFIX = new byte[] {
            (byte) 0xa1, (byte) 0x44, (byte) 0x03, (byte) 0x42, (byte) 0x04
    };

    /**
     * Convert Base64Url uncompressed public key (65 bytes) and private key (32 bytes) into Java PrivateKey
     */
    public static PrivateKey getPrivateKey(String vapidPublicKeyBase64Url, String vapidPrivateKeyBase64Url) throws Exception {
        byte[] privBytes = Base64.getUrlDecoder().decode(vapidPrivateKeyBase64Url);
        byte[] pubBytes = Base64.getUrlDecoder().decode(vapidPublicKeyBase64Url);

        byte[] pkcs8 = new byte[PKCS8_P256_HEADER.length + privBytes.length + PKCS8_P256_FOOTER_PREFIX.length + pubBytes.length];
        System.arraycopy(PKCS8_P256_HEADER, 0, pkcs8, 0, PKCS8_P256_HEADER.length);
        System.arraycopy(privBytes, 0, pkcs8, PKCS8_P256_HEADER.length, privBytes.length);
        System.arraycopy(PKCS8_P256_FOOTER_PREFIX, 0, pkcs8, PKCS8_P256_HEADER.length + privBytes.length, PKCS8_P256_FOOTER_PREFIX.length);
        System.arraycopy(pubBytes, 0, pkcs8, PKCS8_P256_HEADER.length + privBytes.length + PKCS8_P256_FOOTER_PREFIX.length, pubBytes.length);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }

    /**
     * Generate VAPID JWT Token for push endpoint origin (RFC 8292)
     */
    public static String createVapidToken(String endpoint, String subject, PrivateKey privateKey) {
        URI uri = URI.create(endpoint);
        String origin = uri.getScheme() + "://" + uri.getAuthority();

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + (12 * 3600 * 1000)); // 12 hours

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "ES256")
                .setAudience(origin)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }
}

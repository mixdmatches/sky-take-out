package com.sky.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    /**
     * 生成jwt
     * 使用Hs256算法, 私匙使用固定秘钥
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 生成JWT的时间
        Date exp = new Date(System.currentTimeMillis() + ttlMillis);

        return Jwts.builder()
                // 设置jwt的body
                .claims(claims)
                // 设置过期时间
                .expiration(exp)
                // 设置签名使用的签名算法和签名使用的秘钥
                .signWith(buildKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Token解密
     *
     * @param secretKey jwt秘钥 此秘钥一定要保留好在服务端, 不能暴露出去, 否则sign就可以被伪造, 如果对接多个客户端建议改造成多个
     * @param token     加密后的token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser()
                // 设置签名的秘钥
                .verifyWith(buildKey(secretKey))
                .build()
                // 设置需要解析的jwt
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * HS256要求秘钥长度不小于256位，这里对配置的秘钥做SHA-256摘要生成满足要求的秘钥
     */
    private static SecretKey buildKey(String secretKey) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return Keys.hmacShaKeyFor(messageDigest.digest(secretKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前JDK不支持SHA-256算法", e);
        }
    }

}
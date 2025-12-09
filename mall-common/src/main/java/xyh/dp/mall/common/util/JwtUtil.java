package xyh.dp.mall.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
public class JwtUtil {

    /**
     * 生成JWT Token
     * 
     * @param subject 主题(通常是用户ID)
     * @param claims 自定义数据
     * @param secret 密钥
     * @param expiration 过期时间(毫秒)
     * @return JWT Token
     */
    public static String generateToken(String subject, Map<String, Object> claims, String secret, long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT Token
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return Claims
     * @throws io.jsonwebtoken.JwtException JWT解析异常
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取Token中的用户ID
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return 用户ID
     */
    public static String getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.getSubject();
    }

    /**
     * 验证Token是否过期
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return true-已过期, false-未过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}

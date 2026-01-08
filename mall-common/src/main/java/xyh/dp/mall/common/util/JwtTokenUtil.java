package xyh.dp.mall.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT双Token工具类
 * 支持Access Token + Refresh Token + JTI机制
 * 
 * <p>功能特性:
 * - Access Token: 短期有效(15分钟)，用于API访问
 * - Refresh Token: 长期有效(7天)，用于刷新Access Token
 * - JTI: JWT唯一标识符，用于token撤销和黑名单管理
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
public class JwtTokenUtil {

    /**
     * HS256算法要求的最小密钥长度（字节）
     */
    private static final int MIN_SECRET_KEY_LENGTH = 32;
    
    /**
     * Token类型：Access Token
     */
    public static final String TOKEN_TYPE_ACCESS = "access";
    
    /**
     * Token类型：Refresh Token
     */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * 生成Access Token
     * 
     * @param subject 主题(通常是用户ID)
     * @param claims 自定义数据
     * @param secret 密钥
     * @param expiration 过期时间(毫秒)
     * @return Access Token对象
     */
    public static TokenPair generateAccessToken(String subject, Map<String, Object> claims, 
                                                  String secret, long expiration) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        
        // 添加token类型
        claims.put("token_type", TOKEN_TYPE_ACCESS);
        
        String token = generateToken(subject, claims, secret, expiration, jti);
        
        return TokenPair.builder()
                .token(token)
                .jti(jti)
                .expiresIn(expiration / 1000)
                .build();
    }

    /**
     * 生成Refresh Token
     * 
     * @param subject 主题(通常是用户ID)
     * @param claims 自定义数据
     * @param secret 密钥
     * @param expiration 过期时间(毫秒)
     * @return Refresh Token对象
     */
    public static TokenPair generateRefreshToken(String subject, Map<String, Object> claims, 
                                                   String secret, long expiration) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        
        // 添加token类型
        claims.put("token_type", TOKEN_TYPE_REFRESH);
        
        String token = generateToken(subject, claims, secret, expiration, jti);
        
        return TokenPair.builder()
                .token(token)
                .jti(jti)
                .expiresIn(expiration / 1000)
                .build();
    }

    /**
     * 生成JWT Token（内部方法）
     * 
     * @param subject 主题
     * @param claims 自定义数据
     * @param secret 密钥
     * @param expiration 过期时间(毫秒)
     * @param jti JWT ID
     * @return JWT Token字符串
     */
    private static String generateToken(String subject, Map<String, Object> claims, 
                                         String secret, long expiration, String jti) {
        SecretKey key = getSecretKey(secret);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .id(jti)  // 设置JTI
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
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
        SecretKey key = getSecretKey(secret);
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
     * 获取Token中的JTI
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return JTI
     */
    public static String getJti(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.getId();
    }
    
    /**
     * 获取Token类型
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return Token类型(access/refresh)
     */
    public static String getTokenType(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get("token_type", String.class);
    }

    /**
     * 验证Token是否过期
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return true-已过期, false-未过期
     * @throws io.jsonwebtoken.JwtException JWT解析异常，调用方需处理
     */
    public static boolean isTokenExpired(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.getExpiration().before(new Date());
    }
    
    /**
     * 验证是否为Access Token
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return true-是Access Token, false-否
     * @throws io.jsonwebtoken.JwtException JWT解析异常，调用方需处理
     */
    public static boolean isAccessToken(String token, String secret) {
        String tokenType = getTokenType(token, secret);
        return TOKEN_TYPE_ACCESS.equals(tokenType);
    }
    
    /**
     * 验证是否为Refresh Token
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return true-是Refresh Token, false-否
     * @throws io.jsonwebtoken.JwtException JWT解析异常，调用方需处理
     */
    public static boolean isRefreshToken(String token, String secret) {
        String tokenType = getTokenType(token, secret);
        return TOKEN_TYPE_REFRESH.equals(tokenType);
    }

    /**
     * 获取密钥对象
     * 如果密钥长度不足，自动进行填充
     *
     * @param secret 原始密钥字符串
     * @return SecretKey对象
     */
    private static SecretKey getSecretKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        
        // 如果密钥长度不足，进行填充
        if (keyBytes.length < MIN_SECRET_KEY_LENGTH) {
            byte[] paddedKey = new byte[MIN_SECRET_KEY_LENGTH];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            // 用固定字符填充剩余位置
            for (int i = keyBytes.length; i < MIN_SECRET_KEY_LENGTH; i++) {
                paddedKey[i] = (byte) (i % 256);
            }
            keyBytes = paddedKey;
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Token对象
     * 包含token字符串、JTI和过期时间
     */
    public static class TokenPair {
        private String token;
        private String jti;
        private Long expiresIn;
        
        private TokenPair(String token, String jti, Long expiresIn) {
            this.token = token;
            this.jti = jti;
            this.expiresIn = expiresIn;
        }
        
        public static TokenPairBuilder builder() {
            return new TokenPairBuilder();
        }
        
        public String getToken() {
            return token;
        }
        
        public String getJti() {
            return jti;
        }
        
        public Long getExpiresIn() {
            return expiresIn;
        }
        
        public static class TokenPairBuilder {
            private String token;
            private String jti;
            private Long expiresIn;
            
            public TokenPairBuilder token(String token) {
                this.token = token;
                return this;
            }
            
            public TokenPairBuilder jti(String jti) {
                this.jti = jti;
                return this;
            }
            
            public TokenPairBuilder expiresIn(Long expiresIn) {
                this.expiresIn = expiresIn;
                return this;
            }
            
            public TokenPair build() {
                return new TokenPair(token, jti, expiresIn);
            }
        }
    }
}

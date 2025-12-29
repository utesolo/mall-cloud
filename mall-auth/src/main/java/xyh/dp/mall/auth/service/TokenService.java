package xyh.dp.mall.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import xyh.dp.mall.auth.config.JwtProperties;
import xyh.dp.mall.auth.entity.User;
import xyh.dp.mall.common.exception.BusinessException;
import xyh.dp.mall.common.util.JwtTokenUtil;
import xyh.dp.mall.common.util.JwtTokenUtil.TokenPair;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token管理服务
 * 负责Access Token和Refresh Token的生成、验证、刷新和撤销
 * 
 * <p>功能特性：
 * - 双Token机制：Access Token(15分钟) + Refresh Token(7天)
 * - JTI机制：每个token都有唯一标识符
 * - Token黑名单：支持强制登出和token撤销
 * - Redis缓存：token与JTI映射关系
 * </p>
 * 
 * @author mall-cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;
    
    // Redis Key前缀
    private static final String ACCESS_TOKEN_PREFIX = "auth:access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private static final String JTI_BLACKLIST_PREFIX = "auth:jti_blacklist:";
    private static final String USER_TOKEN_MAPPING_PREFIX = "auth:user_tokens:";

    /**
     * 生成双Token
     * 
     * @param user 用户对象
     * @return Token信息Map(accessToken, refreshToken, expiresIn等)
     */
    public Map<String, Object> generateTokens(User user) {
        // 构建Claims
        Map<String, Object> claims = buildClaims(user);
        
        // 生成Access Token
        TokenPair accessTokenPair = JwtTokenUtil.generateAccessToken(
                user.getId().toString(),
                new HashMap<>(claims),
                jwtProperties.getSecret(),
                jwtProperties.getAccessTokenExpiration()
        );
        
        // 生成Refresh Token
        TokenPair refreshTokenPair = JwtTokenUtil.generateRefreshToken(
                user.getId().toString(),
                new HashMap<>(claims),
                jwtProperties.getSecret(),
                jwtProperties.getRefreshTokenExpiration()
        );
        
        // 缓存Token信息到Redis
        cacheAccessToken(user.getId(), accessTokenPair);
        cacheRefreshToken(user.getId(), refreshTokenPair);
        
        // 保存用户的token映射关系
        saveUserTokenMapping(user.getId(), accessTokenPair.getJti(), refreshTokenPair.getJti());
        
        log.info("生成双Token成功, userId: {}, accessJti: {}, refreshJti: {}", 
                user.getId(), accessTokenPair.getJti(), refreshTokenPair.getJti());
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessTokenPair.getToken());
        result.put("refreshToken", refreshTokenPair.getToken());
        result.put("expiresIn", accessTokenPair.getExpiresIn());
        result.put("accessJti", accessTokenPair.getJti());
        result.put("refreshJti", refreshTokenPair.getJti());
        
        return result;
    }

    /**
     * 刷新Access Token
     * 使用Refresh Token换取新的Access Token
     * 
     * @param refreshToken Refresh Token
     * @return 新的Access Token信息
     * @throws BusinessException Token无效或已过期
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        try {
            // 1. 验证Refresh Token的有效性
            if (!JwtTokenUtil.isRefreshToken(refreshToken, jwtProperties.getSecret())) {
                throw new BusinessException("无效的刷新令牌");
            }
            
            // 2. 解析Token获取用户信息
            String userId = JwtTokenUtil.getUserId(refreshToken, jwtProperties.getSecret());
            String refreshJti = JwtTokenUtil.getJti(refreshToken, jwtProperties.getSecret());
            
            // 3. 检查JTI是否在黑名单中
            if (isJtiBlacklisted(refreshJti)) {
                throw new BusinessException("刷新令牌已失效，请重新登录");
            }
            
            // 4. 检查Refresh Token是否过期
            if (JwtTokenUtil.isTokenExpired(refreshToken, jwtProperties.getSecret())) {
                throw new BusinessException("刷新令牌已过期，请重新登录");
            }
            
            // 5. 验证Refresh Token是否存在于Redis
            String cachedRefreshToken = redisTemplate.opsForValue()
                    .get(REFRESH_TOKEN_PREFIX + userId);
            if (cachedRefreshToken == null || !cachedRefreshToken.contains(refreshJti)) {
                throw new BusinessException("刷新令牌不存在，请重新登录");
            }
            
            // 6. 生成新的Access Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", Long.parseLong(userId));
            // 从旧token中获取其他信息
            var oldClaims = JwtTokenUtil.parseToken(refreshToken, jwtProperties.getSecret());
            claims.put("userType", oldClaims.get("userType"));
            
            TokenPair newAccessTokenPair = JwtTokenUtil.generateAccessToken(
                    userId,
                    claims,
                    jwtProperties.getSecret(),
                    jwtProperties.getAccessTokenExpiration()
            );
            
            // 7. 缓存新的Access Token
            cacheAccessToken(Long.parseLong(userId), newAccessTokenPair);
            
            log.info("刷新Access Token成功, userId: {}, newAccessJti: {}", userId, newAccessTokenPair.getJti());
            
            // 8. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessTokenPair.getToken());
            result.put("expiresIn", newAccessTokenPair.getExpiresIn());
            result.put("accessJti", newAccessTokenPair.getJti());
            
            return result;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新Access Token失败", e);
            throw new BusinessException("刷新令牌失败，请重新登录");
        }
    }

    /**
     * 撤销用户的所有Token
     * 用于强制登出
     * 
     * @param userId 用户ID
     */
    public void revokeAllTokens(Long userId) {
        try {
            // 1. 获取用户的所有JTI
            String userTokenKey = USER_TOKEN_MAPPING_PREFIX + userId;
            Map<Object, Object> jtiMap = redisTemplate.opsForHash().entries(userTokenKey);
            
            // 2. 将所有JTI加入黑名单
            for (Object jtiObj : jtiMap.values()) {
                String jti = (String) jtiObj;
                addJtiToBlacklist(jti, jwtProperties.getRefreshTokenExpiration());
            }
            
            // 3. 删除用户的token缓存
            redisTemplate.delete(ACCESS_TOKEN_PREFIX + userId);
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
            redisTemplate.delete(userTokenKey);
            
            log.info("撤销用户所有Token成功, userId: {}, jtiCount: {}", userId, jtiMap.size());
            
        } catch (Exception e) {
            log.error("撤销Token失败, userId: {}", userId, e);
        }
    }

    /**
     * 验证Access Token
     * 
     * @param accessToken Access Token
     * @return true-有效, false-无效
     */
    public boolean validateAccessToken(String accessToken) {
        try {
            // 1. 验证是否为Access Token
            if (!JwtTokenUtil.isAccessToken(accessToken, jwtProperties.getSecret())) {
                return false;
            }
            
            // 2. 验证是否过期
            if (JwtTokenUtil.isTokenExpired(accessToken, jwtProperties.getSecret())) {
                return false;
            }
            
            // 3. 检查JTI是否在黑名单中
            String jti = JwtTokenUtil.getJti(accessToken, jwtProperties.getSecret());
            if (isJtiBlacklisted(jti)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("验证Access Token失败", e);
            return false;
        }
    }

    /**
     * 构建JWT Claims
     * 
     * @param user 用户对象
     * @return Claims Map
     */
    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("userType", user.getUserType());
        return claims;
    }

    /**
     * 缓存Access Token到Redis
     * 
     * @param userId 用户ID
     * @param tokenPair Token对象
     */
    private void cacheAccessToken(Long userId, TokenPair tokenPair) {
        String key = ACCESS_TOKEN_PREFIX + userId;
        // 存储: userId -> jti
        redisTemplate.opsForValue().set(
                key,
                tokenPair.getJti(),
                jwtProperties.getAccessTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 缓存Refresh Token到Redis
     * 
     * @param userId 用户ID
     * @param tokenPair Token对象
     */
    private void cacheRefreshToken(Long userId, TokenPair tokenPair) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        // 存储: userId -> jti
        redisTemplate.opsForValue().set(
                key,
                tokenPair.getJti(),
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 保存用户token映射关系
     * 
     * @param userId 用户ID
     * @param accessJti Access Token的JTI
     * @param refreshJti Refresh Token的JTI
     */
    private void saveUserTokenMapping(Long userId, String accessJti, String refreshJti) {
        String key = USER_TOKEN_MAPPING_PREFIX + userId;
        redisTemplate.opsForHash().put(key, "access_jti", accessJti);
        redisTemplate.opsForHash().put(key, "refresh_jti", refreshJti);
        // 设置过期时间为Refresh Token的过期时间
        redisTemplate.expire(key, jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);
    }

    /**
     * 将JTI加入黑名单
     * 
     * @param jti JWT ID
     * @param expiration 过期时间(毫秒)
     */
    private void addJtiToBlacklist(String jti, long expiration) {
        String key = JTI_BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(
                key,
                "1",
                expiration,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 检查JTI是否在黑名单中
     * 
     * @param jti JWT ID
     * @return true-在黑名单中, false-不在
     */
    private boolean isJtiBlacklisted(String jti) {
        String key = JTI_BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

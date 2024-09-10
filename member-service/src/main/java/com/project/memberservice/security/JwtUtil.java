package com.project.memberservice.security;

import com.project.memberservice.entity.UserRoleEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@RequiredArgsConstructor
@Component
public class JwtUtil {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_KEY = "auth";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.access_expiration_time}")
    private long accessExpirationTime;

    @Value("${jwt.secret_key}") // Base64 Encode 한 SecretKey
    private String secretKey;

    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(secretKeyBytes);
    }

    /**
     * Access Token 생성
     *
     * @param username 해당 회원의 email(ID)
     * @param role 해당 회원의 UserRoleEnum 값
     * @param memberId
     * @return 생성한 Access Token
     */
    public String createToken(String username, UserRoleEnum role, String memberId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username) // 사용자 식별자값(email)
                .claim(AUTHORIZATION_KEY, String.valueOf(role))
                .claim("memberId", memberId)
                .setExpiration(new Date(now.getTime() + accessExpirationTime))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    /**
     * header 에서 Access Token 가져오기
     *
     * @param request
     * @return request header에 포함된 Access Token
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Access Token 검증
     *
     * @param token Access Token
     * @return 해당 토큰이 유효하면 true, 유효하지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("유효하지 않은 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("잘못된 JWT 토큰입니다.");
        }
        return false;
    }

    /**
     * Access Token의 정보 가져오기
     *
     * @param token Access Token
     * @return 해당 토큰에 포함된 정보(Claims)
     */
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * Access Token이 logout 블랙리스트에 있는지 확인하기
     *
     * @param accessToken 블랙리스트에서 확인할 Access Token
     * @return 블랙리스트에 존재하면 true, 없으면 false
     */
    public boolean isTokenInBlacklist(String accessToken) {
        // Redis에서 해당 accessToken에 저장된 값을 조회
        Object value = redisTemplate.opsForValue().get(accessToken);

        if(value != null) {
            // 저장된 값이 "logout"인지 확인
            return "logout".equals(value.toString());
        }

        return false;
    }
}
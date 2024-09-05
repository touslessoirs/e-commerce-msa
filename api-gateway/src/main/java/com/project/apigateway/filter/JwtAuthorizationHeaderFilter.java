package com.project.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthorizationHeaderFilter extends AbstractGatewayFilterFactory<JwtAuthorizationHeaderFilter.Config> {
    @Value("${jwt.secret_key}") // Base64 Encode 한 SecretKey
    private String secretKey;

    private Key key;
    private final RedisTemplate<String, Object> redisTemplate;

    public static class Config {
    }

    @PostConstruct
    public void init() {
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(secretKeyBytes);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer", "");

            if (!isJwtValid(jwt) || isTokenInBlacklist(jwt)) {
                return onError(exchange, "유효하지 않은 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            String memberId = claims.get("memberId", String.class);

            // 요청 헤더에 memberId 추가
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Member-Id", memberId)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);

        byte[] bytes = "유효하지 않은 JWT 토큰입니다.".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }

    private boolean isJwtValid(String jwt) {
        boolean returnValue = true;
        String subject = null;  //username(email)

        try {
            JwtParser jwtParser = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build();

            subject = jwtParser.parseClaimsJws(jwt).getBody().getSubject();
        } catch (SignatureException ex) {
            log.error("유효하지 않은 JWT 서명입니다: {}", ex.getMessage());
            returnValue = false;
        } catch (ExpiredJwtException ex) {
            log.error("JWT 토큰이 만료되었습니다: {}", ex.getMessage());
            returnValue = false;
        } catch (Exception ex) {
            log.error("JWT 토큰 파싱 중 에러가 발생했습니다: {}", ex.getMessage());
            returnValue = false;
        }

        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }

        return returnValue;
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인하는 메서드
     *
     * @param accessToken 블랙리스트에서 확인할 Access Token
     * @return 블랙리스트에 존재하면 true, 없으면 false
     */
    private boolean isTokenInBlacklist(String accessToken) {
        // Redis에서 해당 accessToken에 저장된 값을 조회
        Object value = redisTemplate.opsForValue().get(accessToken);

        if(value != null) {
            // 저장된 값이 "logout"인지 확인
            return "logout".equals(value.toString());
        }

        return false;
    }

}
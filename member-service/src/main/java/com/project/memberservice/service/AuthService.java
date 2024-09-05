package com.project.memberservice.service;

import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.RefreshToken;
import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.repository.RefreshTokenRepository;
import com.project.memberservice.security.JwtUtil;
import com.project.memberservice.util.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${jwt.secret_key}") // Base64 Encode 한 SecretKey
    private String secretKey;

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    /**
     * 로그아웃
     * 1. Access Token 블랙리스트 처리
     * 2. Redis에서 리프레시 토큰 삭제
     *
     * @param accessToken 블랙리스트 처리할 Access Token
     * @param refreshToken Redis에서 삭제할 Refresh Token
     */
    public void logout(String accessToken, String refreshToken) {
        try {
            // 1. Access Token 블랙리스트 처리
            long expiration = getExpiration(accessToken);
            redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);

            // 2. Redis에서 리프레시 토큰 삭제
            Optional<RefreshToken> storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken);
            if (!storedRefreshToken.isPresent()) {
                throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
            } else {
                refreshTokenRepository.delete(storedRefreshToken.get());
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.GENERAL_EXCEPTION);
        }
    }

    /**
     * Access Token의 만료 시간 조회
     * 
     * @param accessToken Access Token
     * @return 해당 Access Token의 만료 시간
     */
    private long getExpiration(String accessToken) {
        byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(secretKeyBytes);
        accessToken = accessToken.replace("Bearer", "");

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();

        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
    
    /**
     * Refresh Token을 기반으로 Access Token을 재발급한다.
     *
     * @param refreshToken (final로 값 변경 방지)
     * @return 재발급받은 Access Token, Refresh Token
     */
    @Transactional
    public String generateAccessToken(final String refreshToken) {
        // redis(refreshTokenRepository)에 해당 Refresh Token이 있는지 조회 (유효한지 확인)
        RefreshToken savedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        // Refresh Token에 해당하는 회원 정보 조회
        Member member = memberRepository.findById(savedRefreshToken.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Access Token 재발급
        String newAccessToken = jwtUtil.createToken(member.getEmail(), member.getRole(), String.valueOf(member.getMemberId()));

        return newAccessToken;
    }

    /**
     * 인증번호 검증
     * 해당 사용자(Redis key)에 대해 저장된(발급된) 인증번호(Redis value)가 입력한 인증번호와 일치하는지 검증한다.
     * 일치하면 is_verified 컬럼의 값을 1(true)로 변경한다.
     *
     * @param id memberId
     * @param verificationCode 사용자가 입력한 인증번호
     * @return 검증 결과에 대한 응답
     */
    public ResponseEntity checkVerificationCode(String id, String verificationCode) {
        Long memberId = Long.parseLong(id);
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String email = member.getEmail();
        String storedCode = redisUtil.getData(email);   //해당 email에 발급된 인증번호

        if (storedCode == null) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        } else if (storedCode.equals(verificationCode)) {
            member.setIsVerified(1);  //인증 완료
            memberRepository.save(member);

            return ResponseEntity.ok("인증이 완료되었습니다");

        } else {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
    }
}

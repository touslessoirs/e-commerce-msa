package com.project.memberservice.service;

import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.RefreshToken;
import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.repository.RefreshTokenRepository;
import com.project.memberservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository userRepository;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    /**
     * Refresh Token을 기반으로 Access Token 재발급
     *
     * @param refreshToken (final로 값 변경 방지)
     * @return 발급받은 Access Token
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
        return jwtUtil.createToken(member.getEmail(), member.getRole(), String.valueOf(member.getMemberId()));
    }
}

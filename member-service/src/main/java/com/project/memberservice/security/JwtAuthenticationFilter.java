package com.project.memberservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.memberservice.dto.LoginRequestDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.RefreshToken;
import com.project.memberservice.entity.UserRoleEnum;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.repository.RefreshTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
//@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String REFRESH_TOKEN_HEADER = "Refresh-Token";

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository, MemberRepository memberRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberRepository = memberRepository;
        setFilterProcessesUrl("/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
            throws AuthenticationException {
        log.info("login");

        try {
            LoginRequestDto creds = new ObjectMapper().readValue(req.getInputStream(), LoginRequestDto.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            creds.getEmail(),
                            creds.getPassword(),
                            null)
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        log.info("Authentication Success");

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Member member = userDetails.getMember();
        String username = userDetails.getUsername();
        UserRoleEnum role = member.getRole();

        //Access Token 발급
        String token = jwtUtil.createToken(username, role, String.valueOf(member.getMemberId()));   //Access Token

        //Refresh Token 발급 및 redis 저장
        String refreshToken = UUID.randomUUID().toString(); //Refresh Token
        RefreshToken redis = new RefreshToken(refreshToken, member.getMemberId());
        refreshTokenRepository.save(redis);

        //마지막 로그인 시간 저장
        member.setLastLoginTime(LocalDateTime.now());
        memberRepository.save(member);

        //응답 메시지 설정
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        res.addHeader(JwtUtil.AUTHORIZATION_HEADER, token);
        res.addHeader(REFRESH_TOKEN_HEADER, refreshToken);

        res.getWriter().write("{\"message\": \"로그인이 완료되었습니다.\"}");
    }


    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        log.error("Authentication failed: " + failed.getMessage());

        // 응답 상태와 메시지 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 실패 응답 메시지
        response.getWriter().write("{\"message\": \"로그인에 실패하였습니다. 아이디 또는 비밀번호를 확인해 주세요.\"}");
    }

}

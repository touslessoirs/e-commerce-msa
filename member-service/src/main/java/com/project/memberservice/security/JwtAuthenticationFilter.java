package com.project.memberservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.memberservice.dto.LoginRequestDto;
import com.project.memberservice.entity.RefreshToken;
import com.project.memberservice.entity.UserRoleEnum;
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
import java.util.UUID;

@Slf4j
@Component
//@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String REFRESH_TOKEN_HEADER = "Refresh-Token";

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
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

        String username = ((UserDetailsImpl) auth.getPrincipal()).getUsername();
        UserRoleEnum role = ((UserDetailsImpl) auth.getPrincipal()).getMember().getRole();
        String memberId = String.valueOf(((UserDetailsImpl) auth.getPrincipal()).getMemberId());

        //Access Token 발급
        String token = jwtUtil.createToken(username, role, memberId);   //Access Token

        //Refresh Token 발급 및 redis 저장
        String refreshToken = UUID.randomUUID().toString(); //Refresh Token
        RefreshToken redis = new RefreshToken(refreshToken, ((UserDetailsImpl) auth.getPrincipal()).getMemberId());
        refreshTokenRepository.save(redis);

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

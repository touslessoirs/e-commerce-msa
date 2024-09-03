package com.project.memberservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.memberservice.dto.LoginRequestDto;
import com.project.memberservice.entity.UserRoleEnum;
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

@Slf4j
@Component
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
            throws AuthenticationException {
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
        String username = ((UserDetailsImpl) auth.getPrincipal()).getUsername();
        UserRoleEnum role = ((UserDetailsImpl) auth.getPrincipal()).getMember().getRole();
        String memberId = String.valueOf(((UserDetailsImpl) auth.getPrincipal()).getMemberId());

        String token = jwtUtil.createToken(username, role, memberId);

        //응답 메시지 설정
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"message\": \"Login successful\"}");

        res.addHeader(JwtUtil.AUTHORIZATION_HEADER, token);
    }

}

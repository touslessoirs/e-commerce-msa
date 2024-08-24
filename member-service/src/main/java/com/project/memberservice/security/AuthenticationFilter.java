package com.project.memberservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.memberservice.entity.UserRoleEnum;
import com.project.memberservice.dto.LoginRequestDto;
import com.project.memberservice.dto.UserInfoDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    //사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "auth";
    @Value("${jwt.token.secret_key}")
    private String secretKey;
//    @Value("${jwt.token.expiration_time}")
//    private Long expirationTime;

    private final UserDetailsServiceImpl userDetailsService;
    private final Environment environment;

//    @PostConstruct
//    public void init() {
//        byte[] bytes = Base64.getDecoder().decode(secretKey);
//        key = Keys.hmacShaKeyFor(bytes);
//        log.info("Expiration Time from properties: " + expirationTime);
//    }

    public AuthenticationFilter(AuthenticationManager authenticationManager,
                                UserDetailsServiceImpl userDetailsService, Environment environment) {
        super(authenticationManager);
        this.userDetailsService = userDetailsService;
        this.environment = environment;
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
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        String username = ((UserDetailsImpl)auth.getPrincipal()).getMember().getEmail();
        //username에 해당하는 memberId, role 조회
        UserInfoDto userInfo = userDetailsService.getMemberDetailsByEmail(username);
        String memberId = String.valueOf(userInfo.getMemberId());
        UserRoleEnum role = userInfo.getRole();

        byte[] secretKeyBytes = Base64.getEncoder().encode(environment.getProperty("jwt.token.secret_key").getBytes());
        SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);

        Instant now = Instant.now();

        String token = Jwts.builder()
                .setSubject(memberId)
                .claim(AUTHORIZATION_KEY, role) //사용자 권한
                .setExpiration(Date.from(now.plusMillis(Long.parseLong(environment.getProperty("jwt.token.expiration_time")))))
                .setIssuedAt(Date.from(now))
                .signWith(secretKey)
                .compact();

        //응답 메시지 설정
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"message\": \"Login successful\"}");

        res.addHeader("Authorization", token);
        res.addHeader("memberId", memberId);
    }
}

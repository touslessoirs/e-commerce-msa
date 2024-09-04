package com.project.memberservice.controller;

import com.project.memberservice.exception.CustomException;
import com.project.memberservice.security.JwtUtil;
import com.project.memberservice.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/")
public class TokenController {

    private final TokenService tokenService;

    /* Refresh Token을 기반으로 Access Token 재발급 */
    @PostMapping("/reissueToken")
    public ResponseEntity getToken(@RequestBody String refreshToken) {
        try {
            // Refresh Token을 기반으로 Access Token 재발급
            String newAccessToken = tokenService.generateAccessToken(refreshToken);

            // 새로운 Access Token -> header에 추가
            HttpHeaders headers = new HttpHeaders();
            headers.add(JwtUtil.AUTHORIZATION_HEADER, newAccessToken);

            return ResponseEntity.ok().headers(headers).body("Access Token reissued successfully");
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}

package com.project.memberservice.controller;

import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.security.JwtUtil;
import com.project.memberservice.service.AuthService;
import com.project.memberservice.service.MailSendService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/")
public class AuthController {

    private final AuthService authService;
    private final MailSendService mailService;

    /* 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String refreshToken = request.getHeader("Refresh-Token");

        if (accessToken == null || refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok("Successfully logged out");
    }

    /* Refresh Token을 기반으로 Access Token 재발급 */
    @PostMapping("/reissueToken")
    public ResponseEntity getToken(@RequestBody String refreshToken) {
        try {
            // Refresh Token을 기반으로 Access Token 재발급
            String newAccessToken = authService.generateAccessToken(refreshToken);

            // 재발급 받은 Token 정보 header에 추가
            HttpHeaders headers = new HttpHeaders();
            headers.add(JwtUtil.AUTHORIZATION_HEADER, newAccessToken);

            return ResponseEntity.ok().headers(headers).body("Access Token이 재발급되었습니다.");
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /* 인증번호 전송 */
    @PostMapping("/mail")
    public ResponseEntity mailSend(@RequestHeader("X-Member-Id") String id) {
        log.info("memberId: {}", id);
        mailService.composeMail(id);
        return ResponseEntity.ok("인증번호 전송 완료");
    }

    /* 인증번호 검증 */
    @PostMapping("/mail/verify")
    public ResponseEntity AuthCheck(@RequestHeader("X-Member-Id") String id,
                                    @RequestParam String verificationCode){
        return authService.checkVerificationCode(id, verificationCode);
    }
}

package com.project.memberservice.controller;

import com.project.memberservice.security.UserDetailsImpl;
import com.project.memberservice.service.MailSendService;
import com.project.memberservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mail")
public class MailController {

    private final MemberService memberService;
    private final MailSendService mailService;

    /* 인증번호 전송 */
    @PostMapping("/auth")
    public ResponseEntity mailSend(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        mailService.composeMail(userDetails);
        return ResponseEntity.ok("인증번호 전송 완료");
    }

    /* 인증번호 검증 */
    @PostMapping("/auth/verify")
    public ResponseEntity AuthCheck(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                    @RequestParam String authNumber){
        return mailService.checkAuthNumber(userDetails, authNumber);
    }
}
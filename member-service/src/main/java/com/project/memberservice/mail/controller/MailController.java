package com.project.memberservice.mail.controller;

import com.project.memberservice.mail.dto.MailCheckDto;
import com.project.memberservice.mail.service.MailSendService;
import com.project.memberservice.security.UserDetailsImpl;
import com.project.memberservice.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mail")
public class MailController {

    private final MemberService memberService;
    private final MailSendService mailService;

    //이메일 인증 요청
    @PostMapping("/auth")
    public ResponseEntity mailSend(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String email = userDetails.getUsername();
        mailService.composeMail(email);
        return ResponseEntity.ok("인증번호 전송 완료");
    }

    //인증번호 검증
    @PostMapping("/auth/verify")
    public ResponseEntity AuthCheck(@Valid @RequestBody MailCheckDto mailCheckDto){
        return mailService.CheckAuthNumber(mailCheckDto.getEmail(), mailCheckDto.getAuthNum());
    }
}

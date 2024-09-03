package com.project.memberservice.controller;

import com.project.memberservice.service.MailSendService;
import com.project.memberservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mail")
public class MailController {

    private final MemberService memberService;
    private final MailSendService mailService;

    /* 인증번호 전송 */
    @PostMapping("/auth")
    public ResponseEntity mailSend(@RequestHeader("X-Member-Id") String id) {
        log.info("memberId: {}", id);
        mailService.composeMail(id);
        return ResponseEntity.ok("인증번호 전송 완료");
    }

    /* 인증번호 검증 */
    @PostMapping("/auth/verify")
    public ResponseEntity AuthCheck(@RequestHeader("X-Member-Id") String id,
                                    @RequestParam String authNumber){
        return mailService.checkAuthNumber(id, authNumber);
    }
}
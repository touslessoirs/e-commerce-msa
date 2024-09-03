package com.project.memberservice.controller;

import com.project.memberservice.dto.MemberRequestDto;
import com.project.memberservice.dto.MemberResponseDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.service.MemberService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/")
public class MemberController {

    @Value("${greeting.message}")
    private String greeting;
    private final Environment env;
    private final MemberService memberService;

    public MemberController(Environment env, MemberService memberService) {
        this.env = env;
        this.memberService = memberService;
    }

    @GetMapping("/health-check")
    @Timed(value = "member.health-check", longTask = true)
    public String status() {
        return String.format("MEMBER SERVICE Running on PORT "
                + env.getProperty("server.port")
                + ", token expiration time=" + env.getProperty("jwt.expiration_time"));
    }

    @GetMapping("/welcome")
    @Timed(value = "member.welcome", longTask = true)
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 회원 가입 */
    @PostMapping("/members")
    public ResponseEntity<MemberResponseDto> signUp(@Valid @RequestBody MemberRequestDto memberRequestDto) {
        MemberResponseDto memberResponseDto = memberService.signUp(memberRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponseDto);
    }

    /* 전체 사용자 조회 */
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponseDto>> getAllMembers(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("Authorization Header in Backend: {}", authorizationHeader);

        Iterable<Member> memberList = memberService.getAllMembers();

        List<MemberResponseDto> result = new ArrayList<>();
        memberList.forEach(member -> {
            result.add(new MemberResponseDto(member));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 사용자 정보 & 주문 내역 조회 */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<MemberResponseDto> getUser(@PathVariable("memberId") Long memberId) {
        MemberResponseDto memberResponseDto = memberService.getMemberByMemberId(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(memberResponseDto);
    }

    /* 회원 정보 수정 (비밀번호 제외) */
    @PutMapping("/members/{memberId}")
    public ResponseEntity<MemberResponseDto> modifyUser(@PathVariable("memberId") Long memberId,
                                                        @Valid @RequestBody MemberRequestDto memberRequestDto) {
        MemberResponseDto updatedMember = memberService.updateMember(memberId, memberRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(updatedMember);
    }

    /* 회원 탈퇴 */
    @PutMapping("/withdraw/{memberId}")
    public ResponseEntity deleteMember(@PathVariable("memberId") Long memberId) {
        memberService.withdraw(memberId);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}

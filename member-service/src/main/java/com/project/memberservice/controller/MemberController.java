package com.project.memberservice.controller;

import com.project.memberservice.dto.MemberRequestDto;
import com.project.memberservice.dto.MemberResponseDto;
import com.project.memberservice.dto.PasswordChangeRequestDto;
import com.project.memberservice.service.MemberService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class MemberController {

    @Value("${greeting.message}")
    private String greeting;

    @Value("${server.port}")
    private String port;

    @Value("${jwt.access_expiration_time}")
    private String expirationTime;

    private final MemberService memberService;

    @GetMapping("/welcome")
    @Timed(value = "member.welcome", longTask = true)
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 회원 가입 */
    @PostMapping("/signup")
    public ResponseEntity<MemberResponseDto> signup(@Valid @RequestBody MemberRequestDto memberRequestDto) {
        MemberResponseDto memberResponseDto = memberService.signup(memberRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponseDto);
    }

    /* 전체 사용자 조회 */
    @GetMapping("/allMembers")
    public ResponseEntity<Page<MemberResponseDto>> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MemberResponseDto> memberResponseDtoPage = memberService.getAllMembers(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(memberResponseDtoPage);
    }

    /* 회원 정보 & 주문 내역 조회 */
    @GetMapping("/members")
    public ResponseEntity<MemberResponseDto> getUser(@RequestHeader("X-Member-Id") String id) {
        MemberResponseDto memberResponseDto = memberService.getMemberByMemberId(id);

        return ResponseEntity.status(HttpStatus.OK).body(memberResponseDto);
    }

    /* 회원 정보 수정 (비밀번호 제외) */
    @PutMapping("/members")
    public ResponseEntity<MemberResponseDto> modifyUser(@RequestHeader("X-Member-Id") String id,
                                                        @Valid @RequestBody MemberRequestDto memberRequestDto) {
        MemberResponseDto updatedMember = memberService.updateMember(id, memberRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(updatedMember);
    }

    /* 비밀번호 변경 */
    @PutMapping("/members/change-password")
    public ResponseEntity<String> changePassword(@RequestHeader("X-Member-Id") String id,
                                                 @RequestBody PasswordChangeRequestDto passwordChangeRequestDto,
                                                 HttpServletRequest request) {
        memberService.changePassword(id, passwordChangeRequestDto, request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    /* 회원 탈퇴 */
    @PutMapping("/withdraw")
    public ResponseEntity deleteMember(@RequestHeader("X-Member-Id") String id,
                                       HttpServletRequest request) {
        memberService.withdraw(id, request);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}

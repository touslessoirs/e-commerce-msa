package com.project.memberservice.controller;

import com.project.memberservice.dto.MemberRequestDto;
import com.project.memberservice.dto.MemberResponseDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.exception.InvalidAdminTokenException;
import com.project.memberservice.service.MemberService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<MemberResponseDto> signUp(@RequestBody MemberRequestDto memberRequestDto) {
//        ModelMapper mapper = new ModelMapper();
//        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
//
        MemberResponseDto memberResponseDto = null;
        try {
            memberResponseDto = memberService.signUp(memberRequestDto);
        } catch (InvalidAdminTokenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponseDto);
    }

    /* 전체 사용자 조회 */
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponseDto>> getAllMembers(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("Authorization Header in Backend: {}", authorizationHeader);

        Iterable<Member> memberList = memberService.getAllMembers();

        List<MemberResponseDto> result = new ArrayList<>();
        memberList.forEach(v -> {
            result.add(new ModelMapper().map(v, MemberResponseDto.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 사용자 정보 & 주문 내역 조회 */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<MemberResponseDto> getUser(@PathVariable("memberId") Long memberId) {
        MemberResponseDto memberResponseDto = memberService.getMemberByMemberId(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(memberResponseDto);
    }
}

package com.project.memberservice.controller;

import com.project.memberservice.dto.MemberDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.service.MemberService;
import com.project.memberservice.vo.Greeting;
import com.project.memberservice.vo.MemberRequest;
import com.project.memberservice.vo.MemberResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class MemberController {

    private final Environment env;
    private final MemberService memberService;

    public MemberController(Environment env, MemberService memberService) {
        this.env = env;
        this.memberService = memberService;
    }

    @Autowired
    private Greeting greeting;

    @GetMapping("/health_check")
    public String status() {
        return "It's working in User Service";
    }

    //    @Operation(summary = "환영 메시지 출력 API", description = "Welcome message를 출력하기 위한 API")
    @GetMapping("/welcome")
//    @Timed(value="members.welcome", longTask = true)
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
//        System.out.println("users.welcome ip:" + request.getRemoteAddr() +
//                "," + request.getRemoteHost() +
//                "," + request.getRequestURI() +
//                "," + request.getRequestURL());
        return greeting.getMessage();
    }

    @PostMapping("/members")
    public ResponseEntity<MemberResponse> signup(@RequestBody MemberRequest memberRequest) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        MemberDto memberDto = mapper.map(memberRequest, MemberDto.class);
        MemberDto savedMemberDto = memberService.signup(memberDto);
        MemberResponse memberResponse = mapper.map(savedMemberDto, MemberResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

}

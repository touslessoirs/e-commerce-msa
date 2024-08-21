package com.project.memberservice.controller;

import com.project.memberservice.dto.MemberDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.service.MemberService;
import com.project.memberservice.vo.Greeting;
import com.project.memberservice.vo.MemberRequest;
import com.project.memberservice.vo.MemberResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("/health-check")
    public String status() {
        return String.format("MEMBER SERVICE Running on PORT "
                + env.getProperty("server.port")
                + ", token expiration time=" + env.getProperty("jwt.token.expiration_time"));
    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting.getMessage();
    }

    /* 회원 가입 */
    @PostMapping("/members")
    public ResponseEntity<MemberResponse> signUp(@RequestBody MemberRequest memberRequest) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        MemberDto memberDto = mapper.map(memberRequest, MemberDto.class);
        MemberDto savedMemberDto = memberService.signUp(memberDto);
        MemberResponse memberResponse = mapper.map(savedMemberDto, MemberResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

    /* 전체 사용자 조회 */
    @GetMapping("/members")
    public ResponseEntity<List<MemberResponse>> getAllMembers() {
        Iterable<Member> memberList = memberService.getAllMembers();

        List<MemberResponse> result = new ArrayList<>();
        memberList.forEach(v -> {
            result.add(new ModelMapper().map(v, MemberResponse.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 사용자 정보 & 주문 내역 조회 */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<MemberResponse> getUser(@PathVariable("memberId") Long memberId) {
        MemberDto memberDto = memberService.getMemberByMemberId(memberId);

        MemberResponse memberResponse = new ModelMapper().map(memberDto, MemberResponse.class);

        return ResponseEntity.status(HttpStatus.OK).body(memberResponse);


//        MemberDto memberDto = memberService.getMemberByMemberId(memberId);
//
//        if (memberDto == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//
//        MemberResponse returnValue = new ModelMapper().map(memberDto, MemberResponse.class);
//
//        EntityModel entityModel = EntityModel.of(returnValue);
//        WebMvcLinkBuilder linkTo = linkTo(methodOn(this.getClass()).getUsers());
//        entityModel.add(linkTo.withRel("all-users"));
//
//        try {
//            return ResponseEntity.status(HttpStatus.OK).body(entityModel);
//        } catch (Exception ex) {
//            throw new RuntimeException();
//        }
    }
}

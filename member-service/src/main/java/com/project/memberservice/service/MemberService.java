package com.project.memberservice.service;

import com.project.memberservice.client.OrderServiceClient;
import com.project.memberservice.dto.MemberRequestDto;
import com.project.memberservice.dto.MemberResponseDto;
import com.project.memberservice.dto.OrderResponseDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.UserRoleEnum;
import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.exception.FeignErrorDecoder;
import com.project.memberservice.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MemberService {

    @Value("${admin.token}")
    private String ADMIN_TOKEN;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;
    private final OrderServiceClient orderServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;
    private final CircuitBreakerFactory circuitBreakerFactory;

    public MemberService(MemberRepository memberRepository,
                         PasswordEncoder passwordEncoder,
                         Environment env,
                         OrderServiceClient orderServiceClient,
                         FeignErrorDecoder feignErrorDecoder,
                         CircuitBreakerFactory circuitBreakerFactory) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
        this.orderServiceClient = orderServiceClient;
        this.feignErrorDecoder = feignErrorDecoder;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /**
     * 회원가입
     *
     * @param memberRequestDto
     * @return MemberResponseDto
     */
    public MemberResponseDto signUp(MemberRequestDto memberRequestDto) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        try {
            // 사용자 ROLE 확인
            UserRoleEnum role = UserRoleEnum.USER;  //default

            //관리자 권한으로 가입 요청
            if (memberRequestDto.isAdmin()) {
                if (!ADMIN_TOKEN.equals(memberRequestDto.getAdminToken())) {
                    throw new CustomException(ErrorCode.INVALID_ADMIN_TOKEN);
                }
                log.info("관리자 가입 요청이 승인되었습니다.");
                role = UserRoleEnum.ADMIN;
            }

            Member member = mapper.map(memberRequestDto, Member.class);
            member.setPassword(passwordEncoder.encode(member.getPassword()));
            member.setRole(role);

            Member savedMember = memberRepository.save(member);

            MemberResponseDto memberResponseDto = mapper.map(savedMember, MemberResponseDto.class);
            return memberResponseDto;

        } catch (DataIntegrityViolationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    /**
     * 사용자 정보 & 주문 내역 조회
     *
     * @param memberId
     * @return MemberDto
     */
    public MemberResponseDto getMemberByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        MemberResponseDto memberResponseDto = new ModelMapper().map(member, MemberResponseDto.class);

        //주문 내역 조회
//        List<OrderResponseDto> orders = orderServiceClient.getOrdersByMemberId(memberId);
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
        List<OrderResponseDto> orders = circuitBreaker.run(() -> orderServiceClient.getOrdersByMemberId(memberId),
                throwable -> new ArrayList<>());
        memberResponseDto.setOrders(orders);

        return memberResponseDto;
    }

    /**
     * 전체 사용자 조회
     *
     * @return Member List
     */
    public Iterable<Member> getAllMembers() {
        return memberRepository.findAll();
    }

}
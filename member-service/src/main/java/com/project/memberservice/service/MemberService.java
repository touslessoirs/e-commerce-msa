package com.project.memberservice.service;

import com.project.memberservice.dto.MemberRequestDto;
import com.project.memberservice.dto.MemberResponseDto;
import com.project.memberservice.dto.OrderResponseDto;
import com.project.memberservice.dto.PasswordChangeRequestDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.UserRoleEnum;
import com.project.memberservice.entity.UserStatusEnum;
import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.exception.FeignErrorDecoder;
import com.project.memberservice.feign.OrderServiceClient;
import com.project.memberservice.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final AuthService authService;
    @Value("${admin.token}")
    private String ADMIN_TOKEN;

    private final CartService cartService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderServiceClient orderServiceClient;
    private final CircuitBreakerFactory circuitBreakerFactory;
    private final FeignErrorDecoder feignErrorDecoder;

    /**
     * 회원가입
     *
     * @param memberRequestDto 회원 가입에 필요한 정보
     * @return MemberResponseDto 회원 정보
     */
    @Transactional
    public MemberResponseDto signup(MemberRequestDto memberRequestDto) {
        try {
            // 1. 사용자 ROLE 확인
            UserRoleEnum role = UserRoleEnum.USER;  //default

            // 2. 관리자 권한으로 가입 요청
            if (memberRequestDto.isAdmin()) {
                if (!ADMIN_TOKEN.equals(memberRequestDto.getAdminToken())) {
                    throw new CustomException(ErrorCode.INVALID_ADMIN_TOKEN);
                }
                log.info("관리자 가입 요청이 승인되었습니다.");
                role = UserRoleEnum.ADMIN;
            }

            // 3. 회원 정보 저장
            Member member = new Member(
                    memberRequestDto.getEmail(),
                    passwordEncoder.encode(memberRequestDto.getPassword()),
                    memberRequestDto.getName(),
                    memberRequestDto.getPhone(),
                    memberRequestDto.getAddress(),
                    memberRequestDto.getAddressDetail(),
                    0,  //이메일 인증 미완료
                    UserStatusEnum.ACTIVE,
                    role
            );

            Member savedMember = memberRepository.save(member);

            // 4. 장바구니 생성
            cartService.createCart(member);

            MemberResponseDto memberResponseDto = new MemberResponseDto(savedMember);
            return memberResponseDto;

        } catch (DataIntegrityViolationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    /**
     * 회원 정보 & 주문 내역 조회
     *
     * @param id memberId
     * @return MemberDto 해당 회원의 정보 + 주문 내역 목록
     */
    public MemberResponseDto getMemberByMemberId(String id) {
        Long memberId = Long.parseLong(id);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        MemberResponseDto memberResponseDto = new MemberResponseDto(member);

        //주문 내역 조회
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
        List<OrderResponseDto> orders = circuitBreaker.run(() -> {
            ResponseEntity<List<OrderResponseDto>> responseEntity = orderServiceClient.getOrdersByMemberId(id);
            return responseEntity.getBody();
        }, throwable -> {
            return new ArrayList<>();
        });

        memberResponseDto.setOrders(orders);

        return memberResponseDto;
    }

    /**
     * 전체 사용자 조회
     *
     * @return Member 전체 사용자 목록
     */
    public Iterable<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * 회원 정보 수정 (비밀번호 제외)
     *
     * @param id memberId
     * @param memberRequestDto 수정하려는 회원 정보
     * @return MemberResponseDto 회원 정보
     */
    @Transactional
    public MemberResponseDto updateMember(String id, MemberRequestDto memberRequestDto) {
        Long memberId = Long.parseLong(id);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        member.setEmail(memberRequestDto.getEmail());
        member.setName(memberRequestDto.getName());
        member.setPhone(memberRequestDto.getPhone());
        member.setAddress(memberRequestDto.getAddress());
        member.setAddressDetail(memberRequestDto.getAddressDetail());

        Member updatedMember = memberRepository.save(member);
        return new MemberResponseDto(updatedMember);
    }

    /**
     * 비밀번호 변경
     * 변경 완료 시 로그아웃한다.
     *
     * @param id memberId
     * @param passwordChangeRequestDto 기존 비밀번호, 변경하려는 비밀번호
     */
    @Transactional
    public void changePassword(String id, PasswordChangeRequestDto passwordChangeRequestDto, HttpServletRequest request) {
        Long memberId = Long.parseLong(id);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(passwordChangeRequestDto.getCurrentPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 새 비밀번호 암호화
        String encodedNewPassword = passwordEncoder.encode(passwordChangeRequestDto.getNewPassword());
        member.setPassword(encodedNewPassword);

        // 새 비밀번호 저장
        memberRepository.save(member);
        
        // 로그아웃
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String refreshToken = request.getHeader("Refresh-Token");

        if (accessToken == null || refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        authService.logout(accessToken, refreshToken);
    }

    /**
     * 회원 탈퇴
     * 1. status DELETED로 수정
     * 2. 해당 회원의 장바구니, 장바구니에 담긴 상품 정보 삭제
     * 3. 탈퇴 완료 시 로그아웃한다.
     *
     * @param id memberId
     */
    @Transactional
    public void withdraw(String id, HttpServletRequest request) {
        Long memberId = Long.parseLong(id);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        member.setStatus(UserStatusEnum.DELETED);
        memberRepository.save(member);

        // 장바구니(담겨있던 상품 정보 포함) 삭제
        cartService.deleteCart(member);

        // 로그아웃
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String refreshToken = request.getHeader("Refresh-Token");

        if (accessToken == null || refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        authService.logout(accessToken, refreshToken);
    }

}
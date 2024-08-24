package com.project.memberservice.service;

import com.project.memberservice.client.OrderServiceClient;
import com.project.memberservice.entity.Member;
import com.project.memberservice.exception.FeignErrorDecoder;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.dto.MemberRequestDto;
import com.project.memberservice.dto.MemberResponseDto;
import com.project.memberservice.dto.OrderResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;
    private final OrderServiceClient orderServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;

    public MemberService(MemberRepository memberRepository,
                         PasswordEncoder passwordEncoder,
                         Environment env,
                         OrderServiceClient orderServiceClient,
                         FeignErrorDecoder feignErrorDecoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
        this.orderServiceClient = orderServiceClient;
        this.feignErrorDecoder = feignErrorDecoder;
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
            Member member = mapper.map(memberRequestDto, Member.class);
            member.setPassword(passwordEncoder.encode(member.getPassword()));

            Member savedMember = memberRepository.save(member);

            MemberResponseDto memberResponseDto = mapper.map(savedMember, MemberResponseDto.class);
            return memberResponseDto;

        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("phone_UNIQUE")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다.");
            } else if (e.getMessage().contains("email_UNIQUE")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "중복된 데이터가 존재합니다.");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "회원가입 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 사용자 정보 & 주문 내역 조회
     *
     * @param memberId
     * @return MemberDto
     */
    public MemberResponseDto getMemberByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            throw new UsernameNotFoundException("User not found");
        }
        MemberResponseDto memberResponseDto = new ModelMapper().map(member, MemberResponseDto.class);

        //주문 내역 조회
        List<OrderResponseDto> orders = orderServiceClient.getOrdersByMemberId(memberId);
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
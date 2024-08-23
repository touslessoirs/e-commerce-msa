package com.project.memberservice.service;

import com.project.memberservice.client.OrderServiceClient;
import com.project.memberservice.dto.MemberDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.exception.FeignErrorDecoder;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.vo.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
     * 회원 가입
     *
     * @param memberDto
     * @return memberDto
     */
    public MemberDto signUp(MemberDto memberDto) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        Member member = mapper.map(memberDto, Member.class);
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        Member savedMember = memberRepository.save(member);
        MemberDto returnDto = mapper.map(savedMember, MemberDto.class);

        return returnDto;
    }

    /**
     * 사용자 정보 & 주문 내역 조회
     *
     * @param memberId
     * @return MemberDto
     */
    public MemberDto getMemberByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if(member == null) {
            throw new UsernameNotFoundException("User not found");
        }
        MemberDto memberDto = new ModelMapper().map(member, MemberDto.class);

        //주문 내역 조회
        List<OrderResponse> orders = orders = orderServiceClient.getOrdersByMemberId(memberId);
        memberDto.setOrders(orders);

        return memberDto;
    }


    /**
     * 전체 사용자 조회
     *
     * @return Member List
     */
    public Iterable<Member> getAllMembers(){
        return memberRepository.findAll();
    }

}
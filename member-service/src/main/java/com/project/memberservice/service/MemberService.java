package com.project.memberservice.service;

import com.project.memberservice.dto.MemberDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.repository.MemberRepository;
import com.project.memberservice.vo.OrderResponse;
import com.thoughtworks.xstream.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MatchingStrategy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 회원 가입
     *
     * @param memberDto
     * @return memberDto (memberId 포함)
     */
    public MemberDto signup(MemberDto memberDto) {
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
     * @return
     */
    public MemberDto getMemberByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if(member == null) {
            throw new UsernameNotFoundException("User not found");
        }
        MemberDto memberDto = new ModelMapper().map(member, MemberDto.class);

        //주문 내역 조회
        List<OrderResponse> orders = new ArrayList<>();
        memberDto.setOrders(orders);

        return memberDto;
    }


    /**
     * 사용자 목록 조회
     *
     * @return
     */
    public Iterable<Member> getMemberByAll(){
        return memberRepository.findAll();
    }















}

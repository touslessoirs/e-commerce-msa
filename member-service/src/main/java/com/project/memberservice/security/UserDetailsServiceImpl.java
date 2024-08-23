package com.project.memberservice.security;

import com.project.memberservice.dto.MemberDto;
import com.project.memberservice.entity.Member;
import com.project.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * username(email)에 해당하는 UserDetails 조회
     *
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username);
        if(member == null) {
            throw new UsernameNotFoundException(username);
        }

        return new UserDetailsImpl(member);
    }

    /**
     * username(email)에 해당하는 MemberDto 조회 (-> memberId 조회)
     *
     * @param username
     * @return MemberDto
     */
    public MemberDto getMemberDetailsByEmail(String username) {
        Member member = memberRepository.findByEmail(username);
        if(member == null) {
            throw new UsernameNotFoundException(username);
        }

        MemberDto memberDto = new ModelMapper().map(member, MemberDto.class);
        return memberDto;
    }
}
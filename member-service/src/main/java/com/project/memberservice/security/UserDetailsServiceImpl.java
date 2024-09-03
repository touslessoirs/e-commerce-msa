package com.project.memberservice.security;

import com.project.memberservice.entity.Member;
import com.project.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
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
     * @param username email
     * @return UserDetails
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username).orElse(null);
        if(member == null) {
            throw new UsernameNotFoundException(username);
        }

        return new UserDetailsImpl(member);
    }
}
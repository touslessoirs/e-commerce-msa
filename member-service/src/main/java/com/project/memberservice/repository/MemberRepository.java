package com.project.memberservice.repository;

import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.UserStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByMemberId(Long memberId);

    /**
     * 특정 상태의 회원 중에서 주어진 날짜보다 이전에 로그인한 회원 목록을 조회한다.
     *
     * @param status 조회할 회원의 status
     * @param cutoffDate 기준 시점(이 날짜 이전에 로그인한 경우)
     * @return 해당 조건에 맞는 회원 목록
     */
    Page<Member> findAllByStatusAndLastLoginTimeBefore(UserStatusEnum status, LocalDateTime cutoffDate, Pageable pageable);
}

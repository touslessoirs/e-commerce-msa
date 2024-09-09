package com.project.memberservice.service;

import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.UserStatusEnum;
import com.project.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@EnableKafka
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskManager {

    private final MemberRepository memberRepository;

    /**
     * 휴면 회원 전환
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 실행
    public void updateMemberStatus() {
        // 1년 전 날짜 계산
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);

        // 1년 동안 로그인하지 않은 ACTIVE(활성 회원) 회원 조회
        List<Member> inactivateMembers = memberRepository.findAllByStatusAndLastLoginTime(UserStatusEnum.ACTIVE, cutoffDate);

        // INACTIVE(휴면 회원)으로 전환
        for (Member member : inactivateMembers) {
            member.setStatus(UserStatusEnum.DORMANT);
            memberRepository.save(member);
        }

        log.info("휴면 회원 전환 완료. 전환된 사용자 수: {}", inactivateMembers.size());
    }

}

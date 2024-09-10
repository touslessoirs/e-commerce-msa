package com.project.memberservice.config;

import com.project.memberservice.entity.Member;
import com.project.memberservice.entity.UserStatusEnum;
import com.project.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchConfig {

    private final String JOB_NAME = "updateMemberStatusJob";
    private final String STEP_NAME = "updateMemberStatusStep";
    private final MemberRepository memberRepository;

    /**
     * 휴면 회원 업데이트 Job 등록
     */
    @Bean
    public Job updateMemberStatusJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(updateMemberStatusStep(jobRepository, transactionManager))
                .build();
    }

    /**
     * 휴면 회원 업데이트 Step 등록 (Chunk 기반)
     */
    @Bean
    @JobScope
    public Step updateMemberStatusStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Member, Member>chunk(50, transactionManager) // chunk size
                .reader(memberItemReader())
                .processor(memberItemProcessor())
                .writer(memberItemWriter())
                .transactionManager(transactionManager)
                .build();
    }

    /**
     * ItemReader: 업데이트할 회원을 조회한다.
     */
    @Bean
    @StepScope
    public RepositoryItemReader<Member> memberItemReader() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
        return new RepositoryItemReaderBuilder<Member>()
                .name("memberItemReader")
                .repository(memberRepository)
                .methodName("findAllByStatusAndLastLoginTimeBefore")
                .arguments(UserStatusEnum.ACTIVE, cutoffDate)
                .pageSize(50)  // page size
                .sorts(Collections.singletonMap("memberId", org.springframework.data.domain.Sort.Direction.ASC)) // 정렬 기준
                .build();
    }

    /**
     * ItemProcessor: 회원 상태를 업데이트한다.
     */
    @Bean
    @StepScope
    public ItemProcessor<Member, Member> memberItemProcessor() {
        return member -> {
            member.setStatus(UserStatusEnum.DORMANT);
            return member;
        };
    }

    /**
     * ItemWriter: 업데이트된 회원을 저장한다.
     */
    @Bean
    @StepScope
    public ItemWriter<Member> memberItemWriter() {
        return members -> {
            memberRepository.saveAll(members);
        };
    }
}
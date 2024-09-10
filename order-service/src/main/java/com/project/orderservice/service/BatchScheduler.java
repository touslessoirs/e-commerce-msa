package com.project.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class BatchScheduler {

    private final String JOB_NAME = "updateOrderStatusJob";
    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;

    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시 실행
    public void runUpdateOrderStatusJob() {
        try {
            Job job = jobRegistry.getJob(JOB_NAME);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("time", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);
            log.info(">>> 주문 상태 업데이트 작업 실행 완료");
        } catch (Exception e) {
            log.error(">>> 주문 상태 업데이트 작업 실행 중 오류 발생", e);
        }
    }
}

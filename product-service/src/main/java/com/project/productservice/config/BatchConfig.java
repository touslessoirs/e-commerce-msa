package com.project.productservice.config;

import com.project.productservice.entity.Product;
import com.project.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchConfig {

    private final String JOB_NAME = "productSyncJob";
    private final String STEP_NAME = "productSyncStep";
    private static final String STOCK_KEY_PREFIX = "stock_ID: ";
    private static final String PURCHASE_KEY_PREFIX = "purchase_start_time_ID: ";

    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean
    public Job productSyncJob(JobRepository jobRepository, Step productSyncStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(productSyncStep)
                .build();
    }

    @Bean
    public Step productSyncStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Product, Product>chunk(20, transactionManager)    // chunk size
                .reader(productSyncReader())
                .processor(productSyncProcessor())
                .writer(productSyncWriter())
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public ItemReader<Product> productSyncReader() {
        return new RepositoryItemReaderBuilder<Product>()
                .name("productSyncReader")
                .repository(productRepository)
                .methodName("findAll")
                .pageSize(20)
                .sorts(Collections.singletonMap("productId", org.springframework.data.domain.Sort.Direction.ASC))   // 정렬 기준
                .build();
    }

    @Bean
    public ItemProcessor<Product, Product> productSyncProcessor() {
        return product -> {
            // 재고 동기화
            synchronizeRedisAndDb(
                    STOCK_KEY_PREFIX + product.getProductId(),
                    String.valueOf(product.getStock())
            );

            // 구매 가능 시간 동기화
            synchronizeRedisAndDb(
                    PURCHASE_KEY_PREFIX + product.getProductId(),
                    product.getPurchaseStartTime().toString()
            );

            return product;
        };
    }

    private void synchronizeRedisAndDb(String redisKey, String dbValue) {
        String redisValue = redisTemplate.opsForValue().get(redisKey);

        if (redisValue != null && !redisValue.equals(dbValue)) {
            redisTemplate.opsForValue().set(redisKey, dbValue);
        }
    }

    @Bean
    public ItemWriter<Product> productSyncWriter() {
        return items -> {
        };
    }
}

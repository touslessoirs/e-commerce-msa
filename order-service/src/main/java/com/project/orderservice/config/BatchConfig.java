package com.project.orderservice.config;

import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderStatusEnum;
import com.project.orderservice.repository.OrderRepository;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchConfig {

    private final String JOB_NAME = "updateOrderStatusJob";
    private final String STEP_NAME = "updateOrderStatusStep";
    private final OrderRepository orderRepository;

//    /**
//     * 주문 상태 업데이트 Job 등록
//     */
//    @Bean
//    public Job updateOrderStatusJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
//        return new JobBuilder(JOB_NAME, jobRepository)
//                .incrementer(new RunIdIncrementer())
//                .start(updateOrderStatusStep(jobRepository, transactionManager))  // Step 설정
//                .build();
//    }
//
//    /**
//     * 주문 상태 업데이트 Step 등록
//     */
//    @Bean
//    @JobScope
//    public Step updateOrderStatusStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
//        return new StepBuilder(STEP_NAME, jobRepository)
//                .tasklet(updateOrderStatusTasklet(), transactionManager)
//                .build();
//    }
//
//    /**
//     * 주문 상태 업데이트 Tasklet
//     */
//    @Bean
//    @StepScope
//    public Tasklet updateOrderStatusTasklet() {
//        return (contribution, chunkContext) -> {
//            LocalDateTime now = LocalDateTime.now();
//
//            // 1. 결제 대기 → 주문 취소
//            List<Order> ordersToCancelled = orderRepository.findAllByStatusAndModifiedAtBefore(
//                    OrderStatusEnum.PAYMENT_PENDING, now.minusDays(1));
//            for (Order order : ordersToCancelled) {
//                order.setStatus(OrderStatusEnum.CANCELLED);
//            }
//
//            // 2. 결제 완료 → 배송 중
//            List<Order> ordersToShipping = orderRepository.findAllByStatusAndModifiedAtBefore(
//                    OrderStatusEnum.PAYMENT_COMPLETED, now.minusDays(1));
//            for (Order order : ordersToShipping) {
//                order.setStatus(OrderStatusEnum.SHIPPING);
//            }
//
//            // 3. 배송 중 -> 배송 완료
//            List<Order> ordersToDelivered = orderRepository.findAllByStatusAndModifiedAtBefore(
//                    OrderStatusEnum.SHIPPING, now.minusDays(2));
//            for (Order order : ordersToDelivered) {
//                order.setStatus(OrderStatusEnum.DELIVERED);
//            }
//
//            // 4. 배송 완료 -> 주문 확정
//            List<Order> ordersToConfirmed = orderRepository.findAllByStatusAndModifiedAtBefore(
//                    OrderStatusEnum.DELIVERED, now.minusDays(3));
//            for (Order order : ordersToConfirmed) {
//                order.setStatus(OrderStatusEnum.ORDER_CONFIRMED);
//            }
//
//            orderRepository.saveAll(ordersToCancelled);
//            orderRepository.saveAll(ordersToShipping);
//            orderRepository.saveAll(ordersToDelivered);
//            orderRepository.saveAll(ordersToConfirmed);
//
//            return RepeatStatus.FINISHED;
//        };
//    }

    /**
     * 주문 상태 업데이트 Job 등록
     */
    @Bean
    public Job updateOrderStatusJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(updateOrderStatusStep(jobRepository, transactionManager))
                .build();
    }

    /**
     * 주문 상태 업데이트 Step 등록 (Chunk 기반)
     */
    @Bean
    @JobScope
    public Step updateOrderStatusStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Order, Order>chunk(10, transactionManager) // chunk size
                .reader(orderItemReader())
                .processor(orderItemProcessor())
                .writer(orderItemWriter())
                .transactionManager(transactionManager)
                .build();
    }

    /**
     * ItemReader: 업데이트할 주문을 조회한다.
     */
    @Bean
    @StepScope
    public ItemReader<Order> orderItemReader() {
        LocalDateTime now = LocalDateTime.now();

        return new ItemReader<Order>() {
            private int index = 0;
            private List<Order> orders;

            @Override
            public Order read() {
                if (orders == null) {
                    orders = orderRepository.findAllByStatusAndModifiedAtBefore(
                            OrderStatusEnum.PAYMENT_PENDING, now.minusDays(1));
                    orders.addAll(orderRepository.findAllByStatusAndModifiedAtBefore(
                            OrderStatusEnum.PAYMENT_COMPLETED, now.minusDays(1)));
                    orders.addAll(orderRepository.findAllByStatusAndModifiedAtBefore(
                            OrderStatusEnum.SHIPPING, now.minusDays(2)));
                    orders.addAll(orderRepository.findAllByStatusAndModifiedAtBefore(
                            OrderStatusEnum.DELIVERED, now.minusDays(3)));
                }

                if (index < orders.size()) {
                    return orders.get(index++);
                } else {
                    return null; // 더 이상 읽을 데이터가 없을 경우 종료
                }
            }
        };
    }

    /**
     * ItemProcessor: 주문 상태를 업데이트한다.
     */
    @Bean
    @StepScope
    public ItemProcessor<Order, Order> orderItemProcessor() {
        return order -> {
            LocalDateTime now = LocalDateTime.now();

            if (order.getStatus() == OrderStatusEnum.PAYMENT_PENDING && order.getModifiedAt().isBefore(now.minusDays(1))) {
                order.setStatus(OrderStatusEnum.CANCELLED);
            } else if (order.getStatus() == OrderStatusEnum.PAYMENT_COMPLETED && order.getModifiedAt().isBefore(now.minusDays(1))) {
                order.setStatus(OrderStatusEnum.SHIPPING);
            } else if (order.getStatus() == OrderStatusEnum.SHIPPING && order.getModifiedAt().isBefore(now.minusDays(2))) {
                order.setStatus(OrderStatusEnum.DELIVERED);
            } else if (order.getStatus() == OrderStatusEnum.DELIVERED && order.getModifiedAt().isBefore(now.minusDays(3))) {
                order.setStatus(OrderStatusEnum.ORDER_CONFIRMED);
            }

            return order;
        };
    }

    /**
     * ItemWriter: 업데이트된 주문을 저장한다.
     */
    @Bean
    @StepScope
    public ItemWriter<Order> orderItemWriter() {
        return orders -> orderRepository.saveAll(orders);
    }
}
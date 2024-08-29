package com.project.productservice.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @DistributedLock 선언 시 수행되는 Aop class
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    /**
     * @param joinPoint
     * @return
     * @throws Throwable
     * @Around : @DistributedLock 어노테이션이 적용된 메서드들에 대해 해당 메서드가 실행됨
     */
    @Around("@annotation(com.project.productservice.aop.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //메서드명, 파라미터 확인
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(),
                joinPoint.getArgs(),
                distributedLock.key());
        RLock rLock = redissonClient.getLock(key);  // (1) Redisson Clinet로 부터 RLock을 얻을 때 key를 주입한다.

        try {
            boolean available = rLock.tryLock(distributedLock.waitTime(),
                    distributedLock.leaseTime(), distributedLock.timeUnit());  // (2) 지정된 시간 동안 락을 시도
            if (!available) {
                return false;
            } else {
                log.info("Redisson Lock Acquired: {}", key); // 락을 얻었을 때 -> key 값 출력
                return aopForTransaction.proceed(joinPoint);  // (3) 트랜잭션 처리
            }
        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            try {
                rLock.unlock();   // (4) 종료시 락 해제
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already UnLock {} {}",
                        kv("serviceName", method.getName()),
                        kv("key", key)
                );
            }
        }
    }

    private Map<String, String> kv(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

}

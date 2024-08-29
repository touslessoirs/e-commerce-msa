package com.project.productservice.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String key();   //락의 이름
    TimeUnit timeUnit() default TimeUnit.SECONDS;   //락의 시간 단위
    long waitTime() default 1L; //락을 획득하기 위해 시도하는 최대 대기 시간 (1s)
    long leaseTime() default 3L;    //락을 획득한 후, 이 락이 자동으로 해제되기까지의 시간 (3s)
}
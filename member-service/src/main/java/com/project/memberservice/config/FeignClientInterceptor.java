package com.project.memberservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // feign 요청에 헤더 추가
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        template.header("X-Member-Id", memberId);
    }
}
package com.project.memberservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 606480)  //Redis key prefix, 유효시간: 7일
public class RefreshToken {
    @Id
    private String refreshToken;    //Redis key (UUID)

    private Long memberId;  //Redis value
}

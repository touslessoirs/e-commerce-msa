package com.project.memberservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 60)  //Redis key prefix, 유효시간: 4시간 (14440)
public class RefreshToken {
    @Id
    private String refreshToken;    //Redis key (UUID)

    private Long memberId;  //Redis value
}

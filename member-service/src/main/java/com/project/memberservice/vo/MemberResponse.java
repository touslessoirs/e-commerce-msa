package com.project.memberservice.vo;

import lombok.Data;

@Data
public class MemberResponse {
    private Long memberId;
    private String email;
    private String name;
}

package com.project.memberservice.dto;

import lombok.Data;

@Data
public class MemberDto {
    private Long memberId;
    private String email;
    private String name;
    private String password;
    private String phone;
    private String address;
    private String addressDetail;
}
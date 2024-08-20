package com.project.memberservice.dto;

import com.project.memberservice.vo.OrderResponse;
import lombok.Data;

import java.util.List;

@Data
public class MemberDto {
    private Long memberId;
    private String email;
    private String name;
    private String password;
    private String phone;
    private String address;
    private String addressDetail;

    private List<OrderResponse> orders;
}
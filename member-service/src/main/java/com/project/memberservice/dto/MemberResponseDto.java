package com.project.memberservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberResponseDto {
    private Long memberId;
    private String email;
    private String name;
    private String address;
    private String addressDetail;
    private String phone;
    private LocalDateTime createdAt;

    private List<OrderResponseDto> orders;

    public MemberResponseDto(Long memberId, String email, String name, String address, String addressDetail, String phone, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.email = email;
        this.name = name;
        this.address = address;
        this.addressDetail = addressDetail;
        this.phone = phone;
        this.createdAt = createdAt;
    }
}

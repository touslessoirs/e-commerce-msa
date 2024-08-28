package com.project.memberservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.memberservice.entity.Member;
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

    public MemberResponseDto(Member member){
        this.memberId = member.getMemberId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.address = member.getAddress();
        this.addressDetail = member.getAddressDetail();
        this.phone = member.getPhone();
        this.createdAt = member.getCreatedAt();
    }
}

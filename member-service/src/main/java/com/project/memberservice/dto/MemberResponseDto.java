package com.project.memberservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

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

    private List<OrderResponseDto> orders;
}

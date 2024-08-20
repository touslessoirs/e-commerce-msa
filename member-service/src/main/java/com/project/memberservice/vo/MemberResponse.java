package com.project.memberservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberResponse {
    private Long memberId;
    private String email;
    private String name;
    private String phone;
    private String address;
    private String addressDetail;
    private Enum role;

    private List<OrderResponse> orders;
}

package com.project.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberRequestDto {
    @Email
    @NotNull(message = "email 입력은 필수입니다.")
    private String email;
    @NotNull(message = "이름 입력은 필수입니다.")
    private String name;
    @NotNull(message = "비밀번호 입력은 필수입니다.")
    private String password;
    @NotNull(message = "전화번호 입력은 필수입니다.")
    private String phone;
    @NotNull(message = "주소 입력은 필수입니다.")
    private String address;
    @NotNull(message = "주소 입력은 필수입니다.")
    private String addressDetail;
}

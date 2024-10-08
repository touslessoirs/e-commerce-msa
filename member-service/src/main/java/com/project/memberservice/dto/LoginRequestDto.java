package com.project.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    @Email
    @NotNull(message = "email 입력은 필수입니다.")
    private String email;
    @NotNull(message = "비밀번호 입력은 필수입니다.")
    private String password;
}

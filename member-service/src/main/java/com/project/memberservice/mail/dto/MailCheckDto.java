package com.project.memberservice.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MailCheckDto {
    @Email
    @NotEmpty(message = "이메일을 입력하세요.")
    private String email;
    @NotEmpty(message = "인증번호를 입력하세요.")
    private String authNum;
}

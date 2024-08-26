package com.project.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberRequestDto {
    @Email
    @NotNull(message = "USER-002")
    private String email;
    @NotNull(message = "USER-002")
    private String name;
    @NotNull(message = "USER-002")
    private String password;
    @NotNull(message = "USER-002")
    private String phone;
    @NotNull(message = "USER-002")
    private String address;
    @NotNull(message = "USER-002")
    private String addressDetail;

    private boolean admin = false;
    private String adminToken = "";
}

package com.project.memberservice.dto;

import com.project.memberservice.entity.UserRoleEnum;
import lombok.Data;

@Data
public class UserInfoDto {
    private Long memberId;
    private String email;
    private String password;
    private UserRoleEnum role;
}

package com.project.memberservice.dto;

import com.project.memberservice.entity.UserRoleEnum;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoDto {
    private Long memberId;
    private String email;
    private String password;
    private UserRoleEnum role;
}

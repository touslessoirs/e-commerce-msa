package com.project.memberservice.entity;

public enum UserStatusEnum {
    ACTIVE("활성 회원"),
    DORMANT("휴면 회원"),
    DELETED("탈퇴 회원");

    private final String description;

    UserStatusEnum(String description) {
        this.description = description;
    }
}

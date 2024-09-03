package com.project.memberservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class Member extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long memberId;

    @Column(nullable = false, length = 50, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 200)
    private String addressDetail;

    @Column(name = "is_verified")
    private Integer isVerified;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private UserRoleEnum role;

    public Member(String email, String password, String name, String phone, String address, String addressDetail, UserRoleEnum role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.addressDetail = addressDetail;
        isVerified = 0;
        isDeleted = 0;
        this.role = role;
    }
}

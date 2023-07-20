package com.sparta.outsideworld.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
public class User extends TimeStamped{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 500)
    private String introduction;

    @Column(name = "profile_image")
    private String image;

    ///////////////
    @JsonIgnore
    @Column(nullable = true)
    private String oldPassword1;

    @JsonIgnore
    @Column(nullable = true)
    private String oldPassword2;
    //////////////

    // 관리자 권한 로그인
    // @ColumnDefault("user")
    // DB 엔 UserRoleEnum 이 없기 때문에 Annotation 을 통해 String 임을 밝힌다
    @Enumerated(value = EnumType.STRING)
    // ADMIN, USER 로 타입 고정
    private UserRoleEnum role;


    @Builder
    public User(String username, String password, String email, String introduction, String image, UserRoleEnum role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.introduction = introduction;
        this.image = image;
        this.role = role;
    }

}

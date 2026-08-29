package com.kim.tudu_api.util;

import com.kim.tudu_api.user.model.UserEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestUsers {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    public static UserEntity USER1 = UserEntity.builder()
            .username("DunLappin")
            .email("allen.dunlap@test.com")
            .password(ENCODER.encode("allen"))
            .build();

    public static UserEntity USER2 = UserEntity.builder()
            .username("Aysh4Ninja")
            .email("aysha.seymour@test.com")
            .password(ENCODER.encode("aysha"))
            .build();

    public static UserEntity USER3 = UserEntity.builder()
            .username("J4smin3")
            .email("jasmine.duncan@test.com")
            .password(ENCODER.encode("jasmine"))
            .build();

    public static UserEntity USER4 = UserEntity.builder()
            .username("WyaTornado")
            .email("wyatt.corbett@test.com")
            .password(ENCODER.encode("wyatt"))
            .build();

    public static UserEntity USER5 = UserEntity.builder()
            .username("Milanator")
            .email("milan.hyde@test.com")
            .password(ENCODER.encode("milan"))
            .build();

    public static UserEntity USER6 = UserEntity.builder()
            .username("CleoQuest")
            .email("cleo.woods@test.com")
            .password(ENCODER.encode("cleo"))
            .build();

    public static UserEntity USER7 = UserEntity.builder()
            .username("Roberta_The_Great")
            .email("roberta.english@test.com")
            .password(ENCODER.encode("roberta"))
            .build();

    public static UserEntity USER8 = UserEntity.builder()
            .username("LukaNautical")
            .email("luka.dickson@test.com")
            .password(ENCODER.encode("luka"))
            .build();

    public static UserEntity USER9 = UserEntity.builder()
            .username("Edw1n_Chrono")
            .email("edwin.lugo@test.com")
            .password(ENCODER.encode("edwin"))
            .build();

    public static UserEntity USER10 = UserEntity.builder()
            .username("Malik_Stormbringer")
            .email("malik.dejesus@test.com")
            .password(ENCODER.encode("malik"))
            .build();
}

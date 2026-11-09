package com.baton.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private long expiresIn;
    private MemberInfoResponse member;

    public static LoginResponse of(String accessToken, long expiresIn, MemberInfoResponse member) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .member(member)
                .build();
    }
}


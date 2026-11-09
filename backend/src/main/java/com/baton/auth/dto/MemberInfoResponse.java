package com.baton.auth.dto;

import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberInfoResponse {

    private String name;
    private Department department;

    @JsonProperty("rank")
    private Rank rank;

    public static MemberInfoResponse of(String name, Department department, Rank rank) {
        return MemberInfoResponse.builder()
                .name(name)
                .department(department)
                .rank(rank)
                .build();
    }
}

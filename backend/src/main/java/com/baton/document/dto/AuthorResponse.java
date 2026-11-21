package com.baton.document.dto;

import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;

/** "author": { "name": "김영업", "rank": "STAFF" } — 내부 id·uuid·email은 내려주지 않는다 */
public record AuthorResponse(String name, Rank rank) {

    public static AuthorResponse from(Member member) {
        return new AuthorResponse(member.getName(), member.getJobRank());
    }
}

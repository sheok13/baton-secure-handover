package com.baton.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /chat/sessions/{id}/messages 요청 (API 설계서 5.1). 부서·직급 등 권한 값은 받지 않는다 */
public record ChatQuestionRequest(
        @NotBlank @Size(max = 1000) String question
) {
}

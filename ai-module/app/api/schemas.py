"""요청·응답 모양 (D 소유). JSON 필드 이름은 API 설계서 6장(camelCase)과 같게 쓴다."""
from pydantic import BaseModel, Field, field_validator
from typing import Literal
from app.core.constants import Department, Rank

class HistoryMessage(BaseModel):
    role: Literal["USER", "ASSISTANT"]
    content: str = Field(max_length=4000)

class ChatRequest(BaseModel):
    question: str = Field(min_length=1, max_length=1000)
    filter: AccessFilter
    history: list[HistoryMessage] = Field(default_factory=list)

    @field_validator("question")
    @classmethod
    def _strip_question(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("빈 질문")
        return v
    
class AccessFilter(BaseModel):
    """질의 때 Backend가 보내는 권한 필터 (E의 retriever가 사용). 비어 있으면 거부."""
    departments: list[Department] = Field(min_length=1)
    ranks: list[Rank] = Field(min_length=1)


class IngestResponse(BaseModel):
    docId: int
    chunkCount: int
    maskedCount: int


class DeleteResponse(BaseModel):
    docId: int
    deletedCount: int
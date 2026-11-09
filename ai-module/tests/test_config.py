import pytest
from pydantic import ValidationError
 
from app.core.config import Settings
 
BASE = {
    "_env_file": None,  # 개발자 PC의 .env를 읽지 않고 이 테스트 값만 쓴다
    "ai_internal_api_key": "x" * 16,
    "llm_provider": "groq",
    "llm_model": "test-model",
    "groq_api_key": "test-groq-key",
    "vector_store_path": "./data/chroma/test",
}
 
 
def test_valid_settings():
    s = Settings(**BASE)
    assert s.groq_api_key.get_secret_value() == "test-groq-key"
    assert "test-groq-key" not in repr(s)  # SecretStr: 출력에 키가 보이지 않음
 
 
@pytest.mark.parametrize(
    "override, missing_name",
    [
        ({"groq_api_key": ""}, "GROQ_API_KEY"),
        ({"llm_model": ""}, "LLM_MODEL"),
        ({"vector_store_path": ""}, "VECTOR_STORE_PATH"),
        ({"ai_internal_api_key": "short"}, "AI_INTERNAL_API_KEY"),
        ({"llm_provider": "openai"}, "OPENAI_API_KEY"),
    ],
)
def test_missing_required_fails_fast(override, missing_name):
    with pytest.raises(ValidationError) as e:
        Settings(**{**BASE, **override})
    assert missing_name in str(e.value)
-- ==========================================================
-- V3__rotate_seed_passwords.sql — 시연 계정 비밀번호 해시 긴급 교체 (Credential Rotation)
-- ⚠️ 보안 주의사항:
--   - 시드 계정(4개)의 비밀번호를 안전한 새 비밀번호로 교체합니다.
--   - 본 SQL 및 커밋 내역에는 BCrypt 해시만 저장되며, 평문 비밀번호는 포함하지 않습니다.
--   - 시연용 평문 비밀번호는 내부 팀 채널을 통해서만 공유됩니다.
-- ==========================================================

UPDATE members 
SET password_hash = '$2a$10$WAtqe7.WDP8APAKLJhhd9u4Al/gAvcUiTJEQY0hy.rYo62X8JLSCO',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'sales.staff@baton.local';

UPDATE members 
SET password_hash = '$2a$10$06bpICvLIgFNBL5i7fpE1O5M/LqBrPLlNzCGIu7XeJdv8Y3jWfMzC',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'sales.lead@baton.local';

UPDATE members 
SET password_hash = '$2a$10$.7adi6ShiAfsZu9fDCZFLueTSn8DC04ZqvGKz8RePS6pk9bNQ5V5K',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'dev.staff@baton.local';

UPDATE members 
SET password_hash = '$2a$10$fCPBkE5EvPbQ8P27KLMuYOusoSQbimshTRMYkLChH96F18utRGwLS',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'dev.lead@baton.local';


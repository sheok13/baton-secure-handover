-- ==========================================================
-- V2__seed_members.sql — 시연 계정 4개 (DB 설계서 6장)
-- 비밀번호는 BCrypt 해시만 저장. 평문은 Git·문서에 쓰지 않고 팀 채널로만 공유
-- ⚠️ develop 머지 후에는 수정 금지
-- ==========================================================
INSERT INTO members (uuid, email, password_hash, name, department, job_rank) VALUES
('3ab5d75f-91e4-4b95-8e9a-37093faf3b70', 'sales.staff@baton.local', '<HASH_1>', '김영업', 'SALES', 'STAFF'),
('a054e2d1-1ec5-4f53-891d-aa3919a6299b', 'sales.lead@baton.local',  '<HASH_2>', '박영업', 'SALES', 'LEADER'),
('b40af7fc-a592-44f2-bd88-9d3abb7a2ca7', 'dev.staff@baton.local',   '<HASH_3>', '이개발', 'DEV',   'STAFF'),
('4cd0cf01-fcb0-437f-a774-ca53171cf63d', 'dev.lead@baton.local',    '<HASH_4>', '최개발', 'DEV',   'LEADER');

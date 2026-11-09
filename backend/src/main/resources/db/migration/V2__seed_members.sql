-- ==========================================================
-- V2__seed_members.sql — 시연 계정 4개 (DB 설계서 6장)
-- 비밀번호는 BCrypt 해시만 저장. 평문은 Git·문서에 쓰지 않고 팀 채널로만 공유
-- ⚠️ develop 머지 후에는 수정 금지
-- ==========================================================
INSERT INTO members (uuid, email, password_hash, name, department, job_rank) VALUES
('3ab5d75f-91e4-4b95-8e9a-37093faf3b70', 'sales.staff@baton.local', '$2a$10$rDm0dzRAPvonmWJp0WO4DOSvPHJjWHCPXEsh28nzb5lpxDM9vDhhO', '김영업', 'SALES', 'STAFF'),
('a054e2d1-1ec5-4f53-891d-aa3919a6299b', 'sales.lead@baton.local',  '$2a$10$.rtGj9nYlqrBV9Jl/81LVemtsYJ0agm9x3m5DNfFrD.tBs6PBLwSK', '박영업', 'SALES', 'LEADER'),
('b40af7fc-a592-44f2-bd88-9d3abb7a2ca7', 'dev.staff@baton.local',   '$2a$10$vc6cmkVPzIrSI6qugEmODeagM3cFCiQt/tkE6Z9LCzMAfpfMb9mTa', '이개발', 'DEV',   'STAFF'),
('4cd0cf01-fcb0-437f-a774-ca53171cf63d', 'dev.lead@baton.local',    '$2a$10$qK3xWtfsGIrfded4zmyvlOPS8HYpzGjPHj19xC/fNWmsQJ8XGkDIi', '최개발', 'DEV',   'LEADER');

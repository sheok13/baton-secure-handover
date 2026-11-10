import { RANK, DEPT } from '../constants/index.js'

export const users = [
  {
  id: 1,                                  // 1~4 등 고유 숫자
  name: '김영업',                          // '김영업'
  email: 'sales.staff@baton.local',       // 'sales.staff@baton.local'
  password: 'password1',                  // mock 로그인 비교용 임시 값 (실제 값은 팀 시드 문서 확인)
  department: DEPT.SALES,                       // DEPT.SALES  ← 문자열 직접 쓰지 말고 상수 사용
  rank: RANK.STAFF,                       // RANK.STAFF
}
,
  {
  id: 2,                                  // 1~4 등 고유 숫자
  name: '박영업',                          // '박영업'
  email: 'sales.lead@baton.local',        // 'sales.lead@baton.local'
  password: 'password1',                  // mock 로그인 비교용 임시 값 (실제 값은 팀 시드 문서 확인)
  department: DEPT.SALES,                       // DEPT.SALES  ← 문자열 직접 쓰지 말고 상수 사용
  rank: RANK.LEADER,                      // RANK.LEADER
}
,
  {
  id: 3,                                  // 1~4 등 고유 숫자
  name: '이개발',                          // '이개발'
  email: 'dev.staff@baton.local',         // 'dev.staff@baton.local'
  password: 'password1',                  // mock 로그인 비교용 임시 값 (실제 값은 팀 시드 문서 확인)
  department: DEPT.DEV,                         // DEPT.DEV  ← 문자열 직접 쓰지 말고 상수 사용
  rank: RANK.STAFF,                       // RANK.STAFF
}
,
  {
  id: 4,                                  // 1~4 등 고유 숫자
  name: '최개발',                          // '최개발'
  email: 'dev.lead@baton.local',          // 'dev.lead@baton.local'
  password: 'password1',                  // mock 로그인 비교용 임시 값 (실제 값은 팀 시드 문서 확인)
  department: DEPT.DEV,                         // DEPT.DEV  ← 문자열 직접 쓰지 말고 상수 사용
  rank: RANK.LEADER,                      // RANK.LEADER
}

]

export const documents = [
  {
  id: 1,                    // 고유 숫자
  title: '사내 복리후생 안내',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-01T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '김영업', rank: RANK.STAFF },
  minRank: RANK.STAFF,      // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.ALL,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 2,                    // 고유 숫자
  title: '비품 구매 절차',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-02T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '이개발', rank: RANK.STAFF },
  minRank: RANK.STAFF,      // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.ALL,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 3,                    // 고유 숫자
  title: '거래처 관리 매뉴얼',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-03T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '김영업', rank: RANK.STAFF },
  minRank: RANK.STAFF,      // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.SALES,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 4,                    // 고유 숫자
  title: '월간 영업보고 작성법',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-04T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '김영업', rank: RANK.STAFF },
  minRank: RANK.STAFF,      // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.SALES,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 5,                    // 고유 숫자
  title: '거래처 계약 단가표',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-05T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '박영업', rank: RANK.LEADER },
  minRank: RANK.LEADER,     // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.SALES,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 6,                    // 고유 숫자
  title: '영업팀 성과평가 기준',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-06T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '박영업', rank: RANK.LEADER },
  minRank: RANK.LEADER,     // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.SALES,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 7,                    // 고유 숫자
  title: '개발 환경 세팅 가이드',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-07T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '이개발', rank: RANK.STAFF },
  minRank: RANK.STAFF,      // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.DEV,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 8,                    // 고유 숫자
  title: '배포 절차서',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-08T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '이개발', rank: RANK.STAFF },
  minRank: RANK.STAFF,      // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.DEV,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 9,                    // 고유 숫자
  title: '서버 접근권한 관리 정책',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-09T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '최개발', rank: RANK.LEADER },
  minRank: RANK.LEADER,     // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.DEV,         // DEPT.ALL / SALES / DEV  (부서 열)
},
  {
  id: 10,                   // 고유 숫자
  title: '외주 개발 계약 현황',           // 문서명 (문서함 '문서명' 열)
  createdAt: '2026-10-10T10:00:00+09:00',       // 작성일 (문서함 '작성일' 열)
  author: { name: '최개발', rank: RANK.LEADER },
  minRank: RANK.LEADER,     // RANK.STAFF 또는 RANK.LEADER (직급 열)
  status: 'READY',
  department: DEPT.DEV,         // DEPT.ALL / SALES / DEV  (부서 열)
}
]
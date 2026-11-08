export const RANK = {
  STAFF: "STAFF",    // API설계서의 사원 직급 문자열
  LEADER: "LEADER",   // 팀장 직급 문자열
}

export const DEPT = {
  ALL: "ALL",      // 전체
  SALES: "SALES",    // 영업
  DEV: "DEV",      // 개발
}

export const RANK_LABEL = {
  [RANK.STAFF]: '사원',
  [RANK.LEADER]: '팀장',
}
export const DEPT_LABEL = {
  [DEPT.ALL]: '전체',
  [DEPT.SALES]: '영업팀',
  [DEPT.DEV]: '개발팀',
}
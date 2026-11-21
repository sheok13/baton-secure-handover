import { RANK, DEPT } from '../constants/index.js'

export function canRead(doc, me) {
  const deptOk = doc.department === DEPT.ALL || doc.department === me.department;
  const rankOk = doc.minRank === RANK.STAFF || me.rank === RANK.LEADER;
  return deptOk && rankOk;
}
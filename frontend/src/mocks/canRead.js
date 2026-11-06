import { RANK, DEPT } from '../constants/index.js'

export function canRead(doc, me) {
  const deptOk = doc.dept === DEPT.ALL || doc.dept === me.dept;
  const rankOk = doc.minRank === RANK.STAFF || me.rank === RANK.LEADER;
  return deptOk && rankOk;
}
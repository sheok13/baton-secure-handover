#!/bin/bash
# 시드 문서 자동 업로드 스크립트 (피드백 반영 및 기존 로직 개선)
# 사용: 저장소 루트에서 SEED_PASSWORD=... ./infra/seed/upload_samples.sh

set -euo pipefail

echo "🚀 시드 문서 자동 업로드 스크립트 시작..."

# [수정] 기존 하드코딩 패스워드 제거 및 팀장님 피드백 반영 (환경변수로만 수신)
: "${SEED_PASSWORD:?SEED_PASSWORD 환경변수를 설정하세요 (보안 정책)}"
BASE_URL="${BASE_URL:-http://localhost:8080/api}"
DOCX="application/vnd.openxmlformats-officedocument.wordprocessingml.document"

# [수정] DB 설계서 6장 기준 매핑 (번호|업로드 계정|열람 부서|공개 직급)
MAP=(
  "1|sales.staff@baton.local|ALL|STAFF"
  "2|dev.staff@baton.local|ALL|STAFF"
  "3|sales.staff@baton.local|SALES|STAFF"
  "4|sales.staff@baton.local|SALES|STAFF"
  "5|sales.lead@baton.local|SALES|LEADER"
  "6|sales.lead@baton.local|SALES|LEADER"
  "7|dev.staff@baton.local|DEV|STAFF"
  "8|dev.staff@baton.local|DEV|STAFF"
  "9|dev.lead@baton.local|DEV|LEADER"
  "10|dev.lead@baton.local|DEV|LEADER"
)

# 기존 로그인 로직을 함수화하여 가독성 개선
get_token() {
  curl -s -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$SEED_PASSWORD\"}" \
    | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4
}

for row in "${MAP[@]}"; do
  IFS='|' read -r no email dept rank <<< "$row"
  
  # 파일명 파싱 (1.사내복리후생안내.docx 등)
  file=$(ls infra/seed/docs/"${no}".*.docx 2>/dev/null || true)
  if [ -z "$file" ] || [ ! -f "$file" ]; then
      echo "⚠️ 파일을 찾을 수 없습니다: 번호 $no"
      continue
  fi
  
  title=$(basename "$file" .docx | sed -E 's/^[0-9]+\.//; s/_/ /g')
  
  token=$(get_token "$email")
  if [ -z "$token" ]; then 
      echo "❌ 로그인 실패: $email"
      continue
  fi
  
  echo -n "👉 로그인 성공: $email | [$no] $title ($dept·$rank) 업로드 중... "
  
  # [수정] 400 에러 방지를 위해 필수 필드(department, minRank, title) 추가
  curl -s -o /dev/null -w "HTTP 상태 코드: %{http_code}\n" -X POST "$BASE_URL/documents" \
    -H "Authorization: Bearer $token" \
    -F "file=@$file;type=$DOCX" \
    -F "department=$dept" \
    -F "minRank=$rank" \
    -F "title=$title"
done

echo "🎉 업로드 쉘 스크립트 완료!"
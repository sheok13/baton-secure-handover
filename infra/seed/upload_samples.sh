#!/bin/bash
USER_PASSWORD=${BATON_TEST_PASSWORD:-"1234"}
BASE_URL="http://localhost:8080/api"
echo "🚀 시드 문서 자동 업로드 스크립트 시작..."
EMAILS=("sales_user@baton.com" "sales_leader@baton.com" "dev_user@baton.com" "dev_leader@baton.com")

for EMAIL in "${EMAILS[@]}"; do
    echo "로그인 시도: $EMAIL"
    TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\", \"password\":\"$USER_PASSWORD\"}" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    if [ -z "$TOKEN" ]; then continue; fi
    prefix=$(echo $EMAIL | cut -d'@' -f1)
    for FILE in infra/seed/docs/${prefix}*.docx; do
        [ -f "$FILE" ] && curl -s -X POST "$BASE_URL/documents" -H "Authorization: Bearer $TOKEN" -F "file=@$FILE"
    done
done
echo "🎉 업로드 쉘 스크립트 완료!"

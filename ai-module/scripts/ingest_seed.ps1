# 예시 문서 10개를 AI 모듈에 다시 수집 (임베딩 모델을 바꾼 뒤 재적재용)
# 실행 (ai-module 폴더, 서버 켜진 상태): Invoke-Expression (Get-Content -Raw -Encoding UTF8 .\scripts\ingest_seed.ps1)
# 같은 docId로 다시 올리면 기존 조각을 지우고 새로 저장하므로 여러 번 실행해도 안전하다.

$KEY = ((Select-String '^AI_INTERNAL_API_KEY=' .env).Line.Split('=', 2)[1] -split '\s')[0]
$DIR = '..\infra\seed\docs'

# 파일 이름에 들어 있는 낱말 → [부서, 공개 직급] (DB 설계서 6장 표)
$MAP = [ordered]@{
  '복리후생' = @('ALL', 'STAFF');   '비품'     = @('ALL', 'STAFF')
  '매뉴얼'   = @('SALES', 'STAFF'); '영업보고' = @('SALES', 'STAFF')
  '단가표'   = @('SALES', 'LEADER'); '성과평가' = @('SALES', 'LEADER')
  '세팅'     = @('DEV', 'STAFF');   '배포'     = @('DEV', 'STAFF')
  '접근권한' = @('DEV', 'LEADER');  '외주'     = @('DEV', 'LEADER')
}

$health = curl.exe -s http://localhost:8000/health
if ($health -notmatch 'ok') { Write-Host "서버가 꺼져 있거나 저장소를 열 수 없습니다: $health"; return }

$i = 0
Get-ChildItem $DIR -File | Where-Object { $_.Extension -in '.pdf', '.docx' } | Sort-Object Name | ForEach-Object {
  $f = $_
  $hit = $MAP.Keys | Where-Object { $f.BaseName -like "*$_*" } | Select-Object -First 1
  if (-not $hit) { Write-Host "건너뜀 (권한 매핑 없음): $($f.Name)"; return }
  $i++
  # docId = 파일 이름 앞 번호 (예: 10.외주_... → 10), 제목은 번호를 뗀 이름
  if ($f.BaseName -notmatch '^(\d+)\.') { Write-Host "건너뜀 (앞 번호 없음): $($f.Name)"; return }
  $id = [int]$Matches[1]
  $title = $f.BaseName -replace '^\d+\.', ''
  $dept, $rank = $MAP[$hit]
  $res = curl.exe -s -X POST http://localhost:8000/internal/ingest -H "X-Internal-Api-Key: $KEY" `
    -F "file=@$($f.FullName)" -F "docId=$id" -F "title=$title" -F "department=$dept" -F "minRank=$rank"
  Write-Host "$id  $($f.Name)  [$dept/$rank]  -> $res"
}
Write-Host "완료: $i 개 업로드 시도"
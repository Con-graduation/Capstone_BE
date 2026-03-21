#!/usr/bin/env bash
set -euo pipefail

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

LOG_FILE="/home/ec2-user/app.log"

echo -e "${YELLOW}Daily Guitar 백엔드 로그${NC}"
echo "================================"

if [ ! -f "$LOG_FILE" ]; then
    echo -e "${YELLOW}⚠️  로그 파일이 없습니다: $LOG_FILE${NC}"
    echo "서버가 아직 실행되지 않았을 수 있습니다."
    exit 1
fi

echo -e "${GREEN}로그 파일: $LOG_FILE${NC}"
echo -e "${YELLOW}로그 출력 시작... (Ctrl+C로 중지)${NC}"
echo "================================"
tail -f "$LOG_FILE"


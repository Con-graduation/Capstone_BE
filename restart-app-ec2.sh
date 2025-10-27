#!/usr/bin/env bash
set -euo pipefail

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

APP_JAR="/home/ec2-user/app.jar"
WORK_DIR="/home/ec2-user"
LOG_FILE="$WORK_DIR/app.log"
PID_FILE="$WORK_DIR/app.pid"
ENV_FILE="$WORK_DIR/.env"

echo -e "${YELLOW}Daily Guitar 백엔드 재시작${NC}"
echo "================================"

# 1. 기존 프로세스 종료
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo -e "${YELLOW}기존 프로세스 종료 중... (PID: $PID)${NC}"
        kill "$PID"
        sleep 2
        
        # 강제 종료 시도
        if ps -p "$PID" > /dev/null 2>&1; then
            echo -e "${RED}강제 종료 중...${NC}"
            kill -9 "$PID"
            sleep 1
        fi
    fi
    rm -f "$PID_FILE"
fi

# 2. JAR 파일 확인
if [ ! -f "$APP_JAR" ]; then
    echo -e "${RED}❌ JAR 파일을 찾을 수 없습니다: $APP_JAR${NC}"
    exit 1
fi

# 3. 환경변수 로드 및 실행
cd "$WORK_DIR"

if [ -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}.env 파일에서 환경변수 로드 중...${NC}"
    nohup env $(cat "$ENV_FILE" | xargs) \
        java -jar "$APP_JAR" --server.port=8080 \
        > "$LOG_FILE" 2>&1 &
else
    echo -e "${YELLOW}.env 파일 없음, 기본 환경변수로 실행...${NC}"
    nohup java -jar "$APP_JAR" --server.port=8080 \
        > "$LOG_FILE" 2>&1 &
fi

NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"

echo -e "${GREEN}✅ 애플리케이션 시작 완료 (PID: $NEW_PID)${NC}"

# 4. 로그 실시간 출력
echo -e "\n${YELLOW}로그 출력 시작... (Ctrl+C로 중지)${NC}"
echo "================================"
tail -f "$LOG_FILE"

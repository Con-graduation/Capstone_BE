#!/usr/bin/env bash
set -euo pipefail

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

MCP_JAR="/home/ec2-user/mcp-server.jar"
WORK_DIR="/home/ec2-user"
LOG_FILE="$WORK_DIR/mcp-server.log"
PID_FILE="$WORK_DIR/mcp-server.pid"
ENV_FILE="$WORK_DIR/.env"

echo -e "${YELLOW}Daily Guitar MCP 서버 재시작${NC}"
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
if [ ! -f "$MCP_JAR" ]; then
    echo -e "${RED}❌ JAR 파일을 찾을 수 없습니다: $MCP_JAR${NC}"
    exit 1
fi

# 3. 환경변수 로드 및 실행
cd "$WORK_DIR"

if [ -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}.env 파일에서 환경변수 로드 중...${NC}"
    nohup env $(cat "$ENV_FILE" | xargs) \
        java -jar "$MCP_JAR" --server.port=8081 \
        > "$LOG_FILE" 2>&1 &
else
    echo -e "${YELLOW}.env 파일 없음, 기본 환경변수로 실행...${NC}"
    nohup java -jar "$MCP_JAR" --server.port=8081 \
        > "$LOG_FILE" 2>&1 &
fi

NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"

# 프로세스가 실제로 실행 중인지 확인
sleep 1
if ! ps -p "$NEW_PID" > /dev/null 2>&1; then
    echo -e "${RED}❌ 프로세스가 시작되지 않았습니다. 로그를 확인하세요.${NC}"
    if [ -f "$LOG_FILE" ]; then
        echo -e "\n${YELLOW}로그 내용:${NC}"
        cat "$LOG_FILE"
    fi
    exit 1
fi

echo -e "${GREEN}✅ MCP 서버 시작 완료 (PID: $NEW_PID)${NC}"
echo -e "${GREEN}포트: 8081${NC}"

# 4. 로그 파일이 생성될 때까지 대기
echo -e "\n${YELLOW}로그 파일 생성 대기 중...${NC}"
for i in {1..10}; do
    if [ -f "$LOG_FILE" ]; then
        break
    fi
    sleep 0.5
done

if [ ! -f "$LOG_FILE" ]; then
    echo -e "${YELLOW}⚠️  로그 파일이 아직 생성되지 않았습니다. 프로세스 상태를 확인하세요.${NC}"
    echo -e "${YELLOW}프로세스 확인: ps -p $NEW_PID${NC}"
    exit 0
fi

# 5. 로그 실시간 출력
echo -e "\n${YELLOW}로그 출력 시작... (Ctrl+C로 중지)${NC}"
echo "================================"
tail -f "$LOG_FILE"


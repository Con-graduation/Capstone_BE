# MCP 서버 환경 변수

백엔드와 **같은 `back/.env` 파일**을 사용합니다 (`application.properties`의 `dotenv.directory=..`).

1. 저장소 루트에서 `cp .env.example .env`
2. `.env`에 `DB_*`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 등 입력
3. `cd mcp-server && ./gradlew bootRun`

전체 설명은 루트 **`ENV.md`**, **`/.env.example`** 참고.

# 환경 변수 / `.env`

1. `cp .env.example .env`
2. `.env`에 DB, JWT, Google(MCP), AWS 등 설정
3. **한때 저장소에 노출된 시크릿은 반드시 콘솔에서 폐기 후 새로 발급**

## 로드 방식

- **메인 백엔드**: `back/.env` (spring-dotenv)
- **MCP 서버**: 동일 파일 (`mcp-server`의 `dotenv.directory=..`)

CI·서버에서는 `.env` 없이 동일 이름의 환경 변수만 설정해도 됩니다 (`dotenv.ignore-if-missing=true`).

변수 목록은 `.env.example` 참고.

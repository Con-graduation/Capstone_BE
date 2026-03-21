# 환경 변수 / `.env`

백엔드와 MCP는 **루트 `back/.env` 한 파일**로 맞춥니다.

1. `cp .env.example .env`
2. DB, JWT, **Google OAuth**(메인·MCP 공통), OpenAI, YouTube, AWS 등 입력
3. 예전에 GitHub에 올라갔던 키는 **반드시 재발급**

## 변수 이름 정리

| 용도 | 변수 |
|------|------|
| 메인 JWT / MCP JWT | `JWT_SECRET` |
| 메인 `spring.security.oauth2` + MCP `google.client.*` | `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_REDIRECT_URI` |
| OpenAI | `OPENAI_API_KEY` |
| YouTube Data API | `YOUTUBE_API_KEY` |

## 로드

- **메인**: `back/.env` (spring-dotenv)
- **MCP**: 동일 파일 (`mcp-server`의 `dotenv.directory=..`)

CI·서버는 `.env` 없이 동일 이름의 환경 변수만 설정해도 됩니다 (`dotenv.ignore-if-missing=true`).

상세 체크리스트는 `mcp-server/ENV_SETUP.md` 참고.

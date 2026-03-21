# MCP Server 환경 변수

백엔드와 **같은 `back/.env`** 를 씁니다 (`dotenv.directory=..`).

## 필수 (.env 또는 export)

| 변수 | 설명 |
|------|------|
| `DB_URL`, `DB_USER`, `DB_PASS` | PostgreSQL (백엔드와 동일 DB) |
| `JWT_SECRET` | 백엔드와 **동일** |
| `GOOGLE_OAUTH_CLIENT_ID` | 백엔드 `spring.security.oauth2` 와 동일 |
| `GOOGLE_OAUTH_CLIENT_SECRET` | 위와 쌍 |

선택: `JWT_EXPIRATION` (기본 `8640000000`), `MCP_SERVER_PORT` (기본 `8081`).

## 설정

```bash
# 저장소 루트(back/)에서
cp .env.example .env
# .env 편집 후
cd mcp-server && ./gradlew bootRun
```

Spring은 **spring-dotenv**로 `back/.env`를 읽습니다.  
전체 변수 목록·메인 앱 설정은 루트 **`ENV.md`**, **`/.env.example`** 참고.

## 실행 확인

```bash
cd mcp-server
./gradlew bootRun
```

## 체크리스트

- [ ] `back/.env` 존재
- [ ] `JWT_SECRET` 이 메인 백엔드와 동일
- [ ] Google OAuth 값이 메인 백엔드와 동일

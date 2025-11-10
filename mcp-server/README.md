# MCP 서버 프로젝트 구조

## 개요
MCP 서버는 백엔드와 **같은 PostgreSQL DB에 직접 접근**합니다.

## 구조
```
mcp-server/
├── build.gradle                    # Gradle 설정
├── src/main/
│   ├── java/
│   │   └── dailyGuitar/capstone/mcp/
│   │       ├── MCPServerApplication.java    # 메인 클래스
│   │       ├── server/
│   │       │   ├── MCPService.java          # DB 직접 접근 서비스
│   │       │   └── MCPProtocolHandler.java # MCP 프로토콜 처리 (구현 필요)
│   │       ├── entity/                      # 엔티티 (백엔드와 동일)
│   │       ├── repository/                  # 리포지토리 (백엔드와 동일)
│   │       └── config/
│   │           └── MCPConfig.java
│   └── resources/
│       └── application.properties            # DB 연결 설정
```

## DB 접근 방식
- **같은 PostgreSQL DB 사용**
- **Spring Data JPA로 직접 접근**
- **백엔드와 동일한 엔티티/리포지토리 사용**

## 다음 단계
1. 엔티티와 리포지토리 복사
2. MCP 프로토콜 핸들러 구현
3. 구글 캘린더 연동 서비스 추가


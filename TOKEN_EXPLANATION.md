# 토큰 종류 및 획득 방법 설명

## 토큰 종류 비교

### 1. JWT 토큰 (우리 앱의 인증 토큰)
- **발급자**: 우리 백엔드 서버
- **용도**: 우리 앱의 API 인증 (사용자 로그인 인증)
- **형식**: JWT (JSON Web Token)
- **예시**: `eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYW0yNzQ2IiwiaWF0IjoxNzYzMTAzNDA2LCJleHAiOjE3NzE3NDM0MDZ9.Xx07rJk6rgDdpbAwLIIQ0IRUJjXtQAEKiyC5zmqDiOg`
- **획득 방법**: 
  ```javascript
  // 우리 앱 로그인 시
  POST /api/auth/login
  {
    "usernameOrEmail": "sam2746",
    "password": "password123"
  }
  // 응답: { "token": "eyJhbGciOiJIUzI1NiJ9..." }
  ```
- **저장 위치**: 프론트엔드 로컬 스토리지
- **사용**: 모든 백엔드 API 호출 시 `Authorization: Bearer {jwtToken}` 헤더에 포함

### 2. 구글 Access Token (구글 API 호출용)
- **발급자**: Google OAuth 서버
- **용도**: 구글 API 호출 (구글 캘린더 API 등)
- **형식**: OAuth 2.0 Access Token (일반 문자열)
- **예시**: `ya29.a0AfH6SMC...` (JWT 형식이 아님)
- **유효 기간**: 1시간
- **획득 방법**: 
  ```javascript
  // 프론트엔드에서 구글 로그인
  const tokenClient = google.accounts.oauth2.initTokenClient({
    client_id: 'YOUR_GOOGLE_CLIENT_ID',
    scope: 'openid profile email https://www.googleapis.com/auth/calendar',
    callback: (response) => {
      const accessToken = response.access_token; // ✅ 프론트엔드에서 획득 가능
      const refreshToken = response.refresh_token; // ✅ 프론트엔드에서 획득 가능
      const expiresIn = response.expires_in; // 3600초 (1시간)
    }
  });
  
  tokenClient.requestAccessToken({ prompt: 'consent' });
  ```
- **저장 위치**: 
  - 프론트엔드: 임시 저장 가능 (로컬 스토리지, 보안 주의)
  - 백엔드: DB에 저장 (영구)
- **사용**: 구글 캘린더 API 호출 시 `Authorization: Bearer {googleAccessToken}` 헤더에 포함

### 3. 구글 Refresh Token (구글 Access Token 갱신용)
- **발급자**: Google OAuth 서버
- **용도**: Access Token 만료 시 새 Access Token 발급
- **형식**: OAuth 2.0 Refresh Token (일반 문자열)
- **예시**: `1//0g...` (JWT 형식이 아님)
- **유효 기간**: 만료되지 않음 (사용자가 해제할 때까지)
- **획득 방법**: 
  ```javascript
  // 프론트엔드에서 구글 로그인 시
  // ⚠️ 중요: prompt: 'consent'를 사용해야 refresh_token을 받을 수 있음
  tokenClient.requestAccessToken({ prompt: 'consent' });
  
  callback: (response) => {
    const refreshToken = response.refresh_token; // ✅ 프론트엔드에서 획득 가능
  }
  ```
- **저장 위치**: 
  - 프론트엔드: ❌ 저장하지 않음 (보안상 위험)
  - 백엔드: DB에 저장 (영구, 절대 노출 금지)
- **사용**: 백엔드에서만 사용 (Access Token 갱신 시)

## 토큰 관계도

```
┌─────────────────────────────────────────────────────────┐
│                    사용자                                │
└─────────────────────────────────────────────────────────┘
         │
         │ 1. 우리 앱 로그인
         ▼
┌─────────────────────────────────────────────────────────┐
│  JWT 토큰 (우리 앱 인증)                                 │
│  - 발급: 우리 백엔드                                     │
│  - 용도: 우리 API 인증                                   │
│  - 획득: 프론트엔드 ✅                                    │
│  - 저장: 프론트엔드 로컬 스토리지                        │
└─────────────────────────────────────────────────────────┘
         │
         │ 2. 구글 로그인 (JWT 토큰과 함께)
         ▼
┌─────────────────────────────────────────────────────────┐
│  구글 Access Token + Refresh Token                       │
│  - 발급: Google OAuth 서버                               │
│  - 용도: 구글 API 호출                                   │
│  - 획득: 프론트엔드 ✅                                    │
│  - 저장: 백엔드 DB (프론트엔드는 임시 저장 가능)          │
└─────────────────────────────────────────────────────────┘
         │
         │ 3. 구글 캘린더 API 호출
         ▼
┌─────────────────────────────────────────────────────────┐
│  구글 캘린더 API                                         │
│  - Authorization: Bearer {구글 Access Token}            │
└─────────────────────────────────────────────────────────┘
```

## 프론트엔드에서 구글 토큰 획득 예시

```javascript
// 1. Google Identity Services 스크립트 로드
<script src="https://accounts.google.com/gsi/client" async defer></script>

<script>
// 2. OAuth2 Token Client 초기화
const tokenClient = google.accounts.oauth2.initTokenClient({
  client_id: '175199577563-5sqn62s05094gvb1jldb3g9mq5vg32hm.apps.googleusercontent.com',
  scope: 'openid profile email https://www.googleapis.com/auth/calendar',
  callback: handleTokenResponse,
});

// 3. 토큰 요청 (사용자가 버튼 클릭 시)
function requestGoogleToken() {
  // ⚠️ 중요: prompt: 'consent'를 사용해야 refresh_token을 받을 수 있음
  tokenClient.requestAccessToken({ prompt: 'consent' });
}

// 4. 토큰 응답 처리
function handleTokenResponse(response) {
  if (response.error) {
    console.error('구글 인증 오류:', response.error);
    return;
  }
  
  // ✅ 프론트엔드에서 획득 가능한 정보
  const accessToken = response.access_token;      // 구글 Access Token
  const refreshToken = response.refresh_token;    // 구글 Refresh Token (prompt: 'consent' 사용 시)
  const expiresIn = response.expires_in;          // 만료 시간 (초)
  const scope = response.scope;                   // 승인된 권한 범위
  
  console.log('구글 토큰 획득 성공!');
  console.log('Access Token:', accessToken);
  console.log('Refresh Token:', refreshToken);
  console.log('만료 시간:', expiresIn, '초');
  
  // 5. 백엔드에 토큰 저장
  const jwtToken = localStorage.getItem('jwtToken'); // 우리 앱의 JWT 토큰
  fetch('https://www.dailyguitar.site/api/auth/google/connect', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${jwtToken}` // 우리 앱의 JWT 토큰 사용
    },
    body: JSON.stringify({
      accessToken: accessToken,      // 구글 Access Token
      refreshToken: refreshToken,    // 구글 Refresh Token
      expiresIn: expiresIn.toString()
    })
  });
}
</script>

<button onclick="requestGoogleToken()">구글 캘린더 연동</button>
```

## 요약

| 토큰 종류 | 발급자 | 획득 위치 | 저장 위치 | 용도 |
|---------|--------|----------|----------|------|
| **JWT 토큰** | 우리 백엔드 | 프론트엔드 ✅ | 프론트엔드 로컬 스토리지 | 우리 앱 API 인증 |
| **구글 Access Token** | Google OAuth | 프론트엔드 ✅ | 백엔드 DB + 프론트엔드 임시 | 구글 API 호출 |
| **구글 Refresh Token** | Google OAuth | 프론트엔드 ✅ | 백엔드 DB만 (보안) | Access Token 갱신 |

## 중요 사항

1. **JWT 토큰 ≠ 구글 토큰**
   - JWT 토큰: 우리 앱 인증용
   - 구글 토큰: 구글 API 호출용

2. **프론트엔드에서 모두 획득 가능**
   - JWT 토큰: 우리 앱 로그인 시
   - 구글 Access Token: 구글 로그인 시
   - 구글 Refresh Token: 구글 로그인 시 (prompt: 'consent' 필요)

3. **Refresh Token 보안**
   - 프론트엔드에 저장하지 않음 (보안상 위험)
   - 백엔드 DB에만 저장
   - 백엔드에서만 사용 (Access Token 갱신)

4. **토큰 사용 흐름**
   ```
   프론트엔드 → JWT 토큰 → 백엔드 API 호출
   프론트엔드 → 구글 Access Token → 구글 캘린더 API 호출
   백엔드 → 구글 Refresh Token → 구글 Access Token 갱신
   ```


package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 정적 페이지 서빙을 위한 컨트롤러
 */
@RestController
public class StaticController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 메인 페이지 (맞춤 곡 추천 페이지) - JWT 토큰 인증 필요
     */
    @GetMapping("/")
    public ResponseEntity<String> index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // JWT 토큰 검증
        String token = extractTokenFromRequest(request);
        
        if (token == null || !isValidToken(token)) {
            // 토큰이 없거나 유효하지 않으면 로그인 페이지로 리다이렉트
            response.sendRedirect("/login.html");
            return ResponseEntity.status(302).body("Redirecting to login");
        }
        
        // 토큰이 유효하면 맞춤 곡 추천 페이지 서빙
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(getRecommendationsPage());
    }

    /**
     * 맞춤 곡 추천 페이지
     */
    @GetMapping("/recommendations")
    public ResponseEntity<String> recommendations(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return index(request, response);
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰 유효성 검증
     */
    private boolean isValidToken(String token) {
        try {
            // 토큰이 유효한 형식인지 확인
            jwtTokenProvider.extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 맞춤 곡 추천 페이지 HTML 반환
     */
    private String getRecommendationsPage() {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>DailyGuitar - 맞춤 곡 추천</title>
                <link rel="stylesheet" href="/css/style.css">
                <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;700&display=swap" rel="stylesheet">
            </head>
            <body>
                <div class="container">
                    <!-- 헤더 -->
                    <header class="header">
                        <div class="header-content">
                            <div class="home-icon">🏠</div>
                            <h1 class="app-title">DailyGuitar</h1>
                            <div class="profile-icon" onclick="logout()">👤</div>
                        </div>
                    </header>

                    <!-- 메인 콘텐츠 -->
                    <main class="main-content">
                        <div class="recommendation-section">
                            <h2 class="section-title">맞춤 곡 추천</h2>
                            
                            <!-- 추천 곡 목록 -->
                            <div class="song-list">
                                <div class="song-item">
                                    <div class="album-cover">
                                        <img src="/images/green-day-last-night.svg" alt="Green Day - Last Night On Earth" class="cover-image">
                                    </div>
                                    <div class="song-info">
                                        <h3 class="song-title">Last Night On Earth</h3>
                                        <p class="artist-name">Green Day</p>
                                    </div>
                                </div>

                                <div class="song-item">
                                    <div class="album-cover">
                                        <img src="/images/acdc-back-in-black.svg" alt="AC/DC - Back in Black" class="cover-image">
                                    </div>
                                    <div class="song-info">
                                        <h3 class="song-title">Back in Black</h3>
                                        <p class="artist-name">AC/DC</p>
                                    </div>
                                </div>

                                <div class="song-item">
                                    <div class="album-cover">
                                        <img src="/images/green-day-american-idiot.svg" alt="Green Day - American Idiot" class="cover-image">
                                    </div>
                                    <div class="song-info">
                                        <h3 class="song-title">American Idiot</h3>
                                        <p class="artist-name">Green Day</p>
                                    </div>
                                </div>

                                <div class="song-item">
                                    <div class="album-cover">
                                        <img src="/images/rhcp-by-the-way.svg" alt="Red Hot Chili Peppers - By the Way" class="cover-image">
                                    </div>
                                    <div class="song-info">
                                        <h3 class="song-title">By the Way</h3>
                                        <p class="artist-name">Red Hot Chili Peppers</p>
                                    </div>
                                </div>
                            </div>

                            <!-- 추가 액션 버튼 -->
                            <div class="action-buttons">
                                <button class="refresh-btn" onclick="refreshRecommendations()">
                                    <span class="refresh-icon">🔄</span>
                                    또 다른 곡
                                </button>
                            </div>

                            <!-- 수동 설정 섹션 -->
                            <div class="manual-settings">
                                <h3 class="settings-title">곡 추천 수동 설정</h3>
                                <p class="settings-description">원하는 코드와 BPM을 선택해주세요!</p>
                            </div>
                        </div>
                    </main>

                    <!-- 하단 네비게이션 -->
                    <nav class="bottom-nav">
                        <div class="nav-item">
                            <div class="nav-icon">🎵⭐</div>
                        </div>
                        <div class="nav-item">
                            <div class="nav-icon">🎸</div>
                        </div>
                        <div class="nav-item">
                            <div class="nav-icon">▶️</div>
                        </div>
                        <div class="nav-item">
                            <div class="nav-icon">📊🎵</div>
                        </div>
                        <div class="nav-item">
                            <div class="nav-icon">⚙️</div>
                        </div>
                    </nav>
                </div>

                <script>
                    // JWT 토큰 관리
                    const token = localStorage.getItem('jwt_token');
                    
                    if (!token) {
                        window.location.href = '/login.html';
                    }

                    // 로그아웃 함수
                    function logout() {
                        localStorage.removeItem('jwt_token');
                        window.location.href = '/login.html';
                    }

                    // 추천 곡 새로고침
                    function refreshRecommendations() {
                        const refreshBtn = document.querySelector('.refresh-btn');
                        refreshBtn.innerHTML = '<span class="refresh-icon">🔄</span> 로딩 중...';
                        
                        setTimeout(() => {
                            refreshBtn.innerHTML = '<span class="refresh-icon">🔄</span> 또 다른 곡';
                            showNotification('새로운 곡을 추천해드렸습니다!');
                        }, 1500);
                    }

                    // 알림 표시
                    function showNotification(message) {
                        const notification = document.createElement('div');
                        notification.className = 'notification';
                        notification.textContent = message;
                        notification.style.cssText = `
                            position: fixed;
                            top: 80px;
                            left: 50%;
                            transform: translateX(-50%);
                            background: rgba(0, 0, 0, 0.8);
                            color: white;
                            padding: 12px 20px;
                            border-radius: 25px;
                            font-size: 14px;
                            z-index: 1000;
                        `;
                        document.body.appendChild(notification);
                        
                        setTimeout(() => {
                            notification.remove();
                        }, 3000);
                    }
                </script>
            </body>
            </html>
            """;
    }
}

// DailyGuitar 앱 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // DOM 요소들
    const refreshBtn = document.querySelector('.refresh-btn');
    const songItems = document.querySelectorAll('.song-item');
    const navItems = document.querySelectorAll('.nav-item');

    // 곡 데이터 (실제로는 API에서 가져올 데이터)
    const songData = [
        {
            title: "Last Night On Earth",
            artist: "Green Day",
            cover: "/images/green-day-last-night.jpg"
        },
        {
            title: "Back in Black",
            artist: "AC/DC",
            cover: "/images/acdc-back-in-black.jpg"
        },
        {
            title: "American Idiot",
            artist: "Green Day",
            cover: "/images/green-day-american-idiot.jpg"
        },
        {
            title: "By the Way",
            artist: "Red Hot Chili Peppers",
            cover: "/images/rhcp-by-the-way.jpg"
        }
    ];

    // 대체 곡 데이터 (새로고침용)
    const alternativeSongs = [
        {
            title: "Wonderwall",
            artist: "Oasis",
            cover: "/images/oasis-wonderwall.jpg"
        },
        {
            title: "Hotel California",
            artist: "Eagles",
            cover: "/images/eagles-hotel-california.jpg"
        },
        {
            title: "Sweet Child O' Mine",
            artist: "Guns N' Roses",
            cover: "/images/gnr-sweet-child.jpg"
        },
        {
            title: "Stairway to Heaven",
            artist: "Led Zeppelin",
            cover: "/images/led-zeppelin-stairway.jpg"
        }
    ];

    // 곡 목록 업데이트 함수
    function updateSongList(songs) {
        songItems.forEach((item, index) => {
            if (songs[index]) {
                const titleElement = item.querySelector('.song-title');
                const artistElement = item.querySelector('.artist-name');
                const coverElement = item.querySelector('.cover-image');
                
                titleElement.textContent = songs[index].title;
                artistElement.textContent = songs[index].artist;
                coverElement.alt = `${songs[index].artist} - ${songs[index].title}`;
            }
        });
    }

    // 새로고침 버튼 클릭 이벤트
    refreshBtn.addEventListener('click', function() {
        // 로딩 상태 표시
        refreshBtn.classList.add('loading');
        refreshBtn.innerHTML = '<span class="refresh-icon">🔄</span> 로딩 중...';
        
        // 1.5초 후에 새로운 곡으로 교체
        setTimeout(() => {
            updateSongList(alternativeSongs);
            
            // 버튼 원래 상태로 복원
            refreshBtn.classList.remove('loading');
            refreshBtn.innerHTML = '<span class="refresh-icon">🔄</span> 또 다른 곡';
            
            // 성공 메시지 표시 (선택사항)
            showNotification('새로운 곡을 추천해드렸습니다!');
        }, 1500);
    });

    // 곡 아이템 클릭 이벤트
    songItems.forEach((item, index) => {
        item.addEventListener('click', function() {
            const title = this.querySelector('.song-title').textContent;
            const artist = this.querySelector('.artist-name').textContent;
            
            // 곡 선택 시 시각적 피드백
            this.style.backgroundColor = '#e3f2fd';
            setTimeout(() => {
                this.style.backgroundColor = '';
            }, 200);
            
            showNotification(`${artist} - ${title}을 선택했습니다!`);
        });
    });

    // 네비게이션 아이템 클릭 이벤트
    navItems.forEach((item, index) => {
        item.addEventListener('click', function() {
            // 모든 네비게이션 아이템에서 active 클래스 제거
            navItems.forEach(nav => nav.classList.remove('active'));
            
            // 클릭된 아이템에 active 클래스 추가
            this.classList.add('active');
            
            // 각 네비게이션 기능 구현 (현재는 알림만)
            const navLabels = ['추천', '기타', '연주', '통계', '설정'];
            showNotification(`${navLabels[index]} 메뉴를 선택했습니다!`);
        });
    });

    // 알림 표시 함수
    function showNotification(message) {
        // 기존 알림 제거
        const existingNotification = document.querySelector('.notification');
        if (existingNotification) {
            existingNotification.remove();
        }
        
        // 새 알림 생성
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
            animation: slideDown 0.3s ease;
        `;
        
        // CSS 애니메이션 추가
        const style = document.createElement('style');
        style.textContent = `
            @keyframes slideDown {
                from {
                    opacity: 0;
                    transform: translateX(-50%) translateY(-20px);
                }
                to {
                    opacity: 1;
                    transform: translateX(-50%) translateY(0);
                }
            }
        `;
        document.head.appendChild(style);
        
        document.body.appendChild(notification);
        
        // 3초 후 알림 제거
        setTimeout(() => {
            notification.style.animation = 'slideDown 0.3s ease reverse';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.remove();
                }
            }, 300);
        }, 3000);
    }

    // 스크롤 시 헤더 그림자 효과
    let lastScrollTop = 0;
    window.addEventListener('scroll', function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const header = document.querySelector('.header');
        
        if (scrollTop > lastScrollTop && scrollTop > 100) {
            // 아래로 스크롤
            header.style.boxShadow = '0 2px 12px rgba(0, 0, 0, 0.15)';
        } else {
            // 위로 스크롤
            header.style.boxShadow = '0 2px 12px rgba(0, 0, 0, 0.08)';
        }
        
        lastScrollTop = scrollTop;
    });

    // 터치 이벤트 지원 (모바일)
    let touchStartY = 0;
    let touchEndY = 0;

    document.addEventListener('touchstart', function(e) {
        touchStartY = e.changedTouches[0].screenY;
    });

    document.addEventListener('touchend', function(e) {
        touchEndY = e.changedTouches[0].screenY;
        handleSwipe();
    });

    function handleSwipe() {
        const swipeThreshold = 50;
        const diff = touchStartY - touchEndY;
        
        if (Math.abs(diff) > swipeThreshold) {
            if (diff > 0) {
                // 위로 스와이프 (새로고침)
                refreshBtn.click();
            }
        }
    }

    // 초기화 완료 메시지
    console.log('DailyGuitar 앱이 성공적으로 로드되었습니다!');
});

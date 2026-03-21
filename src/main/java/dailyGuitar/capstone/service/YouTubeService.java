package dailyGuitar.capstone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

/**
 * YouTube Data API v3를 사용하여 곡 검색 및 링크 검증 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeService {
    
    private final RestTemplate restTemplate;
    
    @Value("${youtube.api.key}")
    private String youtubeApiKey;
    
    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    
    /**
     * 곡 제목과 아티스트로 YouTube 공식 링크 검색
     * 
     * @param songTitle 곡 제목
     * @param artist 아티스트 이름
     * @return YouTube 비디오 ID (없으면 null)
     */
    public String searchOfficialVideo(String songTitle, String artist) {
        if (youtubeApiKey == null || youtubeApiKey.isEmpty()) {
            log.warn("YouTube API 키가 설정되지 않았습니다.");
            return null;
        }
        
        try {
            // 여러 검색 쿼리 변형 시도
            String[] searchQueries = {
                String.format("%s %s", artist, songTitle),  // "Queen I Want to Break Free"
                String.format("%s - %s", artist, songTitle), // "Queen - I Want to Break Free"
                String.format("%s %s official", artist, songTitle), // "Queen I Want to Break Free official"
                String.format("%s - %s official", artist, songTitle) // "Queen - I Want to Break Free official"
            };
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 각 검색 쿼리를 순서대로 시도
            for (String searchQuery : searchQueries) {
                try {
                    // URL 인코딩 (특수문자 처리)
                    String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
                    
                    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                            .queryParam("part", "snippet")
                            .queryParam("q", encodedQuery)
                            .queryParam("type", "video")
                            .queryParam("maxResults", 10)
                            .queryParam("key", youtubeApiKey)
                            .queryParam("order", "relevance");
                    
                    String requestUrl = builder.toUriString();
                    log.debug("YouTube API 요청: {}", requestUrl.replace(youtubeApiKey, "***"));
                    
                    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.GET,
                            entity,
                            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                    );
                    
                    Map<String, Object> responseBody = response.getBody();
                    if (responseBody == null) {
                        log.debug("YouTube API 응답이 null입니다: query={}", searchQuery);
                        continue; // 다음 쿼리 시도
                    }
                    
                    // 에러 확인
                    if (responseBody.containsKey("error")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                        log.error("YouTube API 에러: query={}, error={}", searchQuery, error);
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) responseBody.get("items");
                    if (items == null || items.isEmpty()) {
                        log.debug("YouTube 검색 결과가 없습니다: query={}", searchQuery);
                        continue; // 다음 쿼리 시도
                    }
                    
                    log.debug("YouTube 검색 성공: query={}, results={}", searchQuery, items.size());
                    
                    // 아티스트 이름을 소문자로 변환하여 비교 (채널명 매칭용)
                    String artistLower = artist.toLowerCase();
                    
                    // 검색 결과 중에서 공식 채널 비디오를 우선 선택
                    for (Map<String, Object> item : items) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                        if (snippet == null) continue;
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> id = (Map<String, Object>) item.get("id");
                        if (id == null || !id.containsKey("videoId")) continue;
                        
                        String videoId = (String) id.get("videoId");
                        String channelTitle = (String) snippet.get("channelTitle");
                        String title = (String) snippet.get("title");
                        
                        if (channelTitle == null) continue;
                        
                        // 채널명에 아티스트 이름이 포함되어 있으면 우선 선택
                        if (channelTitle.toLowerCase().contains(artistLower)) {
                            log.debug("YouTube 공식 채널 비디오 찾음: videoId={}, channel={}, title={}", 
                                    videoId, channelTitle, title);
                            return videoId;
                        }
                    }
                    
                    // 공식 채널을 찾지 못하면 첫 번째 결과 반환
                    Map<String, Object> firstItem = items.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> id = (Map<String, Object>) firstItem.get("id");
                    if (id != null && id.containsKey("videoId")) {
                        String videoId = (String) id.get("videoId");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> snippet = (Map<String, Object>) firstItem.get("snippet");
                        String channelTitle = snippet != null ? (String) snippet.get("channelTitle") : null;
                        log.debug("YouTube 비디오 찾음 (첫 번째 결과): videoId={}, channel={}", videoId, channelTitle);
                        return videoId;
                    }
                } catch (Exception e) {
                    log.warn("YouTube 검색 쿼리 실패: query={}, error={}", searchQuery, e.getMessage());
                    continue; // 다음 쿼리 시도
                }
            }
            
            // 모든 검색 쿼리 시도 실패
            log.warn("모든 YouTube 검색 쿼리 시도 실패: artist={}, songTitle={}", artist, songTitle);
            return null;
        } catch (Exception e) {
            log.error("YouTube API 호출 중 오류 발생: songTitle={}, artist={}", songTitle, artist, e);
            return null;
        }
    }
    
    /**
     * 비디오 ID로 YouTube 링크 생성
     * 
     * @param videoId YouTube 비디오 ID
     * @return YouTube 링크
     */
    public String buildYouTubeLink(String videoId) {
        if (videoId == null || videoId.isEmpty()) {
            return null;
        }
        return "https://www.youtube.com/watch?v=" + videoId;
    }
    
    /**
     * 곡 제목과 아티스트로 YouTube 링크 검색 및 생성
     * 
     * @param songTitle 곡 제목
     * @param artist 아티스트 이름
     * @return YouTube 링크 (없으면 null)
     */
    public String findOfficialYouTubeLink(String songTitle, String artist) {
        String videoId = searchOfficialVideo(songTitle, artist);
        if (videoId == null) {
            return null;
        }
        return buildYouTubeLink(videoId);
    }
}


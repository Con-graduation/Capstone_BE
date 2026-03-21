package dailyGuitar.capstone.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String remoteAddr = extractClientIp(request);
        String origin = headerOrNull(request, "Origin");
        String userAgent = headerOrNull(request, "User-Agent");
        String referer = headerOrNull(request, "Referer");

        long startNs = System.nanoTime();
        // pre-handle log
        log.info("요청 수신: {} {}{} - IP: {}, Origin: {}, User-Agent: {}, Referer: {}",
                method,
                uri,
                query == null ? "" : ("?" + query),
                remoteAddr,
                origin,
                userAgent,
                referer);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long tookMs = (System.nanoTime() - startNs) / 1_000_000;
            int status = response.getStatus();
            log.info("요청 완료: {} {} - {} {}ms", method, uri, status, tookMs);
        }
    }

    private String headerOrNull(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return v == null ? "null" : v;
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // XFF may contain multiple IPs, first is client
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp;
        return req.getRemoteAddr();
    }
}



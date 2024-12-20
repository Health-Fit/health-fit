package com.heemin.ws.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heemin.ws.model.service.auth.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.PatternMatchUtils;

public class JwtAuthorizationFilter implements Filter {

    private final String[] whiteUris = {"/", "/api/auth/login/**", "/api/videos", "/api/auth/access-token",
            "/api/categories", "/api/videos/ex"};
    private final JwtProvider jwtProvider = new JwtProvider();
    private final ObjectMapper objectMapper;
    private final RedisTemplate redisTemplate;

    public JwtAuthorizationFilter(ObjectMapper objectMapper, RedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        if (httpServletRequest.getMethod().toUpperCase().equals("OPTIONS")) { // OPTIONS 메서드는 안전하므로 별도의 인증 필요 없음 (필터 종료)
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.setStatus(HttpStatus.OK.value());

            // CORS 헤더 추가
            response.setHeader("Access-Control-Allow-Origin", "*"); // 모든 출처 허용 (혹은 특정 출처만 허용)
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");

            return;
        }

        if (isWhiteUri(httpServletRequest.getRequestURI())) { // whiteUri라면, 다음 필터로 이동시킴
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (httpServletRequest.getRequestURI().contains("/api/chats/") && httpServletRequest.getMethod().toUpperCase()
                .equals("GET")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!isContainToken(httpServletRequest)) { // 요청에 토큰이 없다면, 401반환 후 필터 체인 중단
            sendNoAuthResponse(servletResponse);
            return;
        }

        String token = getToken(httpServletRequest);

        if (isBlackToken(token)) { // 로그아웃한 accessToken이면, 401반환 후 중단
            sendNoAuthResponse(servletResponse);
        }

        Claims claims = jwtProvider.getClaims(token);
        httpServletRequest.setAttribute("memberId", claims.get("memberId"));
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isBlackToken(String accessToken) {
        return redisTemplate.hasKey(accessToken);
    }

    private boolean isWhiteUri(String uri) {
        return PatternMatchUtils.simpleMatch(whiteUris, uri);
    }

    private boolean isContainToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization"); // Header에서 Authorization 값 꺼냄
        return authorization != null && authorization.startsWith(
                "Bearer "); // 'Bearer' 뒤에 토큰이 붙기 때문에 'Bearer'로 시작하는지 확임함
    }

    private void sendNoAuthResponse(ServletResponse response) throws IOException { // Json 응답 생성
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ((HttpServletResponse) response).setStatus(HttpStatus.UNAUTHORIZED.value());

        response.getWriter().write(
                objectMapper.writeValueAsString("토큰이 없어 접근할 수 없습니다.")
        );
    }

    private String getToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization.substring(7).replace("\"", ""); // 'Bearer ' 뒤 7번째 인덱스부터 토큰 시작함
    }
}

package com.heemin.ws.model.service.auth;

import com.heemin.ws.model.dao.AuthDao;
import com.heemin.ws.model.dao.ExerciseVideoDao;
import com.heemin.ws.model.dao.MemberDao;
import com.heemin.ws.model.dto.Response;
import com.heemin.ws.model.dto.auth.Jwt;
import com.heemin.ws.model.dto.auth.LoginResponse;
import com.heemin.ws.model.dto.auth.OauthToken;
import com.heemin.ws.model.dto.member.Member;
import com.heemin.ws.model.dto.video.ExerciseVideo;
import com.heemin.ws.model.service.auth.requester.OauthRequester;
import com.heemin.ws.model.service.auth.requester.OauthRequesterFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthDao authDao;
    private final MemberDao memberDao;
    private final ExerciseVideoDao exerciseVideoDao;
    private final JwtProvider jwtProvider;
    private final OauthRequesterFactory oauthRequesterFactory;

    public AuthService(AuthDao authDao, MemberDao memberDao, ExerciseVideoDao exerciseVideoDao, JwtProvider jwtProvider,
                       OauthRequesterFactory oauthRequesterFactory) {
        this.authDao = authDao;
        this.memberDao = memberDao;
        this.exerciseVideoDao = exerciseVideoDao;
        this.jwtProvider = jwtProvider;
        this.oauthRequesterFactory = oauthRequesterFactory;
    }

    @Transactional
    public Response login(String type, String code) {
        // type 이름으로 Reuqester 가져오기
        OauthRequester requester = oauthRequesterFactory.getRequester(type);

        // 소셜 서버에 접근해서 토큰과 회원 정보 가져오기
        OauthToken oauthToken = requester.getToken(code);
        Member memberInfo = requester.getMemberInfo(oauthToken);

        // db에 없는 회원이면 회원가입
        // 회원가입이라면, 운동영상 정보 가져오기 (선호 영상 선택)
        Member member = getMember(memberInfo.getEmail());
        List<ExerciseVideo> videos = null;

        if (member == null) {
            member = memberInfo;
            member.setId(signup(member));
            videos = exerciseVideoDao.selectExample();
        }

        // 자체 Jwt 토큰 만들고 refreshToken 저장
        Jwt jwt = jwtProvider.createJwt(Map.of("memberId", member.getId()));
        authDao.insertRefreshToken(member.getId(), jwt.getRefreshToken());

        // 회원 가입인 경우, 운동 영상 정보 + Jwt 반환
        return new Response(new LoginResponse(jwt, videos), 200);
    }

    private Member getMember(String email) {
        return memberDao.selectByEmail(email);
    }

    private int signup(Member member) {
        member.setRegDate(LocalDate.now());
        return memberDao.insert(member);
    }
}

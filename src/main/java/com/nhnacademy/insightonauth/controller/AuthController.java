package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.controller.support.LoginResponder;
import com.nhnacademy.insightonauth.controller.support.OauthWebSupport;
import com.nhnacademy.insightonauth.dto.auth.*;
import com.nhnacademy.insightonauth.dto.oauth.OauthLoginRequest;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.exception.auth.InvalidCredentialsException;
import com.nhnacademy.insightonauth.exception.auth.InvalidRefreshTokenException;
import com.nhnacademy.insightonauth.exception.auth.RefreshTokenNotFoundException;
import com.nhnacademy.insightonauth.exception.oauth.OauthAlreadyLinkedException;
import com.nhnacademy.insightonauth.exception.oauth.OauthLinkedToOtherAccountException;
import com.nhnacademy.insightonauth.exception.signup.EmailAlreadyRegisteredException;
import com.nhnacademy.insightonauth.exception.user.ManagerGroupExistsException;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.MyPageService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 정상 인증 흐름을 담당하는 컨트롤러.
 * 회원가입(이메일 인증 포함), 로그인/로그아웃, 소셜 로그인, 액세스 토큰 재발급을 처리한다.
 * 계정을 잃어버린 뒤의 복구 흐름은 {@link AccountController}가 담당한다.
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String X_USER_ID = "X-User-Id";
    private static final String PENDING_RESTORE_STATUS = "PENDING_RESTORE";
    private static final String LOGIN_OAUTH_ERROR_PATH = "/login?oauthError=1";
    private static final String MYPAGE_LINK_ERROR_PATH = "/mypage?linkError=1";

    private final UserAuthenticationService userAuthenticationService;
    private final UserEmailService userEmailService;
    private final UserManagementService userManagementService;
    private final JwtProvider jwtProvider;
    private final LoginResponder loginResponder;
    private final OauthWebSupport oauthWebSupport;
    private final MyPageService myPageService;

    // 회원가입용 이메일 인증 코드 발송 요청 (재전송 쿨다운·횟수 제한 적용)
    @PostMapping("/email/verify-request")
    public ResponseEntity<Void> sendEmailVerify(@RequestBody @Valid EmailVerifyRequest emailVerifyRequest) {
        userEmailService.emailVerifyRequest(emailVerifyRequest.email());
        return ResponseEntity.noContent().build();
    }

    // 이메일 인증 코드 확인 → 성공 시 가입 요청에 쓸 인증 토큰 발급
    @PostMapping("/email/verify-confirm")
    public ResponseEntity<EmailVerifyConfirmResponse> emailCodeConfirm(
            @RequestBody @Valid EmailVerifyConfirmRequest emailVerifyConfirmRequest) {
        String verificationToken =
                userEmailService.emailVerifyConfirm(emailVerifyConfirmRequest.email(), emailVerifyConfirmRequest.code());

        return ResponseEntity.ok(new EmailVerifyConfirmResponse(verificationToken));
    }

    // 이메일 중복 여부 확인 (가입 폼 실시간 검사용)
    @PostMapping("/check-email")
    public ResponseEntity<EmailAvailableResponse> checkEmailAvailable(
            @RequestBody @Valid EmailAvailableRequest emailAvailableRequest) {
        boolean available = userManagementService.checkEmailAvailable(emailAvailableRequest.email());

        return ResponseEntity.ok(new EmailAvailableResponse(available));
    }

    // 회원가입 (verify-confirm에서 받은 인증 토큰 필요, 역할은 MEMBER 고정)
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> doSignup(
            @RequestBody @Valid UserSignupRequest userSignupRequest) {
        UserSignupResponse userSignupResponse =
                userManagementService.createUser(userSignupRequest.email(),
                        userSignupRequest.password(),
                        userSignupRequest.userName(),
                        userSignupRequest.phoneNumber(),
                        Role.MEMBER,
                        userSignupRequest.token());

        return ResponseEntity.status(HttpStatus.CREATED).body(userSignupResponse);
    }

    // 일반 회원 로그인 — access 토큰은 본문, refresh 토큰은 HttpOnly 쿠키. 관리자 계정은 이 경로로 로그인 불가
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> doLogin(
            @RequestBody @Valid UserLoginRequest userLoginRequest) {
        UserLoginResult result = userAuthenticationService.login(
                userLoginRequest.email(), userLoginRequest.password());

        // 탈퇴 후 복구 가능 기간 내 계정 — 로그인 성공이 아니라 "복구 안내" 상태.
        // accessToken 이 없으므로 admin 체크 대상이 아니다.
        if (PENDING_RESTORE_STATUS.equals(result.status())) {
            return ResponseEntity.ok(UserLoginResponse.pendingRestore());
        }

        if (jwtProvider.hasAdminRole(result.accessToken())) {
            // 계정 열거 방지: 관리자 / 비번 오류 / 없는 계정 모두 동일 메시지
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return loginResponder.success(result);
    }

    // 로그아웃 — refresh 토큰 삭제 + 현재 access 토큰 블랙리스트 등록
    @PostMapping("/logout")
    public ResponseEntity<Void> doLogout(
            @RequestHeader(name = X_USER_ID) Long userId,
            @RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        userAuthenticationService.logout(userId, accessToken);
        return ResponseEntity.noContent().build();
    }

    // 소셜 로그인 (provider: google, github ...) — 연동 계정이 없으면 신규 가입 처리.
    // 응답 규약은 일반 로그인과 동일 (access 토큰은 바디, refresh 토큰은 HttpOnly 쿠키).
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<UserLoginResponse> oauthLogin(
            @PathVariable String provider,
            @RequestBody @Valid OauthLoginRequest request) {

        UserLoginResult result = userAuthenticationService.oauthLogin(provider, request.code());

        // 탈퇴 후 복구 가능 기간 내 계정 — 로그인 성공이 아니라 "복구 안내" 상태.
        if (PENDING_RESTORE_STATUS.equals(result.status())) {
            return ResponseEntity.ok(UserLoginResponse.pendingRestore());
        }

        return loginResponder.success(result);
    }

    // ── 브라우저 주도 소셜 로그인 ───────────────────────────────────────────────
    //   프론트 버튼 → GET /oauth/authorize/{provider} → provider 동의 화면
    //   → provider 가 GET /oauth/callback?code=..&state=.. 로 되돌림
    //   → code 교환·토큰 발급까지 auth 가 끝내고 accessToken/refreshToken 쿠키를 심은 뒤
    //     프론트로 302 (성공: /oauth/complete, 복구대기: /reactivate, 실패: /login?oauthError=..)

    @GetMapping("/oauth/authorize/{provider}")
    public ResponseEntity<Void> oauthAuthorize(@PathVariable String provider) {
        if (!oauthWebSupport.supports(provider)) {
            return redirect(oauthWebSupport.front(LOGIN_OAUTH_ERROR_PATH), null);
        }
        String state = UUID.randomUUID() + "." + provider;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, oauthWebSupport.stateCookie(state).toString())
                .header(HttpHeaders.LOCATION, oauthWebSupport.authorizeUrl(provider, state))
                .build();
    }

    // 마이페이지 소셜 계정 연동 — 로그인과 동일한 브라우저 주도 왕복.
    //   프론트 "연동" 버튼 → 여기 → provider 동의 화면 → GET /oauth/callback (state 에 .link 표시)
    //   → auth 가 code 교환·연동까지 끝내고 프론트 /mypage 로 302
    //   mergeWith 가 있으면(다른 계정에 이미 연동된 걸 병합하겠다는 확인) provider 재인증을 한 번 더 거치게 해서
    //   지금도 그 소셜 계정을 실제로 통제하는지 재확인한 뒤 콜백에서 연동 대신 병합을 수행한다.
    @GetMapping("/oauth/link/authorize/{provider}")
    public ResponseEntity<Void> oauthLinkAuthorize(
            @PathVariable String provider,
            @RequestParam(required = false) Long mergeWith,
            @CookieValue(value = "accessToken", required = false) String accessToken) {

        Long userId = parseUserId(accessToken);
        if (!oauthWebSupport.supports(provider) || userId == null) {
            return redirect(oauthWebSupport.front(MYPAGE_LINK_ERROR_PATH), null);
        }
        // 연동 대상 userId 를 state 에 실어 둔다. state 는 콜백에서 서버가 심은 oauthState 쿠키와
        // 통째로 대조되므로(위조 불가), 콜백은 이 값을 "연동을 시작한 유저"로 신뢰할 수 있다.
        String state = UUID.randomUUID() + "." + provider + ".link." + userId
                + (mergeWith != null ? ".merge." + mergeWith : "");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, oauthWebSupport.stateCookie(state).toString())
                .header(HttpHeaders.LOCATION, oauthWebSupport.authorizeUrl(provider, state))
                .build();
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(value = OauthWebSupport.STATE_COOKIE, required = false) String expectedState,
            @CookieValue(value = "accessToken", required = false) String accessToken) {

        // state 쿠키는 1회용 — 검증 결과와 무관하게 폐기
        String expiredState = oauthWebSupport.expiredStateCookie().toString();

        // 연동 왕복이었는지는 서버가 심은 state 쿠키로 판단한다(쿼리 state 는 검증 전이라 신뢰 불가).
        // provider 취소(error=access_denied)·code 누락도 연동이었으면 마이페이지로 돌려보낸다.
        // state 형식: 로그인 "<uuid>.<provider>", 연동 "<uuid>.<provider>.link.<userId>" — nonce·provider 에 점 없음.
        boolean link = expectedState != null && expectedState.contains(".link");
        boolean stateMatched = state != null && state.equals(expectedState);

        if (error != null || code == null || expectedState == null || !stateMatched) {
            String errorRedirect = link ? MYPAGE_LINK_ERROR_PATH : LOGIN_OAUTH_ERROR_PATH;
            log.warn("[OAuth] 콜백 검증 실패: error={}, link={}, stateMatched={}", error, link, stateMatched);
            return redirect(oauthWebSupport.front(errorRedirect), expiredState);
        }

        String[] parts = state.split("\\.");                 // [nonce, provider, "link"?, userId?]
        String provider = parts[1];

        if (link) {
            return handleLinkCallback(parts, provider, code, accessToken, expiredState);
        }
        return handleLoginCallback(provider, code, expiredState);
    }

    // state 의 userId(연동 시작 유저) 와 콜백 시점 accessToken 쿠키의 userId(현재 로그인 유저) 가
    // 같아야만 연동을 진행한다. 동의 화면을 띄운 뒤 다른 탭에서 계정을 바꾸면 여기서 걸린다.
    private ResponseEntity<Void> handleLinkCallback(
            String[] parts, String provider, String code, String accessToken, String expiredState) {
        Long stateUserId = parts.length > 3 ? toUserId(parts[3]) : null;
        Long currentUserId = parseUserId(accessToken);
        if (stateUserId == null || !stateUserId.equals(currentUserId)) {
            return redirect(oauthWebSupport.front("/mypage?linkError=auth"), expiredState);
        }
        // state: "<nonce>.<provider>.link.<userId>" 뒤에 병합 확인 왕복이면 ".merge.<secondaryUserId>" 가 더 붙는다.
        Long mergeSecondaryUserId = parts.length > 5 && "merge".equals(parts[4]) ? toUserId(parts[5]) : null;
        try {
            if (mergeSecondaryUserId != null) {
                myPageService.confirmMerge(stateUserId, mergeSecondaryUserId, provider, code);
                return redirect(oauthWebSupport.front("/mypage?merged=1"), expiredState);
            }
            myPageService.linkOauth(stateUserId, provider, code);
            return redirect(oauthWebSupport.front("/mypage?linked=1"), expiredState);
        } catch (OauthAlreadyLinkedException e) {
            return redirect(oauthWebSupport.front("/mypage?linkError=already"), expiredState);
        } catch (OauthLinkedToOtherAccountException e) {
            return redirect(oauthWebSupport.front(
                    "/mypage?linkError=other_account&conflictUserId=" + e.getConflictingUserId()
                            + "&provider=" + provider), expiredState);
        } catch (ManagerGroupExistsException e) {
            return redirect(oauthWebSupport.front("/mypage?linkError=manager_account"), expiredState);
        } catch (RuntimeException e) {
            log.warn("[OAuth] 연동/병합 실패: {}", e.getMessage());
            return redirect(oauthWebSupport.front(MYPAGE_LINK_ERROR_PATH), expiredState);
        }
    }

    private ResponseEntity<Void> handleLoginCallback(String provider, String code, String expiredState) {
        try {
            UserLoginResult result = userAuthenticationService.oauthLogin(provider, code);

            if (PENDING_RESTORE_STATUS.equals(result.status())) {
                return redirect(oauthWebSupport.front("/reactivate"), expiredState);
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, expiredState)
                    .header(HttpHeaders.SET_COOKIE, loginResponder.accessTokenCookie(result.accessToken()).toString())
                    .header(HttpHeaders.SET_COOKIE, loginResponder.refreshTokenCookie(result.refreshToken()).toString())
                    .header(HttpHeaders.LOCATION, oauthWebSupport.front("/oauth/complete"))
                    .build();

        } catch (EmailAlreadyRegisteredException e) {
            return redirect(oauthWebSupport.front("/login?oauthError=email_taken"), expiredState);
        } catch (RuntimeException e) {
            log.warn("[OAuth] 소셜 로그인 실패: {}", e.getMessage());
            return redirect(oauthWebSupport.front(LOGIN_OAUTH_ERROR_PATH), expiredState);
        }
    }

    private ResponseEntity<Void> redirect(String location, String extraSetCookie) {
        ResponseEntity.BodyBuilder b = ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location);
        if (extraSetCookie != null) {
            b.header(HttpHeaders.SET_COOKIE, extraSetCookie);
        }
        return b.build();
    }

    /** accessToken 쿠키에서 userId. 없거나 서명·만료 검증 실패면 null. */
    private Long parseUserId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(jwtProvider.parse(accessToken).getSubject());
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }

    /** state 에 실린 userId 문자열 → Long. 숫자가 아니면 null. */
    private Long toUserId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // refresh 토큰 쿠키로 새 access 토큰 재발급 (쿠키 없음/서명·만료 오류면 예외)
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue("refreshToken") String refreshToken) {
        // 1) refreshToken 쿠키 없음
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenNotFoundException("refreshToken이 없습니다.");
        }

        // 2) 파싱(서명·만료 검증) 실패
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.parse(refreshToken).getSubject());
        } catch (JwtException | NumberFormatException e) {
            throw new InvalidRefreshTokenException("유효하지 않은 refreshToken입니다.");
        }

        // 3) refresh 처리
        TokenRefreshResponse tokenRefreshResponse =
                userAuthenticationService.refresh(userId, refreshToken);
        return ResponseEntity.ok(tokenRefreshResponse);
    }
}

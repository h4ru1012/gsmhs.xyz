# gsmhs.xyz — GSM 학생 서브도메인 신청 사이트

Spring Boot 4.1 + Thymeleaf + DataGSM OAuth SDK 기반.

## 실행 방법

1. JDK 25 설치 확인: `java -version`
2. 환경변수 설정:
   ```bash
   export DATAGSM_CLIENT_ID="774e66a4-0115-44b4-9637-a65b798f7b6e"
   export DATAGSM_CLIENT_SECRET="발급받은-시크릿"
   # 로컬 테스트용 (DataGSM 클라이언트에 이 URI도 등록해야 함)
   export DATAGSM_REDIRECT_URI="http://localhost:8080/oauth/callback"
   ```
3. 실행: `./gradlew bootRun` (gradle wrapper가 없으면 `gradle bootRun` 또는 IntelliJ에서 실행)
4. http://localhost:8080 접속 → "DataGSM으로 로그인" 클릭

## 구조

```
src/main/java/xyz/gsmhs/
├── GsmhsApplication.java          # 진입점
├── config/
│   ├── DataGsmOAuthConfig.java    # SDK 클라이언트 Bean 등록 + 설정 프로퍼티
│   └── CloudflareConfig.java      # Cloudflare 설정 프로퍼티
├── controller/
│   ├── AuthController.java        # /login, /oauth/callback, /logout
│   ├── HomeController.java        # / (허브 페이지: 로그인 상태 + 등록된 프로젝트 목록)
│   └── ProjectController.java     # 서브도메인 신청/수정/삭제
├── domain/Project.java            # 등록된 프로젝트 엔티티
├── repository/ProjectRepository.java
├── service/CloudflareDnsService.java # CNAME 레코드 생성/갱신/삭제
└── dto/SessionUser.java           # 세션에 저장하는 학생 정보

src/main/resources/
├── application.yml                # 설정 (secret은 환경변수로, DB는 SQLite)
└── templates/
    ├── index.html                 # 허브 페이지
    └── register.html              # 서브도메인 신청 폼
```

## OAuth 흐름

1. `/login` → state + PKCE code_verifier 생성해 세션 저장 → DataGSM authorize URL로 리다이렉트
2. DataGSM 로그인 완료 → `/oauth/callback?code=...&state=...`
3. state 검증(CSRF 방지) → SDK로 code + verifier 토큰 교환 → userinfo 조회
4. 학생 정보(이름/학년/반/번호/학번/학과)를 세션에 저장 → `/`로 리다이렉트

## 서브도메인 신청 / 수정 / 삭제

- 로그인한 학생만 `/projects/new`에서 신청 가능 (서브도메인, CNAME 대상 호스트, 소개, 깃허브 링크 입력)
- 신청 즉시 승인 절차 없이 메인 페이지(허브)에 공개됨
- 본인이 등록한 프로젝트만 수정/삭제 가능 (DataGSM 계정 이메일로 소유자 판별)
- 데이터는 SQLite(`gsmhs.db`, 프로젝트 루트에 생성됨)에 저장 — `GSMHS_DB_PATH` 환경변수로 경로 변경 가능
- 서브도메인은 영문 소문자/숫자/하이픈만 허용, 중복 및 예약어(www, oauth, api 등) 체크

## Cloudflare DNS 연동

신청/수정/삭제 시 `{subdomain}.gsmhs.xyz` CNAME 레코드를 Cloudflare API로 자동 관리한다.

- 환경변수 `CF_API_TOKEN`, `CF_ZONE_ID` 둘 다 설정돼 있어야 동작. **비어 있으면 DNS 연동 없이
  DB에만 저장**(로컬 개발 모드) — 이때 등록된 프로젝트는 나중에 수정 시점에 레코드가 생성됨.
- 토큰 발급: Cloudflare 대시보드 → My Profile → API Tokens → Create Token →
  "Edit zone DNS" 템플릿 → Zone Resources에서 gsmhs.xyz 지정.
- Zone ID: 대시보드에서 gsmhs.xyz 도메인 선택 → Overview 우측 하단 "Zone ID" 복사.
- CNAME 방식이므로 학생은 자기 호스팅(GitHub Pages, Vercel 등)의 커스텀 도메인 설정에
  `{subdomain}.gsmhs.xyz`를 등록해야 실제 연결이 완성됨.
- `cloudflare.proxied`(기본 false)를 켜면 Cloudflare 프록시 경유. GitHub Pages는 false 권장.

## 주의사항

- **scope 파라미터 생략**: 클라이언트 등록 화면에는 `self_read`, HTTP 문서에는 `self:read`로
  표기가 달라 아예 scope를 안 보내고 서버 기본값을 쓰도록 함 (`datagsm.oauth.scope` 비워두면 됨).
- **로컬 테스트**: DataGSM 클라이언트 설정에 `http://localhost:8080/oauth/callback`을
  리다이렉트 URI로 추가 등록해야 로컬에서 로그인 테스트 가능.
- **secret 관리**: 절대 코드/깃에 커밋하지 말고 환경변수로만 주입 (DataGSM secret, CF 토큰 모두).

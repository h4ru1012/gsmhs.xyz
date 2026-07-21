# gsmhs.xyz — GSM 학생 서브도메인 등록 사이트

Spring Boot 4.1 + Thymeleaf + DataGSM OAuth SDK 기반.

## 실행 방법

1. JDK 25 설치 확인: `java -version`
2. 환경변수 설정: `.env.example`을 `.env`로 복사하고 값 채우기
   ```bash
   cp .env.example .env
   # DATAGSM_CLIENT_SECRET 등 실제 값 입력
   ```
   `.env`는 git에 커밋되지 않으며, OS 환경변수가 있으면 그쪽이 우선한다.
3. 실행: `./gradlew bootRun` (또는 IntelliJ에서 실행 — 별도 환경변수 설정 불필요)
4. https://gsmhs.xyz (로컬 실행 시 http://localhost:8080) 접속 → "DataGSM으로 로그인" 클릭

## 구조

```
src/main/java/xyz/gsmhs/
├── GsmhsApplication.java          # 진입점
├── config/
│   ├── DataGsmOAuthConfig.java    # SDK 클라이언트 Bean 등록 + 설정 프로퍼티
│   └── CloudflareConfig.java      # Cloudflare 설정 프로퍼티
├── controller/
│   ├── AuthController.java        # /login, /oauth/callback, /logout(POST)
│   ├── HomeController.java        # / (허브), /tos, /privacy
│   ├── ProjectController.java     # 서브도메인 등록/수정/삭제
│   └── AdminController.java       # /admin (관리자 대시보드)
├── domain/
│   ├── Project.java               # 등록된 프로젝트 엔티티
│   ├── AppUser.java               # 로그인 기록 (관리자 페이지용)
│   └── Major.java                 # 학과 코드 → 한글 학과명 매핑
├── repository/                    # ProjectRepository, AppUserRepository
├── service/
│   ├── CloudflareDnsService.java  # CNAME 레코드 생성/갱신/삭제
│   └── AdminService.java          # ADMIN_EMAILS 기반 관리자 판별
└── dto/SessionUser.java           # 세션에 저장하는 학생 정보

src/main/resources/
├── application.yml                # 설정 (secret은 환경변수로, DB는 SQLite)
├── static/                        # favicon.svg, robots.txt
└── templates/                     # index, register, admin, tos, privacy, error
```

## OAuth 흐름

1. `/login` → state + PKCE code_verifier 생성해 세션 저장 → DataGSM authorize URL로 리다이렉트
2. DataGSM 로그인 완료 → `/oauth/callback?code=...&state=...`
3. state 검증(CSRF 방지) → SDK로 code + verifier 토큰 교환 → userinfo 조회
4. 학생 정보(이름/학년/반/번호/학번/학과)를 세션에 저장 → `/`로 리다이렉트

## 서브도메인 등록 / 수정 / 삭제

- 로그인한 학생만 `/projects/new`에서 등록 가능 (서브도메인, CNAME 대상 호스트, 소개, 깃허브 링크 입력)
- 등록 즉시 승인 절차 없이 메인 페이지(허브)에 공개됨
- 본인이 등록한 프로젝트만 수정/삭제 가능 (DataGSM 계정 이메일로 소유자 판별)
- 데이터는 SQLite(`gsmhs.xyz.db`, 프로젝트 루트에 생성됨)에 저장 — `GSMHS_DB_PATH` 환경변수로 경로 변경 가능
- 서브도메인은 영문 소문자/숫자/하이픈만 허용, 중복 및 예약어(www, oauth, api 등) 체크

## Cloudflare DNS 연동

등록/수정/삭제 시 `{subdomain}.gsmhs.xyz` CNAME 레코드를 Cloudflare API로 자동 관리한다.

- 환경변수 `CF_API_TOKEN`, `CF_ZONE_ID` 둘 다 설정돼 있어야 동작. **비어 있으면 DNS 연동 없이
  DB에만 저장**(로컬 개발 모드) — 이때 등록된 프로젝트는 나중에 수정 시점에 레코드가 생성됨.
- 토큰 발급: Cloudflare 대시보드 → My Profile → API Tokens → Create Token →
  "Edit zone DNS" 템플릿 → Zone Resources에서 gsmhs.xyz 지정.
- Zone ID: 대시보드에서 gsmhs.xyz 도메인 선택 → Overview 우측 하단 "Zone ID" 복사.
- CNAME 방식이므로 학생은 자기 호스팅(GitHub Pages, Vercel 등)의 커스텀 도메인 설정에
  `{subdomain}.gsmhs.xyz`를 등록해야 실제 연결이 완성됨.
- `cloudflare.proxied`(기본 false)를 켜면 Cloudflare 프록시 경유. GitHub Pages는 false 권장.

## 프로덕션 배포 (NAS)

NAS의 JDK 25(temurin) 컨테이너에서 직접 빌드·실행하는 방식.

```bash
git clone https://github.com/h4ru1012/gsmhs.xyz.git && cd gsmhs.xyz
cp .env.example .env   # 실제 값 입력 (DATAGSM_CLIENT_ID/SECRET, CF_API_TOKEN/ZONE_ID, ADMIN_EMAILS)
./gradlew build -x test
java -jar build/libs/gsmhs.xyz-0.0.1-SNAPSHOT.jar
```

- ⚠️ **반드시 저장소 루트에서 실행**할 것. `.env`는 `spring.config.import: optional:file:.env`로
  **현재 작업 디렉터리 기준**으로 읽힌다. 다른 디렉터리에서 실행하면 `.env`가 무시되어
  로그인 URL에 `${DATAGSM_CLIENT_ID}`가 그대로 노출된다.
  다른 위치에서 실행해야 한다면 `export $(grep -v '^#' /path/to/.env | xargs)` 후 실행.
- 업데이트 배포: `git pull && ./gradlew build -x test` 후 프로세스 재시작
  (`server.shutdown: graceful`이라 처리 중 요청은 마치고 종료됨).
- DB(`gsmhs.xyz.db`)는 실행 디렉터리에 생성됨 — 백업 대상. `GSMHS_DB_PATH`로 경로 지정 가능.

### 리버스 프록시 (nginx)

라즈베리파이 nginx → `앱호스트:8080`. HTTPS 종료는 nginx(Let's Encrypt 와일드카드)에서 하고,
앱은 `forward-headers-strategy: framework`로 `X-Forwarded-*`를 신뢰하므로 nginx 설정에 다음이 필요:

```nginx
proxy_set_header Host $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

### 배포 전 체크리스트

- [ ] `./gradlew build -x test` 통과
- [ ] `.env` 실제 값 채움 (secret은 git에 절대 커밋 금지)
- [ ] 로컬 http 테스트 시에만 `SESSION_COOKIE_SECURE=false` — **운영에선 제거**(기본 true)
- [ ] DataGSM 클라이언트에 운영 리다이렉트 URI(`https://gsmhs.xyz/oauth/callback`) 등록 확인
- [ ] 로그인 → 등록 → 수정 → 삭제 → 관리자 페이지 스모크 테스트
- [ ] `https://gsmhs.xyz/tos`, `/privacy`, `/robots.txt`, `/favicon.svg` 응답 확인

## 주의사항

- **scope 파라미터 생략**: 클라이언트 등록 화면에는 `self_read`, HTTP 문서에는 `self:read`로
  표기가 달라 아예 scope를 안 보내고 서버 기본값을 쓰도록 함 (`datagsm.oauth.scope` 비워두면 됨).
- **로컬 테스트**: DataGSM 클라이언트 설정에 `http://localhost:8080/oauth/callback`을
  리다이렉트 URI로 추가 등록해야 로컬에서 로그인 테스트 가능.
- **secret 관리**: 절대 코드/깃에 커밋하지 말고 환경변수로만 주입 (DataGSM secret, CF 토큰 모두).

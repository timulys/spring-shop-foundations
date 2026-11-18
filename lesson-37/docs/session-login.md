# 로그인 요청과 세션 유지

회원가입은 회원과 비밀번호 해시를 저장합니다. 로그인은 입력한 비밀번호가 저장된 해시와 맞는지 대조하고, 성공한 회원의 인증 상태를 세션에 보관합니다. 이 과정에서는 Spring Security의 `formLogin` 필터가 `/login` 요청을 처리합니다. [공식 formLogin 흐름](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html)

`MemberDetailsService`는 입력 아이디를 정규화한 뒤 저장된 회원을 찾습니다. `ShopPrincipal`은 회원 번호·아이디·표시 이름과 인증에 사용할 해시를 담습니다. 인증 이후 자격 증명을 지울 수 있도록 `CredentialsContainer`를 구현합니다. 화면으로 내보낼 때는 `MemberResponse`의 세 필드만 사용합니다.

## 브라우저와 서버에 남는 값

브라우저는 세션 식별 쿠키를 보관하고 같은 출처의 요청에 전달합니다. 서버는 식별자에 대응하는 인증 상태를 읽습니다. 새로고침은 새 HTTP 요청이지만 같은 쿠키를 보내므로 회원 정보가 유지됩니다. 다른 쿠키 저장소를 쓰는 브라우저는 해당 인증 상태를 이어받지 않습니다.

이 예제의 세션은 애플리케이션 메모리에 있습니다. 서버를 재시작하면 다시 로그인해야 합니다. 회원·주문의 DB 저장과 로그인 세션의 유지 기간은 서로 다릅니다. [인증 상태 저장과 세션 관리](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)

로그인 성공 시 세션 ID가 바뀌고, 로그아웃은 세션과 인증 상태를 종료합니다. 쿠키에는 HttpOnly와 SameSite=Lax를 지정합니다. 로컬 실습 주소는 HTTP이며, 외부 서비스의 HTTPS와 쿠키 Secure 설정은 배포 환경에 맞춰 함께 구성합니다.

## CSRF 토큰을 보내는 위치

Spring Security의 기본 CSRF 검사는 POST·PATCH·DELETE처럼 상태를 바꾸는 요청에 적용됩니다. 화면의 공통 요청 함수는 `/api/csrf`에서 토큰과 헤더 이름을 받은 뒤 실제 요청에 넣습니다. 가입과 로그인, 로그아웃도 같은 경로를 사용합니다. 토큰을 받는 시간과 실제 요청 시간을 하나의 응답 대기 제한 안에서 처리합니다.

로그인·로그아웃 후에는 토큰이 갱신됩니다. 이 예제는 변경 요청마다 새로 토큰을 받아 이전 토큰을 재사용하지 않습니다. CSRF 토큰을 빠뜨린 로그아웃 요청은 403으로 끝나고 로그인 상태가 유지됩니다. [공식 CSRF 설명](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

## 응답 계약

| 상황 | 응답 |
|---|---|
| 로그인 성공 | 204, 응답 본문 없음 |
| 잘못된 비밀번호·없는 아이디 | 401, LOGIN_FAILED |
| 인증 없이 내 정보 GET | 401, AUTHENTICATION_REQUIRED |
| CSRF 토큰 없이 로그인·로그아웃 POST | 403, CSRF_INVALID |
| 로그아웃 성공 | 204, 이후 내 정보 GET은 401 |

인증 필터에서 발생한 오류는 컨트롤러에 도달하기 전에 처리됩니다. `SecurityResponses`가 기존 API와 같은 오류 모양을 만들고, `RequestIdFilter`를 인증 필터보다 먼저 실행해 오류에도 요청 ID를 넣습니다.

HTTP 테스트는 실제 쿠키를 저장하는 클라이언트를 사용합니다. 기존 주문 테스트의 입력과 금액 기준은 유지하고, 요청 준비 도구가 CSRF 토큰을 전달합니다. 인증 실패나 토큰 누락을 검사하는 테스트는 해당 준비를 의도적으로 생략해 원인을 하나씩 확인합니다.

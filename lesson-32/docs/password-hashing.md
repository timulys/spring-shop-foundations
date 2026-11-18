# 비밀번호 해시와 가입 처리

`PasswordEncoder.encode`는 입력 비밀번호로 저장할 해시 문자열을 만듭니다. BCrypt는 계산할 때마다 무작위 salt를 사용하므로 같은 입력에서 나온 두 문자열도 달라집니다. 입력을 대조할 때는 `matches(입력값, 저장된_해시)`를 사용합니다. 저장된 문자열에 들어 있는 버전·비용 계수·salt가 대조 계산에 쓰입니다.

이 프로젝트는 `BCryptPasswordEncoder(12)`를 Bean으로 등록합니다. 비용 계수는 계산량에 영향을 줍니다. 12는 이 실습의 선택이며, 실제 서비스에서는 사용할 장비와 동시 요청량을 기준으로 계산 시간을 측정해 결정합니다. [Spring Security 비밀번호 저장](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

## 입력 길이의 두 기준

입력 DTO와 정책은 Java `String.length()` 기준 8~64자를 허용합니다. 이 값은 UTF-16 코드 단위 수입니다. BCrypt에 전달하기 전에는 UTF-8로 변환한 길이도 72바이트 이하인지 검사합니다. 영문 `a` 64개는 64바이트, 한글 `가` 24개는 72바이트, `가` 25개는 75바이트입니다. 마지막 입력은 글자 수 조건을 만족하지만 바이트 조건에서 거부됩니다.

앞뒤 공백도 비밀번호의 일부로 유지합니다. 공백으로만 된 값은 거부하고, 올바르지 않은 유니코드 문자열도 거부합니다. 화면 검사는 빠른 안내를 맡으며, 서버가 같은 조건을 다시 검사한 뒤 해시를 계산합니다. [사용 버전의 BCrypt 구현](https://github.com/spring-projects/spring-security/blob/7.1.2/crypto/src/main/java/org/springframework/security/crypto/bcrypt/BCrypt.java)

## 가입의 처리 순서

`SignupService`는 아이디를 정규화하고 입력 비밀번호를 검사합니다. 아이디가 이미 저장되어 있으면 HTTP 409 `LOGIN_ID_TAKEN`을 반환합니다. 사용 가능한 경우에는 해시를 계산하고, 그 계산이 끝난 뒤 DB 쓰기 트랜잭션을 시작합니다. `Member`에는 해시만 전달하고 `saveAndFlush`로 저장합니다.

사전 조회와 저장 사이에 다른 요청이 같은 아이디를 저장할 수 있습니다. 이때 `uk_members_login_id` 제약이 두 번째 저장을 거부합니다. 서비스는 해당 트랜잭션이 롤백된 뒤 이 제약의 예외를 같은 HTTP 409로 변환합니다. 다른 제약의 오류는 중복 아이디로 바꾸지 않습니다.

`SignupHttpTest`의 동시 요청 검사는 두 요청의 실제 DB 조회가 끝날 때까지 기다리게 합니다. 두 요청이 모두 사전 조회를 통과한 상태에서 저장을 시도하면 응답은 201과 409이고, 회원은 한 명만 남습니다.

## 응답과 화면

POST `/api/members`가 성공하면 HTTP 201과 `id`, `loginId`, `displayName`을 반환합니다. 입력 DTO는 JSON으로 출력할 때 비밀번호를 제외하고, 엔티티도 해시를 JSON에서 제외합니다. 두 객체의 문자열 표현에도 비밀번호나 해시를 넣지 않습니다. 화면은 가입 요청이 끝나면 비밀번호 입력칸을 비웁니다.

응답 대기 시간이 지나거나 연결이 끊어지면 가입이 저장되었을 가능성이 남습니다. 화면은 이 상태를 성공으로 표시하지 않으며 자동 재전송도 하지 않습니다. 다시 가입했을 때 중복 안내가 나오는 경우에는 아이디의 가입 상태를 확인하도록 안내합니다.

31강에서는 `spring-security-crypto`로 비밀번호 처리를 추가했습니다. 현재 32강에는 웹 인증 필터와 세션 로그인도 연결되어 있습니다. [PasswordEncoder API 구현](https://github.com/spring-projects/spring-security/blob/7.1.2/crypto/src/main/java/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.java)

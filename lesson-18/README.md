# 18강 · 화면 이벤트와 비동기 요청 이해하기

버튼 클릭으로 실제 Spring Boot API를 호출하고, 성공과 오류 응답을 화면에 표시합니다. 17강의 서버 기능과 테스트를 유지하며 화면에서 함께 사용할 `api.js`를 추가했습니다.

Java 21, Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. JavaScript는 브라우저에서 실행하며 별도의 Node.js 설치나 프런트엔드 빌드가 필요하지 않습니다.

## 실행

IntelliJ에서 이 폴더를 열고 Project SDK와 Gradle JVM을 21로 지정합니다. 이전 실습 앱과 DB를 종료한 뒤 실행합니다.

```bash
docker compose up -d --wait db
./gradlew bootRun
```

- 상품 목록: http://127.0.0.1:8080/
- 이번 강의 실습: http://127.0.0.1:8080/request-lab.html

파일을 직접 더블클릭하는 대신 실행 중인 서버 주소로 접속합니다. HTML과 JavaScript, API가 같은 서버에서 제공됩니다.

## 직접 작성할 코드

- `src/main/resources/static/api.js`: `requestJson`의 요청, JSON 읽기, `response.ok` 검사
- `src/main/resources/static/request-lab.js`: 이벤트에서 경로 읽기, `await`, 성공·실패 표시, `finally`의 버튼 복구
- HTML·CSS와 `ApiError` 클래스는 제공 코드입니다.

브라우저 개발자 도구의 Sources에서 `request-lab.js`를 열고 `const path` 줄에 중단점을 설정합니다. 버튼을 클릭해 `event.currentTarget.dataset.path`를 확인한 뒤 계속 실행합니다. `const data` 다음 줄에 중단점을 설정하면 서버에서 받은 상품 배열을 확인할 수 있습니다. Network에서는 요청 URL·상태 코드·응답 본문을 대조합니다. 디버깅을 마치면 중단점을 해제합니다.

| 버튼 | 기대 결과 |
|---|---|
| 상품 목록 조회 | 200, 상품 3개 배열, 성공 안내 |
| 잘못된 가격 요청 | 400, HTTP_400, 최대 가격 입력 안내 |
| 없는 상품 조회 | 404, HTTP_404, 상품을 찾을 수 없다는 안내 |

`fetch`는 400·404 응답도 `Response`로 받습니다. 이 코드에서는 `response.ok`를 검사해 `ApiError`를 던지고 화면의 `catch`가 안내를 표시합니다. 연결 실패나 JSON 해석 실패는 별도의 일반 오류 안내로 표시합니다. 공통 함수는 이 앱의 JSON API를 대상으로 하며, 204 응답에서는 본문을 읽지 않고 `null`을 반환합니다.

## 테스트

```bash
docker compose --profile test up -d --wait test-db
./gradlew test
```

서버 테스트는 별도 `shop_test` DB를 사용합니다. 기존 HTTP·DB 통합 테스트를 포함한 53개 테스트를 실행합니다.

## 과제

오류 안내에서 HTTP 상태뿐 아니라 서버의 `code`도 표시해 보세요. 완성 코드와 비교한 뒤 400과 404 버튼으로 `HTTP_400`, `HTTP_404`가 구분되는지 확인합니다. 오류 뒤 상품 목록 버튼을 눌러 정상 화면으로 돌아오는지도 확인합니다.

실습 종료 시 앱을 먼저 종료한 뒤 `docker compose --profile test down`으로 이 강의의 컨테이너를 정지합니다.

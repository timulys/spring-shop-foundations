# 37강 · 준비 상태와 장애 확인

DB·앱·화면 서버에 상태 검사를 적용합니다. 앱의 readiness는 앱 준비 상태와 DB 연결을 함께 확인하고, liveness는 앱 자체의 상태를 확인합니다. [상태 검사와 장애 대응 흐름](docs/container-health.md)을 함께 읽어 주세요.

## 실행과 코드 검사

이전 강의의 앱과 컨테이너를 종료한 뒤 실행합니다.

```bash
cp .env.example .env
docker compose up -d --build --wait
docker compose ps
```

Windows에서는 파일 탐색기나 `Copy-Item .env.example .env`로 복사할 수 있습니다. DB·앱·web이 healthy 상태가 되면 http://127.0.0.1:8080 에서 회원가입·로그인·상품 주문을 진행합니다. 키보드 2개와 마우스 1개 주문의 총액은 70,000원입니다.

Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 전체 컨테이너 실행에는 Docker가 필요하고, 호스트에서 코드를 검사할 때는 JDK 21과 Node.js를 사용합니다.

```bash
docker compose --profile test up -d --wait test-db
./gradlew test
npm run test:ui
```

Java 테스트 160개, JavaScript 테스트 10개가 있습니다. IntelliJ의 Project SDK와 Gradle JVM은 21로 지정합니다. Windows에서는 `gradlew.bat`를 사용합니다. 호스트 IDE에서 앱을 실행하는 방법은 36강과 같이 `compose.dev.yaml`로 DB 포트를 연결하고 실행 환경변수를 지정하는 방식입니다.

## 내부 상태 확인

```bash
docker compose exec app curl -i http://127.0.0.1:8080/actuator/health/readiness
docker compose exec app curl -i http://127.0.0.1:8080/actuator/health/liveness
```

정상일 때 두 요청은 200과 `{"status":"UP"}`을 반환합니다. 진단 경로는 NGINX가 전달하는 공개 경로에 포함하지 않으므로 브라우저의 `/actuator/health/readiness` 요청은 404입니다. 상태 응답에는 DB 접속 주소나 상세 오류를 포함하지 않습니다.

## DB 중단과 복구

```bash
docker compose stop db
docker compose ps
docker compose exec app curl -i http://127.0.0.1:8080/actuator/health/readiness
docker compose exec app curl -i http://127.0.0.1:8080/actuator/health/liveness
docker compose logs app db
```

DB가 중단되면 readiness는 503, liveness는 200입니다. 앱 프로세스는 실행 중이지만 연속 상태 검사 실패 후 unhealthy가 됩니다. NGINX의 정적 화면은 열리고 DB를 사용하는 API 요청은 실패합니다.

```bash
docker compose up -d --wait db
```

DB가 준비되면 앱이 연결을 다시 얻고 readiness가 200으로 회복됩니다. 기존 주문의 번호와 금액을 다시 확인해 주세요.

## 앱 중단과 복구

```bash
docker compose stop app
# 이때 정적 화면은 200, 상품 API는 502 또는 504입니다.
docker compose start app
```

앱이 다시 준비되면 로그인 후 이전 주문을 조회합니다. 앱 메모리의 세션은 재시작으로 사라지고 DB의 주문은 유지됩니다. 실습이 끝나면 `docker compose --profile test down`으로 컨테이너를 종료합니다. DB 볼륨은 남습니다.

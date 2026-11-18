# 35강 · 화면·앱·DB를 Compose로 연결하기

NGINX가 정적 화면을 제공하고 API·로그인·로그아웃 요청을 Spring 앱으로 전달합니다. 브라우저는 http://127.0.0.1:8080 주소 하나로 상점을 이용합니다. [프록시와 네트워크 설명](docs/compose-proxy.md)을 함께 읽어 주세요.

## 전체 실행

이전 강의의 앱과 컨테이너를 종료한 뒤 이 폴더에서 실행합니다.

```bash
docker compose up -d --build
docker compose logs -f app
```

앱 로그의 `Started ShopApplication`을 확인한 뒤 상점을 엽니다. 첫 시작에는 DB 스키마와 앱을 초기화하는 동안 API 요청에 502가 반환될 수 있습니다. 로그 보기는 Ctrl+C로 끝낼 수 있으며 컨테이너는 계속 실행됩니다. 서비스 준비 여부를 Compose가 확인하게 하는 설정은 37강에서 추가합니다.

1. 회원을 가입시키고 로그인합니다.
2. 상품을 검색하고 키보드 2개·마우스 1개를 담습니다.
3. 총액 70,000원으로 모의 결제를 완료합니다.
4. 새로고침 후 주문 내역을 조회하고 주문을 취소합니다.
5. 로그아웃하면 내 주문 API는 401을 반환합니다.

Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 앱의 Temurin 이미지와 화면 서버의 NGINX 이미지 다이제스트는 Dockerfile에 고정되어 있습니다. 전체 실행에는 호스트 JDK나 Node.js가 필요하지 않습니다.

## 테스트와 IDE 실행

호스트 테스트에는 JDK 21과 Node.js를 사용합니다. Java 검사 157개, JavaScript 검사 10개가 있습니다.

```bash
docker compose --profile test up -d --wait test-db
./gradlew test
npm run test:ui
```

IntelliJ에서 앱을 직접 실행하려면 먼저 프록시와 컨테이너 앱을 중지하고 DB의 개발용 호스트 포트를 연결합니다.

```bash
docker compose stop web app
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait db
./gradlew bootRun
```

IntelliJ에서는 `build.gradle`을 열고 Project SDK·Gradle JVM을 21로 선택합니다. Windows 터미널에서는 `gradlew.bat`를 사용합니다. IDE 앱의 기본 DB 주소는 `127.0.0.1:15432`, 컨테이너 앱의 주소는 `db:5432`입니다. 테스트는 별도의 `shop_test` DB와 15433 포트를 사용합니다.

## 구성과 종료

- `web`: NGINX·화면 파일·프록시 설정. 호스트의 8080만 연결합니다.
- `app`: Spring 애플리케이션. 컨테이너 내부 8080을 사용합니다.
- `db`: PostgreSQL. 기본 구성에서는 내부 5432만 사용합니다.
- `test-db`: 테스트 프로필을 켰을 때만 실행하는 전용 DB입니다.

```bash
docker compose ps
docker compose logs web app db
docker compose --profile test down
```

`down` 뒤에도 이름이 있는 DB 볼륨은 남습니다. 앱을 다시 만들면 메모리 로그인 세션은 사라지므로 다시 로그인합니다. 회원과 주문 데이터는 DB에 남습니다.

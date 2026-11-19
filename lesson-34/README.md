# 34강 · 애플리케이션 이미지

33강의 회원별 상점을 Docker 이미지로 빌드합니다. 컨테이너에서도 회원가입·로그인·장바구니·모의 결제·주문 취소를 같은 API로 사용합니다. [이미지의 빌드·실행 과정](docs/app-image.md)을 함께 읽어 주세요.

## 코드 검사와 이미지 만들기

JDK 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. Dockerfile은 Eclipse Temurin 21 JDK·JRE의 이미지 다이제스트를 고정합니다. 이전 강의의 앱과 컨테이너를 종료한 뒤 이 폴더에서 실행합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
npm run test:ui
docker build -t spring-shop:lesson34-v1 .
```

Java 테스트는 157개, JavaScript 테스트는 10개입니다. JDK 21과 Node.js는 호스트에서 테스트할 때 사용합니다. 이미지 빌드는 Dockerfile의 JDK에서 `bootJar`를 실행합니다. 빌드 단계에는 DB를 연결하지 않으므로 통합 테스트는 위 명령으로 먼저 실행합니다.

## 컨테이너 실행

```bash
docker run --rm --name shop34 \
  --network spring-shop-lesson34_default \
  -p 127.0.0.1:8080:8080 \
  -e DB_URL=jdbc:postgresql://db:5432/shop \
  spring-shop:lesson34-v1
```

http://127.0.0.1:8080 에 접속합니다. PowerShell에서는 위 명령을 한 줄로 입력하거나 해당 셸의 줄 연결 문법을 사용합니다. 다른 터미널에서 `docker stop shop34`를 실행하면 앱 컨테이너가 종료됩니다. `--rm`은 종료한 앱 컨테이너를 제거하고 DB 볼륨은 그대로 둡니다.

네트워크 이름은 제공한 Compose 프로젝트 이름을 기준으로 합니다. `COMPOSE_PROJECT_NAME`을 별도로 설정했다면 실제 프로젝트의 네트워크 이름으로 바꿉니다. DB의 주소는 같은 네트워크의 서비스 이름인 `db`, 포트는 컨테이너 내부의 5432입니다.

## 실행 결과 확인

회원가입 후 키보드 2개와 마우스 1개를 담아 주문하면 소계 75,000원, 할인 5,000원, 배송비 0원, 총액 70,000원입니다. 다른 회원의 주문은 조회·취소할 수 없습니다. 주문 완료 후 장바구니는 비워집니다.

http://127.0.0.1:8080/image-demo.txt 에서는 `Shop image demo - v1`을 볼 수 있습니다. 이 파일은 이미지 버전 실습용 표시입니다. 파일을 바꾼 뒤 새 이미지를 빌드하고 새 컨테이너를 실행해야 변경한 값이 나타납니다.

호스트에서 실행할 때는 IntelliJ로 `build.gradle`을 열고 JDK 21을 선택하거나 `./gradlew bootRun`을 사용합니다. 같은 8080 포트를 쓰므로 컨테이너 앱을 먼저 종료합니다. Windows에서는 `gradlew.bat`를 사용합니다.

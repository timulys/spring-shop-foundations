# 스프링 부트로 쇼핑몰 만들기

자바 기초 과정의 상품·주문 예제를 Spring Boot·JPA·DDD·Docker·MSA로 확장하는 강의 실습 코드입니다. 강의별 본문 완성 프로젝트 하나를 제공합니다.

| 강의 | 완성 코드 | 확인할 결과 |
|---|---|---|
| 1강 · 첫 서버 실행 | [lesson-01](lesson-01) | /hello 인사·키보드·30000원 |
| 2강 · JSON 상품 목록 | [lesson-02](lesson-02) | 상품 세 개의 코드·이름·숫자 가격 |
| 3강 · 서비스와 생성자 주입 | [lesson-03](lesson-03) | 조회 분리, category 포함 목록 유지 |
| 4강 · 상세와 검색 조건 | [lesson-04](lesson-04) | 상세·필터, 200·400·404 구분 |
| 5강 · 첫 웹 화면 | [lesson-05](lesson-05) | API의 실제 상품명·가격을 카드로 표시 |
| 6강 · Docker와 PostgreSQL | [lesson-06](lesson-06) | DB 연결·상태 확인·볼륨의 데이터 유지 |
| 7강 · 상품 SQL | [lesson-07](lesson-07) | 테이블·조회·조건·정렬·변경·제약 조건 |
| 8강 · JPA 매핑 | [lesson-08](lesson-08) | Flyway로 테이블 준비·엔티티 매핑 검증 |
| 9강 · Repository | [lesson-09](lesson-09) | DB 상품 조회·저장·변경·테스트 롤백 |
| 10강 · 요청 검증 | [lesson-10](lesson-10) | 장바구니 변경·입력 검증·공통 오류 응답 |
| 11강 · 장바구니 관계 | [lesson-11](lesson-11) | 항목 저장·재시작 후 수량 유지·삭제 범위 확인 |
| 12강 · 주문 정보 보관 | [lesson-12](lesson-12) | 상품명·가격 스냅샷·주문 생성·이전 주문 금액 유지 |
| 13강 · 트랜잭션 | [lesson-13](lesson-13) | 주문 저장과 장바구니 비우기·커밋·실패 롤백 |
| 14강 · 변경 감지와 취소 | [lesson-14](lesson-14) | 관리·분리 상태 비교·취소 상태 저장·flush 후 롤백 |
| 15강 · 주문 목록과 페이지 | [lesson-15](lesson-15) | 최신순 정렬·페이지 응답·범위 검증·삽입 후 경계 변화 |
| 16강 · 연관 데이터 조회 | [lesson-16](lesson-16) | 목록 DTO 쿼리·상세 fetch join·SQL과 응답 비교 |
| 17강 · API 통합 테스트 | [lesson-17](lesson-17) | 실제 HTTP 요청·응답과 DB 상태·실패 롤백·실행 순서 독립성 |
| 18강 · 화면 이벤트와 비동기 요청 | [lesson-18](lesson-18) | 버튼 이벤트·공통 API 호출·200·400·404 표시·오류 후 복구 |
| 19강 · 상품 검색과 상세 화면 | [lesson-19](lesson-19) | 카테고리·검색어 조건·상품 카드·상세 조회·빈 결과와 초기화 |

| 20강 · 장바구니 화면 | [lesson-20](lesson-20) | 담기·수량 변경·삭제·서버 합계 표시·잘못된 입력 복구 |

[전체 코드 ZIP 다운로드](https://github.com/timulys/spring-shop-foundations/archive/refs/heads/main.zip)

JDK 21을 설치하고 원하는 강의 폴더의 build.gradle을 IntelliJ에서 엽니다. Project SDK와 Gradle JVM을 21로 지정한 뒤 ShopApplication을 실행합니다. 터미널 실행은 강의 폴더에서 ./gradlew bootRun입니다. Gradle을 별도 설치할 필요는 없습니다.

기준 환경은 Spring Boot 4.1.2, Gradle Wrapper 8.14.3, Java 21(Zulu 21.0.7)입니다. 서버는 127.0.0.1:8080을 사용하므로 한 강의만 실행합니다. 1강은 /hello, 2~4강은 /api/products, 5강은 / 주소에서 결과를 확인합니다.

1~8강의 상품 API는 메모리 목록을 사용합니다. 6강에서 PostgreSQL을 실행하고, 7강에서 SQL을 익힌 뒤, 8강에서 엔티티를 매핑합니다. 9강부터 상품 API가 실제 DB를 조회합니다. 10강의 장바구니 수량은 메모리에 보관하며 DB 저장은 11강에서 추가합니다. 2강 본문은 세 필드이고, 3강부터 2강 카테고리 과제 정답을 반영합니다. 자바 코드 수정 후 저장·재실행합니다. 자세한 조건은 각 강의 README에 있습니다.

6강부터 Docker 실행 환경과 PostgreSQL 18.4를 사용합니다. 각 강의 폴더에서 `docker compose up -d --wait`로 DB를 먼저 시작합니다. DB 포트 15432와 앱 포트 8080이 겹치지 않도록 이전 강의의 앱과 컨테이너를 종료해 주세요. 강의별 `README.md`에 실행 순서, 실습 요청, 예상 결과와 과제를 정리했습니다.

17강부터 통합 테스트용 DB를 별도로 사용합니다. `docker compose --profile test up -d --wait db test-db`로 일반 DB와 테스트 DB를 준비합니다. 테스트는 `shop_test`에서만 데이터를 정리하며, 기본 포트는 15433입니다.

# ⚡Flash-Sale E-Commerce Service

## 📌 목차
1. [프로젝트 소개](#----------)
2. [기술 스택](#--------)
3. [ERD](#---erd)
4. [Architecture](#---architecture)
5. [주요 기능](#--------)
<br><br>
---
<br>

## 🔷 프로젝트 소개
- 기존 모놀리식 구조에서 **MSA(Microservice Architecture)로 전환**하여 서비스 확장성과 유지보수성을 개선한 프로젝트입니다.  
- 상시 구매 가능한 일반 상품 외에, **특정 시간에 한정된 수량의 특가 상품**을 판매하는 Flash Sale 이벤트를 제공합니다.  
- **kafka를 활용한 비동기 처리**와 **Redis 캐싱 전략**을 통해 **대규모 트래픽 처리 과정에서 동시성 문제를 해결**하고, 실시간 재고 관리와 빠른 주문 처리를 지원합니다.

- **API 명세서**  
    🔗 [Member](https://documenter.getpostman.com/view/29351082/2sAXjNZXBh) 🔗 [Order](https://documenter.getpostman.com/view/29351082/2sAXjRVUi4) 🔗 [Product](https://documenter.getpostman.com/view/29351082/2sAXjQ3W5w)
<br><br>

## 🔷 기술 스택
- **Language:** `Java 17`
- **Backend Framework**: `Spring Boot(3.x)`, `Spring Data JPA`, `Spring Security`, `Spring Cloud Gateway`, `Netflix Eureka`, `Spring Batch`
- **Build Tool**: `Gradle`
- **Database:** `MySQL`, `Redis(Caching)`
- **Authentication/Authorization**: `Spring Security`, `JWT`
- **Messaging**: `Kafka`
- **Service Communication**: `Feign Client`
- **Containerization:** `Docker`
- **Performance Testing**: `JMeter` 
<br><br>

## 🔷 ERD
![erd](https://github.com/user-attachments/assets/cfa493cd-b83a-4203-88da-5f22b97ab09a)
<br><br>

## 🔷 Architecture
![architecture](https://github.com/user-attachments/assets/0d1c3734-46db-4a16-9d75-c7aa33738d44)
<br><br>

## 🔷 주요 기능

<details>
  <summary> ⚡ 주문 플로우 차트 ⚡</summary>

  ![orderflow](https://github.com/user-attachments/assets/1d6152bb-6b32-4229-b5f8-e42467abb7de)

</details>

1. **Redis 기반의 재고 관리**
    - 효율적인 재고 관리를 위해 Redis를 활용한 **캐싱** 전략 도입
    - **Redisson의 분산 락**을 사용하여 1000건 이상의 대량의 동시 재고 감소 요청에도 데이터 일관성 유지
    - 일정 주기로 DB와 동기화 작업을 수행하는 **자동화 스케줄링**
2. **Flash Sale 이벤트 도입 : 일부 비동기 방식으로 전환 및 성능 최적화**
    - 재고 확인 및 감소 : **동기 방식과 분산 락** 사용 → 데이터 불일치 문제 방지
    - 결제 및 배송 처리 : **비동기**적으로 전환하여 주문 속도 개선
    - **평균 응답 속도가 50% 개선**되었고, **TPS는 300 → 350으로 약 17% 증가**하여 대규모 트래픽에도 안정적인 주문 처리가 가능해짐
3. **Kafka 기반 비동기 메시징 시스템 도입 → 이메일 전송 처리 성능 개선**
    - 사용자가 이메일 전송을 요청하면, **Kafka 토픽에 이벤트를 발행**하고 **즉시 응답을 반환**하는 구조로 변경
    - 실제 이메일 전송 작업은 **Kafka의 consumer가 해당 토픽을 구독**하고, 백그라운드에서 **비동기적으로 처리**함
    - 응답 시간 : <u>**1건**</u>당 3980ms → <u>**100건**</u>당 12ms로 개선, **TPS : 0.26 → 95.69**로 대폭 향상됨

4. **API Gateway와 JWT 기반의 인증/인가 시스템**
    - API Gateway를 통해 클라이언트 요청을 **라우팅**하고, **토큰 인증 필터**를 적용하여 보안 강화
    - Access/Refresh Token 기반으로 인증 관리 및 Redis에 저장된 블랙리스트를 기반으로 로그아웃된 사용자의 접근 차단

5. **Resilience4j의 CircuitBreaker 패턴**을 도입하여 **서비스 간 장애 전파 방지** 및 안정적인 사용자 경험 제공
    - 회원 정보 조회 시, Order Service와 연동하여 해당 회원의 주문 내역을 함께 조회하는 기능 구현
    - 10번의 호출 중 절반 이상 실패 시 CircuitBreaker가 open 상태로 전환 → **에러 응답 대신 빈 주문 내역을 반환**
<br><br>

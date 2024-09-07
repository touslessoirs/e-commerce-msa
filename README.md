# ⚡Flash-Sale E-Commerce Service

## 📌 목차
1. [프로젝트 소개](#-------)
2. [기술 스택](#-----)
3. [ERD](#erd)
4. [아키텍처 구조](#-------)
5. [주요 기능](#-----)
6. [주문 플로우 차트](#---------)
7. [트러블 슈팅](#------)
    * [Issue #1 Redis를 이용한 재고 관리 중 동시성 문제로 인한 재고 불일치 발생](#issue--1-redis----------------------------------)
    * [Issue #2 CircuitBreaker를 이용한 마이크로서비스 장애 회피](#issue--2-circuitbreaker-------------------)
    * [Issue #3 이메일 전송 처리 성능 개선](#issue--3----------------)
    * [Issue #4 Flash Sale 이벤트 도입 : 일부 비동기 방식으로 전환 및 성능 최적화](#issue--4-flash-sale---------------------------------)
<br><br>
---
<br>

## 🔷 프로젝트 소개
기존 모놀리식 구조에서 **MSA(Microservice Architecture)로 전환**하여 서비스 확장성과 유지보수성을 개선한 프로젝트입니다.  
상시 구매 가능한 일반 상품 외에, **특정 시간에 한정된 수량의 특가 상품**을 판매하는 Flash Sale 이벤트를 제공합니다.  
**kafka를 활용한 비동기 처리**와 **Redis 캐싱 전략**을 통해 **대규모 트래픽 처리 과정에서 동시성 문제를 해결**하고, 실시간 재고 관리와 빠른 주문 처리를 지원합니다.

- 개발 기간 : 2024.08.07 ~ 2024.09.04
- API 명세서  
    🔗 [Member Service](https://documenter.getpostman.com/view/29351082/2sAXjNZXBh)  
    🔗 [Order Service](https://documenter.getpostman.com/view/29351082/2sAXjRVUi4)  
    🔗 [Product Service](https://documenter.getpostman.com/view/29351082/2sAXjQ3W5w)
<br><br>

## 🔷 기술 스택
- **Language:** `Java 17`
- **Backend Framework**: `Spring Boot(3.x)`, `Spring Data JPA`, `Spring Security`, `Spring Cloud Gateway`, `Spring Actuator`
- **Build Tool**: `Gradle`
- **Database:** `MySQL`
- **Authentication/Authorization**: `JWT`
- **Caching & Locking**: `Redis`, `Redisson`
- **Messaging**: `Kafka`
- **Service Communication**: `Feign Client`
- **DevOps:** `Docker`
- **Service Discovery**: `Netflix Eureka (Spring Cloud)`
- **Performance Testing**: `JMeter`
<br><br>

## 🔷 ERD
![erd](https://github.com/user-attachments/assets/14af2ac7-6d75-48d1-89e4-fb00efc16136)
<br><br>

## 🔷 Architecture
![architecture](https://github.com/user-attachments/assets/ccec43cd-bf16-4483-903d-a9b935a4e2d9)
<br><br>

## 🔷 주요 기능
1. **Redis 기반의 재고 관리**
    - Redis를 활용한 캐싱 전략을 도입하여 빠른 재고 조회 및 관리 제공
    - Redisson의 분산 락 이용 → 동시 재고 감소 요청 시 발생할 수 있는 race condition 문제 방지
2. **Feign Client와 Kafka를 통한 서비스 간 통신**
    - 동기식 Feign Client 통신과 Kafka 기반 비동기 이벤트 처리로 시스템 성능 최적화
    - Kafka 기반 메시징으로 결제 및 배송 서비스 간 느슨한 결합 구현 및 장애 발생 시 데이터 유실에 대한 안정성 향상
3. **API Gateway와 JWT 기반의 인증/인가 시스템**
    - API Gateway를 통해 클라이언트 요청을 라우팅하고, 토큰 인증 필터를 적용하여 보안 강화
    - Access/Refresh Token 기반으로 인증 관리 및 Redis에 저장된 블랙리스트를 기반으로 로그아웃된 사용자의 접근 차단
4. **Resilience4j의 Circuitbreaker을 통한 장애 전파 방지**
    - Feign Client 통신 장애 발생 시 회복 탄력성을 갖춤으로서 서비스의 신뢰성 유지
5. **주문 및 배송 상태 관리 스케줄러**
    - 주문 및 배송 상태를 자동으로 스케줄링하여 주기적으로 DB 업데이트
<br><br>

## 🔷 주문 플로우 차트
![orderflow](https://github.com/user-attachments/assets/1d6152bb-6b32-4229-b5f8-e42467abb7de)
<br><br>

## 🔷 트러블 슈팅
### 🔹Issue #1 Redis를 이용한 재고 관리 중 동시성 문제로 인한 재고 불일치 발생
1. 문제
    - 경합 상태로 인한 **재고의 불일치 문제**
2. 해결 과정
    - 데이터 일관성 유지를 위해 재고 정보 감소 시 Redission 분산 락 사용
    - 일정 주기로 DB와 동기화 작업을 수행하는 자동화 스케줄링 구현
3. 결과
    - DB에 대한 직접적인 접근을 줄이고, 1000건 이상의 대량의 동시 요청에도 **정확한 재고 수량 반영**이 가능해짐
<br><br>

### 🔹Issue #2 CircuitBreaker를 이용한 마이크로서비스 장애 회피
1. 문제
    - 회원 정보 조회 시 order service와 연동되어 해당 회원의 주문 내역을 함께 조회하는 기능을 구현함
    - **서비스 간 장애 전파**로 인해 주문 내역 조회 실패 때문에 회원 정보 조회도 불가능해지는 발생
2. 해결 방법
    - Resilience4j 라이브러리를 사용하여 **CircuitBreaker 패턴 도입**
        - 10번의 호출 중 절반 이상 실패하면 CircuitBreaker가 open 상태로 전환됨
        - fallback 응답 설정 → Order Service의 응답을 받을 수 없는 상황에서는 **빈 주문 내역 반환**
3. 결과
    - Order Service에 장애가 발생해도 Member Service가 정상적으로 동작하게 함으로써 사용자 경험 개선
<br><br>

### 🔹Issue #3 이메일 전송 처리 성능 개선
1. 문제
    - 사용자가 이메일 전송을 요청하면, 이메일 전송이 완료된 후에 사용자에게 응답을 반환하는 구조
    - 1건의 이메일을 처리하는 데 3~4초 소요
    - 동시에 여러 명이 이메일을 요청하거나 시스템에 오류가 발생하여 100건 이상의 이메일 전송 요청이 한 번에 들어오면 심각한 성능 문제 발생
2. 해결 방법
    - **Kafka를 활용한 비동기 메시징 시스템 도입**
        - 사용자가 이메일 전송을 요청하면, Kafka 토픽에 해당 **이메일 전송 요청을 비동기적으로 발행**하고, **사용자에게는 즉시 응답을 반환**함
        - 실제 이메일 전송 작업은 Kafka의 consumer가 해당 토픽을 구독하고, 백그라운드에서 비동기적으로 처리함
3. 결과
    - [개선 전] 이메일 전송 요청 **1건**당 응답까지 걸린 시간 : 3980ms, TPS : 0.26/초
    - [개선 후] 이메일 전송 요청 **100건**당 응답까지 걸린 시간 : 12ms, TPS : 95.69/초
<br><br>

### 🔹Issue #4 Flash Sale 이벤트 도입 : 일부 비동기 방식으로 전환 및 성능 최적화
1. 문제
    - 기존의 주문 처리는 동기 방식으로, 재고 확인, 결제 처리, 배송 정보 저장 등의 모든 작업을 순차적으로 진행하는 구조
    - 한정 판매 이벤트를 도입하면서, 1000건 이상의 동시 요청 시 다양한 문제 발생
        - 사용자 신뢰성 저하 : **race condition이 발생**하여 재고가 음수로 처리되거나, 재고가 부족함에도 불구하고 주문이 성공하는 문제
        - 주문 처리 속도가 느려지고, 결제나 배송 정보가 지연되면 전체 주문 흐름이 중단되는 상황 발생
2. 해결 방법
    - **재고 확인 및 감소 - 기존 동기 방식 유지 및 분산 락 도입**
        - 재고가 충분하지 않다면 주문이 실패로 처리되어야 하므로 데이터 불일치 문제 방지
        - 동일한 자원(재고)에 동시에 접근하지 못하도록 분산 락 도입
    - **결제 및 배송 처리 - 비동기 방식으로 전환**
        - 주문 정보는 먼저 '주문 진행 중' 상태로 데이터베이스에 저장한 뒤, 결제 요청 이벤트와 배송 정보 저장 이벤트를 각각 비동기적으로 발행
        - 사용자가 주문을 완료한 후 결제나 배송 정보가 내부적으로 처리될 수 있도록 함
        - 결제 처리 중 오류가 발생하거나 배송 정보 저장이 실패한 경우, 이를 **rollback 이벤트로 처리하여 데이터 일관성 유지**
3. 결과
    - **평균 응답 속도 50% 개선**
        - 주문 처리가 결제와 배송에 의존하지 않기 때문에 사용자 경험이 더욱 안정적으로 개선됨
    - TPS(초당 처리 건수) 300 → 350 **(약 17% 개선)**
        - 대규모 트래픽 상황에서도 안정적으로 주문을 처리할 수 있게 됨

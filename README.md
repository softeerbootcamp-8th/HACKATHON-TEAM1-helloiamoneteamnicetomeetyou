<div align="center">

# NearLy

### 근거리 기반 랜덤 굿즈 교환 서비스

같은 공간에 있는 교환 상대를 찾아, 원하는 굿즈로 바꿉니다.

<img src="https://github.com/user-attachments/assets/3b30f8a1-f223-40a9-ba3b-47e53b6de4b9" width="720" alt="NearLy" />

</div>

---

<br>

## 목차

- [서비스 설명](#서비스-설명)
- [기술 스택](#기술-스택)
- [서비스 아키텍처](#서비스-아키텍처)
- [ERD](#erd)
- [Known Issues](#known-issues)
- [팀 구성](#팀-구성)

<br>

## 서비스 설명

팝업스토어에서 랜덤 굿즈를 산 방문객은 원하는 캐릭터나 상품이 나오지 않거나 중복이 생기면 교환할
상대를 직접 찾아 나섭니다. 같은 현장에 교환이 가능한 사람이 있어도 서로를 알지 못한다는 것이
문제였고, NearLy 는 현장에서 그 교환 가능성을 찾아 연결해 줍니다.

| | 기존 방식 | NearLy |
| --- | --- | --- |
| **상대 탐색** | X 와 오픈채팅, 커뮤니티를 직접 검색 | 원하는 굿즈만 등록하면 자동으로 매칭 |
| **매칭 성공률** | 1:1 이 맞아야만 성사 | 1:1 이 안 되면 세 사람이 순환하는 삼자 교환 |
| **의사 확인** | 모르는 사람에게 직접 말을 걸어야 함 | 등록과 신청으로 교환 의사를 미리 확인 |
| **약속 조율** | 상대가 현장에 있는지 확인하고 시간과 장소를 협의 | 지정 교환장소 고정, 겹치는 시간 자동 확정 |
| **현장에서 만나기** | 사람들 사이에서 상대를 직접 찾음 | 같은 과일 화면을 든 사람을 찾아 식별 |

<br>

### 주요 기능

- **알아서 찾아주는 교환 상대.** 내놓을 굿즈(Have)와 찾는 굿즈(Wanted)를 등록하면 조건이 맞는
  상대를 시스템이 자동으로 찾습니다.
- **둘이 아니어도 셋이서 교환.** 서로 정확히 맞는 상대가 없어도 A 에서 B, B 에서 C, C 에서 A 로
  도는 삼자 교환으로 성사 기회를 넓힙니다.
- **현장에서 바로 교환.** 약속이 확정되면 과일 아이콘 식별 화면을 발급해서, 같은 화면을 든 사람을
  찾아 대면 교환합니다.
- **찔러보기.** 자동 매칭을 기다리지 않고 원하는 상대의 카드에 직접 교환을 신청할 수 있습니다.
- **시간 조율.** 15분 단위로 가능한 시간을 고르면 모두가 되는 가장 빠른 시간으로 확정됩니다.

<br>

### 핵심 플로우

<img src="https://github.com/user-attachments/assets/d5e7f076-9d66-4a68-a087-f8d7fd3b2782" width="900" alt="User flow" />

굿즈를 얻은 다음 교환이 성사되기까지를 교환 등록, 교환 매칭, 정해진 시간에 만남, 교환 네 단계로
줄였습니다. 교환이 끝난 뒤에도 아직 찾는 카드가 남아 있으면 자동 매칭이 다시 돕니다.

<br>

### 화면 구성

<table>
  <tr>
    <td align="center" width="33%"><img src="https://github.com/user-attachments/assets/bcad306c-1679-4a5a-8568-4c671ec0086b" width="230" alt="교환 대기존" /></td>
    <td align="center" width="33%"><img src="https://github.com/user-attachments/assets/7eaa0a0f-0a53-461a-ad27-90fa7d3db91d" width="230" alt="삼자 매칭 결과" /></td>
    <td align="center" width="33%"><img src="https://github.com/user-attachments/assets/2a094655-54d7-4c2f-801a-51d96b65f73a" width="230" alt="현장 식별" /></td>
  </tr>
  <tr>
    <td align="center"><b>교환 대기존</b><br/><sub>지금 부스에 올라온 카드를 보고<br/>내 카드를 끌어 찔러봅니다</sub></td>
    <td align="center"><b>삼자 매칭 결과</b><br/><sub>세 사람의 카드가 어떻게<br/>이어지는지 보여 줍니다</sub></td>
    <td align="center"><b>현장 식별</b><br/><sub>같은 과일 화면을 든 사람이<br/>교환할 상대입니다</sub></td>
  </tr>
</table>

부스 운영자가 쓰는 어드민은 서버에서 그려 내려보냅니다. 화면과 사용법은
[`docs/admin`](./docs/admin) 에 있습니다.

<br>

## 기술 스택

**Frontend**

![React](https://img.shields.io/badge/React_19-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite_8-646CFF?style=flat-square&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_4-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router_8-CA4245?style=flat-square&logo=reactrouter&logoColor=white)
![PWA](https://img.shields.io/badge/PWA_Workbox-5A0FC8?style=flat-square&logo=pwa&logoColor=white)

**Backend**

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=flat-square&logo=mysql&logoColor=white)
![SSE](https://img.shields.io/badge/SSE-FF6F00?style=flat-square&logo=serverfault&logoColor=white)

**Infra**

![Amazon EC2](https://img.shields.io/badge/Amazon_EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white)
![Amazon RDS](https://img.shields.io/badge/Amazon_RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Caddy](https://img.shields.io/badge/Caddy-1F88C0?style=flat-square&logo=caddy&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-000000?style=flat-square&logo=vercel&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

<br>

## 서비스 아키텍처

![서비스 아키텍처](./docs/architecture.png)

프론트엔드는 Vercel 에, 백엔드는 EC2 에 올라가 있고 RDS 는 private subnet 에 두어 EC2 를 거쳐서만
접근합니다. 화면이 HTTPS 라 백엔드에도 HTTPS 가 필요해서, EC2 앞에 Caddy 를 두고 sslip.io 도메인으로
주소를 고정했습니다. main 에 push 하면 테스트와 이미지 빌드를 거쳐 EC2 로 배포하고 결과를 Slack 으로
보냅니다.

<br>

## ERD

<img src="https://github.com/user-attachments/assets/749b2861-3262-4894-b710-8093549add80" width="960" alt="ERD" />

한 사용자가 내놓을 카드는 `user_have_items` 에, 찾는 카드는 `user_want_items` 에 수량과 함께
담깁니다. 매칭이 성사되면 `exchanges` 한 건이 생기고 참여자는 `exchange_participants` 로,
누가 누구에게 어떤 카드를 주는지는 `exchange_items` 로 남습니다. 참여자가 둘이면 1:1 교환이고
셋이면 삼자 교환이라, 두 경우를 나누지 않고 같은 구조를 씁니다.

<br>

## Known Issues

교환은 두 사람이 동시에 잡힐 수 있는 자리라 동시성이 문제가 됩니다. 지금 이렇게 막아 두었고, 남은
것은 아래와 같습니다.

| 내용 | 지금 상태 |
| --- | --- |
| **매칭 재실행을 서버 안에서만 모았습니다** | 등록 화면이 카드를 한 장씩 보내서, 내놓을 카드 4장에 찾는 카드 3장이면 매칭이 7번 돌았습니다. 이벤트를 마지막 한 번으로 합쳐 막았고, 등록을 한 번에 받는 API 로 바꾸는 것이 근본 해결로 남아 있습니다 |
| **매칭과 찔러보기 트랜잭션을 `READ_COMMITTED` 로 낮췄습니다** | MySQL 기본값인 REPEATABLE READ 에서는 `SELECT ... FOR UPDATE` 로 상대를 기다린 뒤 다시 읽어도 트랜잭션 첫 스냅샷을 봐서, 잠금이 걸려도 중복 교환을 막지 못했습니다. 낮춘 대신 같은 트랜잭션에서 같은 행을 두 번 읽으면 값이 달라질 수 있어, 이 경로에 조회를 더할 때 확인이 필요합니다 |
| **교환을 만드는 경로의 잠금 순서를 `ExchangeLock` 한 곳에 모았습니다** | 자동 매칭과 찔러보기가 사용자 행을 다른 순서로 잠그면 교착에 빠져서 UUID 오름차순으로 통일했습니다. 교환을 만드는 경로를 새로 추가하면 반드시 이 잠금을 거쳐야 합니다 |
| **거절 이력이 영구 필터로 남습니다** | 후보 조회 쿼리가 `CANCELLED` 교환에 `REJECTED` 참가자가 있는 조합을 계속 제외합니다. 중복으로 막힌 찔러보기를 거절로 바꾸지 않고 409 만 돌려주는 것도 이 때문입니다 |
| **다른 기기나 탭에서 내 등록을 고치는 경합은 다루지 못했습니다** | 서버가 본인에게는 SSE 이벤트를 보내지 않아서, 화면에 들어올 때 다시 읽는 것으로만 덮입니다 |
| [**#99**](https://github.com/softeerbootcamp-8th/HACKATHON-TEAM1-helloiamoneteamnicetomeetyou/issues/99) **아무도 수락하지 않은 매칭 제안이 만료되지 않습니다** | 제안이 그대로 남아 두 사람을 계속 묶어 둡니다 |

<br>

## 팀 구성

| 이름 | GitHub | 맡은 것 |
| --- | --- | --- |
| **기승민** | [@KiSeungMin](https://github.com/KiSeungMin) | 개발 리더, SSE 실시간 연결, 어드민 콘솔 |
| **유승종** | [@bigbell999](https://github.com/bigbell999) | 교환 대기존과 찔러보기, 지정 교환장소, 배포 파이프라인 |
| **최서지** | [@choiseoji](https://github.com/choiseoji) | 자동 매칭(1:1, 삼자), 매칭 수락과 거절, 알림 |

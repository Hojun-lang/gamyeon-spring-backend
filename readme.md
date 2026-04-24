
# 가면 (Gamyeon) — AI 모의 면접 서비스

> 이력서 기반 맞춤 질문 생성부터 음성 답변 분석, 종합 리포트까지 — AI가 실전 면접을 준비시켜 드립니다.

***

## 📌 프로젝트 개요

가면(Gamyeon)은 AI 기반 모의 면접 서비스입니다.  
사용자가 이력서와 자기소개서를 업로드하면, AI가 직군에 맞는 맞춤형 질문을 생성하고, 음성 답변을 실시간 분석하여 점수·강점·개선점을 담은 종합 리포트를 제공합니다.

### 핵심 기능

| 기능 | 설명 |
|---|---|
| **맞춤 질문 생성** | 이력서/자소서 기반 CUSTOM 4개 + 직군 공통 COMMON 3개 = 총 7문항 |
| **음성 답변 분석** | Whisper STT + 비언어적 행동 분석 (시선, 자세) |
| **질문별 피드백** | GPT-4o 기반 점수, 특성, 강점, 개선점 생성 |
| **종합 리포트** | 전 문항 피드백 집계 → 종합 점수, 요약, 강점/개선 요약 |
| **면접 기록** | 히스토리 카드 형태로 세션별 상태 및 결과 조회 |

***

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                      Client (Frontend)                  │
└──────────────────────────┬──────────────────────────────┘
                           │ REST API
┌──────────────────────────▼──────────────────────────────┐
│              Spring Server (gamyeon-spring-backend)     │
│   데이터 파이프라인 · 인증 · 오케스트레이션 · DB 관리      │
└────────────┬─────────────────────────────┬──────────────┘
             │ Webhook (비동기)             │ PostgreSQL
             ▼                             ▼
┌────────────────────────┐     ┌──────────────────────────┐
│  Python AI Server      │     │       PostgreSQL         │
│  (gamyeon-python-      │     │   intv_session           │
│   backend)             │     │   intv_question          │
│  - media/ (STT)        │     │   feedback               │
│  - feedback/ (GPT-4o)  │     │   report                 │
│  - report/ (집계)      │     │   user, ...              │
│  - question/ (LLM)     │     └──────────────────────────┘
└────────────────────────┘
```


***

## 🛠️ 기술 스택

### Spring 서버

| 분류 | 기술 |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| ORM | Spring Data JPA |
| Database | PostgreSQL |
| 외부 통신 | OpenFeign (Python AI 서버) |
| 인증 | JWT + OAuth2 (Google, Kakao) |
| 빌드 | Gradle |
| 배포 | AWS EC2, Docker |
| API 문서 | Swagger / OpenAPI |

### Python AI 서버

| 분류 | 기술 |
|---|---|
| Framework | FastAPI |
| AI/LLM | LangChain, GPT-4o |
| STT | OpenAI Whisper |
| 아키텍처 | 헥사고날 아키텍처 (feature 단위 모듈화) |

***




## 🗂️ 유비쿼터스 언어

| 용어 | 설명 |
|---|---|
| `intv` | 면접 세션 (interview) |
| `feedback` | 질문별 AI 평가 결과 |
| `report` | 종합 면접 리포트 |
| `question` | 면접 질문 |
| `nonverbal` | 비언어적 행동 분석 (시선, 자세 등) |
| `basepose` | AI 시선/자세 추적을 위한 기준 자세 (영점) |
| `stt_status` | 음성 → 텍스트 변환 상태 (PENDING/COMPLETED/FAILED) |
| `job_role` | 직군 (BACKEND, FRONTEND, DEVOPS 등) |

***


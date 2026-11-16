# Gamyeon | AI模擬面接プラットフォーム

**Java 17 · Spring Boot 3.5.11 · Backend Development Portfolio**

> 🇰🇷 **한국어로 읽으시는 분께:** 이 README는 일본 취업용 포트폴리오를 목적으로 일본어를 먼저 작성했습니다. 한국어 설명은 문서 아래쪽의 **[한국어 버전 바로가기](#korean-version)**에서 확인해 주세요.
>
> 🇯🇵 **日本語版を先に掲載しています。** 韓国語版は[こちら](#korean-version)です。

---

<a id="japanese-version"></a>

## 🇯🇵 日本語版

### はじめに

**Gamyeon** は、履歴書・ポートフォリオ・自己紹介書をもとに、AIによる面接質問の生成、回答分析、質問別フィードバック、総合レポートの提供を行う模擬面接Webサービスです。

本サービスは**複数のメンバーによるチーム開発**で制作しました。私は **Spring Bootバックエンドチームの一員**として、面接の進行管理、回答データの処理、AIサーバーとの連携、および既存のCallback処理の改善などを担当しました。

> **本リポジトリについて**  
> チーム開発のSpring BootバックエンドをForkした、個人の開発実績紹介用リポジトリです。以下では **サービス全体の機能・技術** と **私自身が実装・改善した箇所** を明確に分けています。Python AIサーバー、フロントエンド、バックオフィス、およびSpringバックエンドの一部機能は他のメンバーが担当しました。PRの原文は韓国語のため、担当変更を日本語で要約しています。

### 1. プロジェクト概要

| 項目 | 内容 |
|---|---|
| 開発形態 | チーム開発 |
| 対象リポジトリ | Spring Bootバックエンド |
| 私の担当 | 面接・質問・回答・準備資料関連のAPI、AI連携処理の改善、Callback処理のリファクタリング、退会・復旧機能など |
| 主な目的 | 模擬面接の実施とAIによる振り返り支援 |

**主な機能（サービス全体）**

| 機能 | 概要 |
|---|---|
| ユーザー認証 | OAuth2ログインとJWTによる認証 |
| 面接準備 | 履歴書などの資料をS3へアップロード |
| 質問生成 | AIカスタム質問（最大4問）と共通質問を組み合わせ、基本的に7問を構成 |
| 模擬面接 | 回答動画の登録、面接の一時停止・再開、進捗管理 |
| 回答分析 | Python AIサーバーへのSTT等の分析依頼と結果受信 |
| フィードバック | 質問別の評価結果の保存 |
| 総合レポート | 面接結果の集約・閲覧 |
| アカウント管理 | 退会・一定期間内の復旧、期限経過後の削除 |


### サービス画面

以下は、実際に公開したGamyeonサービスの利用画面です。
面接準備からAIによる分析レポートの確認まで、一連の流れを紹介します。

※画面はチーム全体で開発したサービスのものです。
各画面に関連する私のバックエンド担当箇所も記載しています。

#### 1. 面接準備資料のアップロード

<img width="1470" height="956" alt="Pasted Graphic 11" src="https://github.com/user-attachments/assets/eaf1030c-715e-4da8-89e1-39dda2544dd6" />

履歴書やポートフォリオなどの面接準備資料を登録する画面です。

私はSpring Boot側で、S3 Presigned URLの発行とファイル情報の登録APIを実装しました。

**担当領域：** Preparation / AWS S3

---

#### 2. AI面接の質問・回答

<img width="1470" height="952" alt="Pasted Graphic 17" src="https://github.com/user-attachments/assets/e159e4f2-c814-46a5-b5b5-89f66fbb54a4" />

AIが生成した質問を確認し、動画で回答する画面です。

私は質問生成依頼API、質問セット管理、回答データの登録処理などを担当しました。また、AIへの質問生成依頼に失敗した場合、共通質問を利用するFallback処理も実装しました。

**担当領域：** Question / Answer

---

#### 3. 回答データの登録とAI分析連携

<img width="1470" height="956" alt="Pasted Graphic 18" src="https://github.com/user-attachments/assets/bba24c50-c4ec-4130-9837-abad8cb2bfb8" />

回答動画を登録した後、バックエンドからAIサーバーへ分析を依頼する処理につながります。

私は`AnswerAnalysisJob`を導入し、分析依頼の送信状態、再試行、滞留Jobの復旧を管理できるように改善しました。

**担当領域：** Answer / 非同期処理

**関連PR：** [#14 — 回答分析処理の改善](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/14)

---

#### 4. 面接終了・分析待機

<img width="1470" height="956" alt="Pasted Graphic 19" src="https://github.com/user-attachments/assets/e860a7de-10d7-471c-8de6-0e862e2faa3d" />

すべての質問への回答を終えた後、AIによる分析結果を待つ画面です。

私は面接の進行状態管理と終了処理を実装・改善しました。面接終了後のAIレポート生成処理は、他のチームメンバーが担当しました。

**担当領域：** Interview / 状態管理

---

#### 5. AI面接分析レポート

<img width="1470" height="723" alt="Pasted Graphic 23" src="https://github.com/user-attachments/assets/c83c118d-58f8-47e1-9d18-138e693d93b6" />

面接終了後、AIが生成した総合評価や改善点を確認できる画面です。

レポート生成ロジックは他のチームメンバーが担当しました。私は面接一覧からレポートの要約情報を取得する処理の改善に携わりました。

**担当領域：** 面接一覧向けレポート要約照会

**関連PR：** [#29 — 面接進行・一覧表示の改善](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/29)

---


### 2. システム構成

```text
             Frontend / Backoffice
                       |
                    REST API
                       |
             Spring Boot Backend
             /         |         \
       OAuth/JWT   Interview/API   S3 Presigned URL
                       |                 |
                 外部AI連携             AWS S3
                       |
                 Python AI Server
                       |
                非同期Callback
                       |
             Spring Boot Backend
                       |
                   PostgreSQL
```

この図はサービス全体の概念的な構成です。Python側でのAI処理そのものは担当していません。

Springバックエンドはドメインごとに機能を分割し、**一部のモジュールでPort / Adapterによる依存分離**を取り入れています。すべてのモジュールが同じ構造ではないため、完全なヘキサゴナルアーキテクチャとは表現していません。

- **Domain:** 状態・ビジネスルール
- **Application:** ユースケースと処理の調整
- **Port:** 永続化や外部サービスへの依存の抽象化
- **Adapter / Infrastructure:** JPA、Web、Feign、S3などの実装

### 3. 技術スタック（プロジェクト全体）

| 分類 | 技術 |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.5.11 |
| Database / ORM | PostgreSQL, Spring Data JPA, Hibernate |
| Authentication | Spring Security, OAuth2, JWT |
| AI連携 | OpenFeign, Callback, Job / Scheduler |
| Storage | AWS S3, Presigned URL |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Build / Deploy | Gradle, Docker, AWS EC2 |
| Collaboration | Git, GitHub, Pull Request |

※上記はチーム全体の技術構成です。**OAuth/JWT基盤、AIレポート生成処理、CI/CDなどを私が単独で構築したという意味ではありません。**

### 4. 私の実装・改善実績

**主な担当範囲**

- 面接作成、進行状態、一時停止・再開、回答進捗・次の質問の取得に関する機能の実装・改善
- 準備資料の登録・アップロードAPI、質問生成リクエストと質問セット管理に関する実装・改善
- AIへの回答分析依頼をJobとして管理する仕組み、スケジューラーによる送信・再試行・滞留Jobの復旧
- **既存のFeedback機能を土台とした** Callback受付・保存処理の分離と冪等性の強化
- S3に保存する回答動画のキー構造改善
- **既存の認証基盤に対する追加機能として** 退会・猶予期間・復旧・期限経過後の削除処理を実装
- 面接一覧向けのレポート**要約照会**の改善（AIレポート生成ロジックそのものは担当外）

**主なPR・コミット（本人の変更履歴）**

| 記録 | 対象 | 主な変更内容 |
|---|---|---|
| [PR #14](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/14) | Answer | `AnswerAnalysisJob`導入、分析依頼の送信・再試行管理、スケジューラー、滞留Job復旧、関連テスト |
| [PR #17](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/17) | Feedback | 既存Callback処理をJobベースへリファクタリング、重複受付防止、限定的な再試行、関連テスト |
| [PR #29](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/29) | Interview | 一時停止・再開、回答進捗と次の質問の照会、質問順序、一覧・レポート要約照会の改善 |
| [PR #32](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/32) | Answer / S3 | 回答動画の保存先を`questionSetId`基準から`intvId`基準へ変更 |
| [PR #41](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/41) | User | 退会・7日間の復旧猶予・アカウント復旧・期限経過後のデータ削除処理 |
| [commit `88e0ae9`](https://github.com/team-gamyeon/gamyeon-spring-backend/commit/88e0ae97) | Question | AI質問生成リクエスト失敗時の共通質問によるFallbackと質問セット重複生成の防止 |
| [commit `b1cf305`](https://github.com/team-gamyeon/gamyeon-spring-backend/commit/b1cf305) / [commit `8afaf4c`](https://github.com/team-gamyeon/gamyeon-spring-backend/commit/8afaf4c4) | Preparation | 資料アップロード・登録APIと複数資料の登録処理 |

※PRにはチーム内で調整したファイルも含まれます。**表の内容は各PRで確認できた私の変更内容の要約**であり、関係する全モジュールの単独開発を意味しません。

### 5. 技術的な課題と改善

#### Case 1 — 回答分析依頼のJob化と再試行（PR #14）

**課題:** 回答の分析結果と外部AIサーバーへの送信・再試行状態を混在させると、通信障害や処理途中での停止から復旧しにくくなります。

**対応:** `AnswerAnalysisJob`を導入し、リクエスト送信状態、再試行回数、次回再試行時刻、エラー情報を管理しました。送信処理とDBトランザクション処理を分け、スケジューラーで未送信Jobを処理する構造へ改善しました。

Feignの再試行可能な例外、HTTP **429 / 5xx** などは再試行対象とし、長時間`SENDING`のまま残るJobには復旧処理を設けました。**すべてのエラーを一律に再試行するものではありません。**

**結果:** 回答データの分析結果状態と外部送信処理の責務を分離し、障害発生時の再試行・復旧状態を追跡できる構造にしました。

**関連コード:** `AnswerAnalysisJob`, `AnswerAnalysisDispatchService`, `AnswerAnalysisDispatchTransactionService`, `AnswerAnalysisScheduler`

#### Case 2 — Feedback Callbackの重複防止と復旧（PR #17）

**課題:** AIサーバーから同じCallbackが再送された場合、同じ質問に対するフィードバックが二重保存される可能性があります。既存のFeedback機能を改善し、重複と並行リクエストを考慮した処理が必要でした。

**対応:**

1. `requestId`と`questionSetId`で受信済みJobを確認し、DBのUnique制約でも重複登録を防止
2. Callbackの内容を`FeedbackCallbackJob`として先に保存し、受信処理と実際の保存処理を分離
3. スケジューラーの処理対象取得に悲観的ロックを使用
4. Feedback保存時も既存データ確認・`questionSetId`のUnique制約を利用
5. DBロック失敗など限定された例外で再試行し、長時間`PROCESSING`状態のJobを復旧
6. 新しいFeedbackが保存された場合にのみ後続イベントを発行

**結果:** 再送や並行リクエストによる二重保存を防ぐための多段階の防御を実装し、処理状態・失敗内容を確認しやすくしました。

**関連コード:** `FeedbackWebhookService`, `FeedbackCallbackJob`, `FeedbackCallbackProcessingTransactionService`

#### Case 3 — 面接の一時停止・再開と進捗復元（PR #29）

**課題:** 面接の中断後に再開する際、状態の変更だけでなく、回答済みの質問・次の質問・回答順序を整合的に扱う必要がありました。

**対応:** 面接の状態遷移を整理し、`getResumeContext()`から回答進捗や次の質問を取得する構造を改善しました。質問順序を明示的に扱い、回答順序の検証や一覧表示も見直しました。面接ドメインでは一時停止時間を集計し、経過時間から差し引く処理もあります。

**結果:** 再開時に進捗状況を把握し、面接を継続するための情報を提供できるようにしました。

**関連コード:** `Intv`, `IntvApplicationService`, `ResumeContextInfo`

#### Case 4 — AI質問生成失敗時のFallback（commit `88e0ae9`）

**課題:** 外部AIサーバーへの質問生成依頼に失敗すると、面接を開始できなくなる可能性がありました。

**対応:** 依頼時に例外が発生した場合、AIカスタム質問を0件として共通質問から質問セットを構成する代替フローを追加しました。既存セットの重複生成を防ぐチェックも追加しました。

**結果:** AI依頼の失敗時にも、登録済みの共通質問を利用して面接用質問を構成できるようにしました。※必要数の共通質問データが登録されていることが前提です。

**関連コード:** `QuestionSetApplicationService`

#### Case 5 — 回答動画のS3保存先整理（PR #32）

**課題:** 質問ID単位のパスでは、同じ面接の複数動画が分散し、管理しにくい状態でした。

**対応:** クライアントAPIが受け取る`questionSetId`は維持し、サーバー内で`intvId`を取得して保存先を変更しました。

```text
Before: answers/{questionSetId}/video/{uuid}-{fileName}
After : answers/{intvId}/{uuid}-{fileName}
```

**結果:** フロントエンドのAPI契約を変更せず、面接単位でファイルを確認しやすい構造にしました。

### 6. チーム開発, 協力

機能ごとのブランチ、Notion, Slack, Pull Request、レビューを通じてチームメンバーと連携しました。担当モジュール以外にも既存コードを調査し、APIの接続部分や状態の受け渡しを調整しました。
詳細なドキュメントの整備と積極的なコミュニケーションを通じて、チーム内の認識共有を図り、円滑な共同開発に努めました。

**API仕様書（Notion）**

<img width="1470" height="848" alt="Pasted Graphic 28" src="https://github.com/user-attachments/assets/9b760374-e9b1-4736-8c8d-c933d723afaf" />

[API仕様書を見る](https://flossy-jasper-003.notion.site/API-a843e846003c82889c558162e08d5a72)

<img width="1468" height="719" alt="가면 ver2" src="https://github.com/user-attachments/assets/90f9548f-1ac6-4338-8352-b8848569616a" />
チームNotion

### 7. 関連リポジトリ

本プロジェクトは、複数のチームが連携して開発したAI模擬面接プラットフォームです。

| 領域 | リポジトリ |
|---|---|
| Spring Boot Backend（個人Fork） | [Hojun-lang/gamyeon-spring-backend](https://github.com/Hojun-lang/gamyeon-spring-backend) |
| Spring Boot Backend（チーム開発元） | [team-gamyeon/gamyeon-spring-backend](https://github.com/team-gamyeon/gamyeon-spring-backend) |
| AI Server | [team-gamyeon/gamyeon-AI](https://github.com/team-gamyeon/gamyeon-AI) |
| Frontend（Next.js） | [team-gamyeon/gamyeon-nextjs-frontend](https://github.com/team-gamyeon/gamyeon-nextjs-frontend) |
| Backoffice（Next.js） | [team-gamyeon/gamyeon-nextjs-backoffice](https://github.com/team-gamyeon/gamyeon-nextjs-backoffice) |
| Backoffice Backend（NestJS） | [team-gamyeon/gamyeon-nestjs-backoffice](https://github.com/team-gamyeon/gamyeon-nestjs-backoffice) |

**担当範囲について**

私の主な担当はSpring Bootバックエンドの開発です。

AIサーバー、フロントエンド、バックオフィスの開発にも、ペアプログラミングや仕様調整、技術的な意見交換などを通じて協力しましたが、これらの領域における実装の中心は他のチームメンバーが担当しました。

本READMEでは、チーム全体の成果と私自身の実装・改善実績を区別して紹介しています。

---

<a id="korean-version"></a>

## 🇰🇷 한국어 버전

[日本語版に戻る / 일본어 버전으로 돌아가기](#japanese-version)

### 소개

**Gamyeon**은 이력서·포트폴리오·자기소개서를 기반으로 AI 면접 질문 생성, 답변 분석, 질문별 피드백, 종합 리포트를 제공하는 모의면접 웹 서비스입니다.

이 서비스는 **여러 팀원이 공동 개발**했습니다. 저는 **Spring Boot 백엔드 팀의 일원**으로 면접 진행 관리, 답변 데이터 처리, AI 서버 연동, 기존 콜백 처리 구조 개선 등을 담당했습니다.

> **이 저장소에 대해**  
> 팀이 개발한 Spring Boot 백엔드를 Fork한 개인 개발 실적 소개용 저장소입니다. 아래에서는 **서비스 전체 기능·기술**과 **제가 직접 구현·개선한 부분**을 구분합니다. Python AI 서버, 프론트엔드, 백오피스뿐 아니라 Spring 백엔드 일부 기능도 다른 팀원이 담당했습니다. 기존 PR은 한국어로 작성되었으며, 위 일본어 섹션에는 해당 변경 내용을 일본어로 요약했습니다.

### 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 개발 형태 | 팀 프로젝트 |
| 대상 저장소 | Spring Boot 백엔드 |
| 개인 담당 | 면접·질문·답변·준비자료 관련 API, AI 연동 개선, 콜백 리팩토링, 회원 탈퇴·복구 기능 등 |
| 주요 목적 | 모의면접과 AI 기반 면접 피드백 제공 |

**주요 기능(서비스 전체)**

| 기능 | 개요 |
|---|---|
| 사용자 인증 | OAuth2 로그인 및 JWT 인증 |
| 면접 준비 | 이력서 등 준비자료 S3 업로드 |
| 질문 생성 | AI 맞춤 질문 최대 4개와 공통 질문을 조합해 기본 7문항 구성 |
| 모의면접 | 답변 영상 등록, 일시정지·재개, 진행 상황 관리 |
| 답변 분석 | Python AI 서버에 STT 등 분석 의뢰 및 결과 수신 |
| 피드백 | 질문별 평가 결과 저장 |
| 종합 리포트 | 면접 평가 집계 및 조회 |
| 계정 관리 | 탈퇴·유예기간 내 복구 및 기간 경과 후 삭제 |


### 서비스 화면

아래는 실제 배포한 Gamyeon 서비스의 이용 화면입니다.
면접 준비부터 AI 분석 리포트 확인까지의 전체 과정을 소개합니다.

※ 화면은 팀 전체가 공동 개발한 서비스의 결과물입니다.
각 화면과 관련된 저의 백엔드 담당 업무도 함께 설명합니다.

#### 1. 면접 준비자료 업로드

<img width="1470" height="956" alt="Pasted Graphic 12" src="https://github.com/user-attachments/assets/b7eb4487-652c-4e20-b4f6-e5849732d344" />

이력서와 포트폴리오 등 면접 준비자료를 등록하는 화면입니다.

저는 Spring Boot에서 S3 Presigned URL 발급과 파일 정보 등록 API를 구현했습니다.

**담당 영역:** Preparation / AWS S3

---

#### 2. AI 면접 질문 및 답변

<img width="1470" height="952" alt="Pasted Graphic 17" src="https://github.com/user-attachments/assets/3827a61e-27f4-4d7f-bc87-b7ae531a9538" />

AI가 생성한 질문을 확인하고 영상으로 답변하는 화면입니다.

저는 질문 생성 요청 API, 질문 세트 관리, 답변 데이터 등록 처리 등을 담당했습니다. 또한 AI 질문 생성 요청이 실패할 경우 공통 질문을 활용하는 Fallback 처리도 구현했습니다.

**담당 영역:** Question / Answer

---

#### 3. 답변 데이터 등록 및 AI 분석 연동

<img width="1470" height="956" alt="Pasted Graphic 18" src="https://github.com/user-attachments/assets/f1572dac-62aa-42b9-b774-9037ced4511a" />

답변 영상을 등록한 후 백엔드에서 AI 서버로 분석을 요청하는 과정과 연결되는 화면입니다.

저는 `AnswerAnalysisJob`을 도입하여 분석 요청의 전송 상태, 재시도, 장기 정체 Job 복구를 관리하도록 개선했습니다.

**담당 영역:** Answer / 비동기 처리

**관련 PR:** [#14 — 답변 분석 처리 개선](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/14)

---

#### 4. 면접 완료 및 분석 대기

<img width="1470" height="956" alt="Pasted Graphic 19" src="https://github.com/user-attachments/assets/ff57b93a-10a4-40a3-98e3-bbfb4ba4bc68" />

모든 질문에 대한 답변을 마친 뒤 AI 분석 결과를 기다리는 화면입니다.

저는 면접 진행 상태 관리와 종료 처리를 구현·개선했습니다. 면접 종료 후 AI 리포트 생성 처리는 다른 팀원이 담당했습니다.

**담당 영역:** Interview / 상태 관리

---

#### 5. AI 면접 분석 리포트

<img width="1470" height="723" alt="Pasted Graphic 23" src="https://github.com/user-attachments/assets/04f7dc5d-a15f-4443-bd74-5895e69a0bc1" />

면접 종료 후 AI가 생성한 종합 평가와 개선점을 확인할 수 있는 화면입니다.

리포트 생성 로직은 다른 팀원이 담당했습니다. 저는 면접 목록에서 리포트의 요약 정보를 조회하는 처리의 개선에 참여했습니다.

**담당 영역:** 면접 목록용 리포트 요약 조회

**관련 PR:** [#29 — 면접 진행 및 목록 조회 개선](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/29)

---


### 2. 시스템 구성

```text
             Frontend / Backoffice
                       |
                    REST API
                       |
             Spring Boot Backend
             /         |         \
        OAuth/JWT    면접/API     S3 Presigned URL
                       |                 |
                  외부 AI 연동          AWS S3
                       |
                Python AI Server
                       |
                  비동기 Callback
                       |
             Spring Boot Backend
                       |
                   PostgreSQL
```

이 그림은 서비스 전체의 개념적 구성도이며, Python AI 처리 자체는 제 담당이 아니었습니다.

Spring 백엔드는 도메인별로 모듈을 나누고, **일부 모듈에 Port / Adapter 패턴**을 적용해 외부 의존성을 분리했습니다. 모든 모듈이 동일한 구조는 아니므로 완전한 헥사고날 아키텍처라고 주장하지 않습니다.

- **Domain:** 상태와 비즈니스 규칙
- **Application:** 유스케이스와 비즈니스 처리 조정
- **Port:** DB 및 외부 API 의존성 추상화
- **Adapter / Infrastructure:** JPA, Web, Feign, S3 구현

### 3. 기술 스택(프로젝트 전체)

| 분류 | 기술 |
|---|---|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.5.11 |
| DB / ORM | PostgreSQL, Spring Data JPA, Hibernate |
| 인증 | Spring Security, OAuth2, JWT |
| AI 연동 | OpenFeign, Callback, Job / Scheduler |
| 저장소 | AWS S3, Presigned URL |
| 테스트 | JUnit 5, Mockito, Spring Boot Test |
| 빌드 / 배포 | Gradle, Docker, AWS EC2 |
| 협업 | Git, GitHub, Pull Request |

※ 팀 전체 기술 스택이며, **OAuth/JWT 인증 기반, AI 리포트 생성 로직, CI/CD를 제가 단독 구현했다는 뜻이 아닙니다.**

### 4. 나의 구현·개선 실적

**주요 담당 업무**

- 면접 생성·진행 상태·일시정지·재개, 답변 진도와 다음 질문 조회 관련 기능 구현·개선
- 준비자료 등록 및 업로드 API, 질문 생성 요청과 질문 세트 관리 구현·개선
- AI 답변 분석 요청을 Job으로 관리하고 스케줄러를 통해 전송·재시도·장기 정체 Job 복구
- **기존 Feedback 기능을 바탕으로** 콜백 접수와 저장 처리 분리, 멱등성 강화
- S3 답변 영상의 저장 키 구조 개선
- **기존 인증 기반에 기능을 추가하는 방식으로** 회원 탈퇴·유예기간·복구·기간 경과 후 삭제 구현
- 면접 목록용 **리포트 요약 조회** 개선(AI 리포트 생성 자체는 담당하지 않음)

**주요 PR·커밋(본인 변경 이력)**

| 기록 | 대상 | 주요 변경 내용 |
|---|---|---|
| [PR #14](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/14) | Answer | `AnswerAnalysisJob` 도입, 분석 요청 전송·재시도, 스케줄러, 장기 정체 Job 복구, 관련 테스트 |
| [PR #17](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/17) | Feedback | 기존 콜백 처리의 Job 기반 리팩토링, 중복 접수 방지, 제한적 재시도, 관련 테스트 |
| [PR #29](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/29) | Interview | 일시정지·재개, 진행도·다음 질문 조회, 질문 순서, 목록 및 리포트 요약 조회 개선 |
| [PR #32](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/32) | Answer / S3 | 답변 영상 저장 경로를 `questionSetId` 기준에서 `intvId` 기준으로 변경 |
| [PR #41](https://github.com/team-gamyeon/gamyeon-spring-backend/pull/41) | User | 탈퇴, 7일 복구 유예기간, 계정 복구, 기간 경과 후 데이터 삭제 |
| [커밋 `88e0ae9`](https://github.com/team-gamyeon/gamyeon-spring-backend/commit/88e0ae97) | Question | AI 질문 생성 요청 실패 시 공통 질문 Fallback 및 질문 세트 중복 생성 방지 |
| [커밋 `b1cf305`](https://github.com/team-gamyeon/gamyeon-spring-backend/commit/b1cf305) / [커밋 `8afaf4c`](https://github.com/team-gamyeon/gamyeon-spring-backend/commit/8afaf4c4) | Preparation | 준비자료 업로드·등록 API 및 복수 자료 등록 처리 |

※ 각 PR에는 협업 과정에서 수정한 파일도 포함됩니다. **표는 확인된 본인 변경 내용을 요약한 것**이며, 관련 모듈 전체를 단독 개발했다는 의미가 아닙니다.

### 5. 기술적 문제와 개선 사례

#### Case 1 — 답변 분석 요청 Job화와 재시도(PR #14)

**문제:** 답변의 분석 결과 상태와 AI 서버에 요청을 보내는 상태를 함께 관리하면, 통신 실패나 처리 중단 후 복구가 어려워집니다.

**대응:** `AnswerAnalysisJob`을 도입해 요청 전송 상태, 재시도 횟수, 다음 재시도 시각, 오류 정보를 관리했습니다. 전송과 DB 트랜잭션 처리를 분리하고, 스케줄러가 미전송 Job을 처리하도록 개선했습니다.

Feign 재시도 가능 예외와 HTTP **429 / 5xx** 등을 재시도 대상으로 분류하고, 오래 `SENDING` 상태인 Job을 복구하도록 했습니다. **모든 오류를 무조건 재시도하지는 않습니다.**

**결과:** 답변 분석 결과와 외부 요청 전송의 책임을 분리하고, 실패 시 재시도·복구 상태를 추적할 수 있게 했습니다.

**관련 코드:** `AnswerAnalysisJob`, `AnswerAnalysisDispatchService`, `AnswerAnalysisDispatchTransactionService`, `AnswerAnalysisScheduler`

#### Case 2 — Feedback Callback 중복 방지 및 복구(PR #17)

**문제:** AI 서버가 같은 콜백을 재전송할 경우 동일 질문에 대한 피드백이 중복 저장될 가능성이 있습니다. 기존 Feedback 기능을 개선해 중복 및 동시 요청에 대응할 필요가 있었습니다.

**대응:**

1. `requestId`, `questionSetId`로 기존 Job을 조회하고 DB Unique 제약으로 중복 등록 방어
2. 콜백 내용을 먼저 `FeedbackCallbackJob`에 저장해 접수와 실제 처리 분리
3. 스케줄러의 Job 조회에 비관적 잠금 사용
4. 피드백 저장 시에도 기존 데이터 조회 및 `questionSetId` Unique 제약 활용
5. DB 잠금 실패 등 제한된 예외만 재시도하고 장시간 `PROCESSING` Job 복구
6. 새로운 피드백이 실제 저장됐을 때만 후속 이벤트 발행

**결과:** 재전송·동시 요청으로 인한 중복 저장에 여러 단계의 방어를 적용하고, 처리 상태와 오류 정보를 확인할 수 있도록 개선했습니다.

**관련 코드:** `FeedbackWebhookService`, `FeedbackCallbackJob`, `FeedbackCallbackProcessingTransactionService`

#### Case 3 — 면접 일시정지·재개와 진행 상황 복원(PR #29)

**문제:** 면접 도중 중단 후 재개할 때 상태 변경뿐 아니라 완료한 질문, 다음 질문, 답변 순서를 일관되게 다뤄야 했습니다.

**대응:** 상태 전이를 정리하고 `getResumeContext()`에서 답변 진행 상황과 다음 질문을 조회하도록 개선했습니다. 질문 순서를 명시적으로 관리하고 답변 순서 검증과 목록 조회도 보완했습니다. 면접 도메인에는 일시정지 시간을 누적해 실제 진행 시간에서 차감하는 처리도 존재합니다.

**결과:** 다시 시작할 때 필요한 진행 정보와 다음 질문을 제공하는 구조를 마련했습니다.

**관련 코드:** `Intv`, `IntvApplicationService`, `ResumeContextInfo`

#### Case 4 — AI 질문 생성 실패 시 Fallback(커밋 `88e0ae9`)

**문제:** 외부 AI 서버에 질문 생성을 요청하는 과정에서 오류가 발생하면 면접 진행이 막힐 수 있었습니다.

**대응:** 요청 중 예외가 발생하면 AI 맞춤 질문을 0개로 취급하고, DB의 공통 질문으로 질문 세트를 구성하는 대체 흐름을 추가했습니다. 이미 생성된 질문 세트를 중복 생성하지 않도록 검사도 추가했습니다.

**결과:** AI 요청 실패 시에도 등록된 공통 질문을 이용해 면접용 질문을 구성할 수 있도록 했습니다. ※ 필요한 수의 공통 질문이 DB에 등록되어 있어야 합니다.

**관련 코드:** `QuestionSetApplicationService`

#### Case 5 — S3 답변 영상 저장 경로 정리(PR #32)

**문제:** 질문 ID를 기준으로 영상 경로를 만들면 하나의 면접에 속한 영상이 여러 위치에 분산돼 관리가 불편했습니다.

**대응:** 클라이언트가 전달하는 `questionSetId`는 그대로 유지하고, 서버에서 `intvId`를 조회해 경로를 변경했습니다.

```text
변경 전: answers/{questionSetId}/video/{uuid}-{fileName}
변경 후: answers/{intvId}/{uuid}-{fileName}
```

**결과:** 기존 프론트엔드 API 계약을 유지하면서 면접 단위로 영상을 확인하기 쉽게 개선했습니다.

### 6. 협업

기능별 브랜치, Notion, Slack, Pull Request, 코드 리뷰를 통해 팀원과 협업했습니다. 담당 영역 외의 코드도 확인하며 API 연동과 상태 전달을 조정했습니다.

**API 명세서(Notion)**
<img width="1470" height="848" alt="Pasted Graphic 28" src="https://github.com/user-attachments/assets/8a01ab93-969c-417b-af7b-9e41f5790790" />

[API 명세서 확인하기](https://flossy-jasper-003.notion.site/API-a843e846003c82889c558162e08d5a72)

<img width="1468" height="719" alt="가면 ver2" src="https://github.com/user-attachments/assets/793be659-ad2e-4bac-9951-907e8e3ccbfb" />
팀 노션

### 7. 관련 저장소

본 프로젝트는 여러 팀이 협력하여 개발한 AI 모의면접 플랫폼입니다.

| 영역 | 저장소 |
|---|---|
| Spring Boot Backend (개인 Fork) | [Hojun-lang/gamyeon-spring-backend](https://github.com/Hojun-lang/gamyeon-spring-backend) |
| Spring Boot Backend (팀 원본) | [team-gamyeon/gamyeon-spring-backend](https://github.com/team-gamyeon/gamyeon-spring-backend) |
| AI Server | [team-gamyeon/gamyeon-AI](https://github.com/team-gamyeon/gamyeon-AI) |
| Frontend (Next.js) | [team-gamyeon/gamyeon-nextjs-frontend](https://github.com/team-gamyeon/gamyeon-nextjs-frontend) |
| Backoffice (Next.js) | [team-gamyeon/gamyeon-nextjs-backoffice](https://github.com/team-gamyeon/gamyeon-nextjs-backoffice) |
| Backoffice Backend (NestJS) | [team-gamyeon/gamyeon-nestjs-backoffice](https://github.com/team-gamyeon/gamyeon-nestjs-backoffice) |

**담당 범위에 대하여**

저의 주요 담당 영역은 Spring Boot 백엔드 개발입니다.

AI 서버, 프론트엔드, 백오피스 개발 과정에서도 페어 프로그래밍, 기능 명세 조율, 기술적 의견 교환 등을 통해 협업했으나, 해당 영역의 실질적인 구현은 다른 팀원들이 주도적으로 담당했습니다.

본 README에서는 팀 전체의 개발 성과와 제가 직접 구현·개선한 내용을 구분하여 소개합니다.

---

*본 README는 팀 전체의 개발 내용과 개인의 구현·개선 이력을 구분해 정리한 포트폴리오 문서입니다.*

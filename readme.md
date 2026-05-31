# Gamyeon Backend

Gamyeon は、履歴書・ポートフォリオ・自己紹介書をもとに AI が模擬面接を構成し、回答動画の STT 分析、質問別フィードバック、総合レポート生成まで支援する AI 面接練習サービスです。

このリポジトリは Spring Boot で実装されたバックエンドサーバーです。ユーザー認証、面接セッション管理、資料アップロード、質問生成依頼、回答動画管理、AI サーバーとの非同期連携、フィードバック保存、レポート生成トリガーを担当します。

## 主な機能

| 機能 | 説明 |
|---|---|
| OAuth ログイン | Google / Kakao OAuth 認証と JWT access token / refresh token の発行 |
| 面接セッション管理 | 面接の作成、タイトル変更、開始、一時停止、再開、終了、日別完了数の集計 |
| 面接準備資料管理 | 履歴書、ポートフォリオ、自己紹介書のアップロード URL 発行とメタデータ保存 |
| AI 質問生成 | 準備資料を AI サーバーへ渡し、カスタム質問と共通質問を組み合わせた質問セットを生成 |
| 回答動画管理 | 回答動画アップロード用 presigned URL の発行と回答メタデータの保存 |
| STT 分析連携 | 回答動画を AI サーバーへ分析依頼し、callback で STT 結果を保存 |
| 視線分析連携 | 回答中の gaze segment を AI サーバーへ転送 |
| 質問別フィードバック | AI サーバー callback を受け取り、質問ごとの評価結果を保存 |
| 総合レポート | フィードバックが揃ったタイミングで AI レポート生成を依頼し、総合評価を保存 |
| ダッシュボード | 面接統計、Notice 一覧・詳細 API |

## サービスフロー

```text
1. ユーザーが OAuth でログイン
        |
2. 面接セッションを作成
        |
3. 履歴書などの準備資料を S3 にアップロード
        |
4. Spring サーバーが AI サーバーへ質問生成を依頼
        |
5. AI サーバー callback により質問セットを保存
        |
6. ユーザーが質問ごとに回答動画を S3 にアップロード
        |
7. Spring サーバーが STT / gaze 分析を AI サーバーへ依頼
        |
8. AI サーバー callback により回答分析・フィードバックを保存
        |
9. 面接終了イベントとフィードバック保存イベントをもとにレポート生成をトリガー
        |
10. AI サーバー callback により総合レポートを保存
```

## アーキテクチャ

このプロジェクトは、ドメインごとに責務を分離した Layered Architecture / Hexagonal Architecture に近い構成を採用しています。

```text
src/main/java/com/gamyeon
├── user          # OAuth、JWT、ユーザープロフィール
├── intv          # 面接セッションのライフサイクル
├── preparation   # 準備資料と S3 アップロードフロー
├── question      # AI 質問、カスタム質問、共通質問の生成
├── answer        # 回答動画、STT、視線分析連携
├── feedback      # 質問別フィードバック callback
├── report        # 総合レポート生成と照会
├── dashboard     # 統計とお知らせ
└── common        # 共通レスポンス、例外、セキュリティ、ストレージ
```

### 設計上のポイント

- `domain` は状態遷移やビジネスルールを保持します。
- `application` はユースケースとトランザクション境界を管理します。
- `port` は外部依存を抽象化します。
- `adapter` / `infrastructure` は Web、JPA、Feign、S3 などの詳細実装を担当します。
- AI サーバー連携は callback ベースで非同期処理します。
- ファイルアップロードは Spring サーバーを経由せず、S3 presigned URL を利用します。

## 技術スタック

| 分類 | 技術 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Build | Gradle |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Auth | Spring Security, JWT, OAuth2 |
| External API | OpenFeign, WebFlux |
| Storage | AWS S3 presigned URL |
| JSONB | Hibernate `@JdbcTypeCode(SqlTypes.JSON)` |
| Test | JUnit 5, Mockito, Spring Security Test |
| Format | Spotless, google-java-format |
| Deploy | Docker |

## 主なドメイン

### User / Auth

Google または Kakao OAuth 認証コードを受け取り、外部 OAuth API からユーザー情報を取得します。ユーザーが存在しない場合は新規作成し、access token と refresh token を発行します。

refresh token は DB に保存します。再発行時には既存 refresh token を削除し、新しい token を発行することで、トークンのライフサイクルをサーバー側で管理します。

### Interview

`Intv` は面接セッションのライフサイクルを管理します。

```text
READY -> IN_PROGRESS -> PAUSED -> IN_PROGRESS -> FINISHED
```

面接終了時には `InterviewFinishedEvent` を発行します。Report モジュールはこのイベントを受け取り、レポート生成条件を確認します。

### Preparation

面接作成時に `Preparation` も同時に作成されます。ユーザーは履歴書、ポートフォリオ、自己紹介書をアップロードできます。

アップロードは次の流れで処理します。

```text
1. クライアントが presigned URL をリクエスト
2. サーバーがファイル種別、拡張子、サイズを検証
3. サーバーが S3 file key と presigned URL を返却
4. クライアントが S3 に直接アップロード
5. クライアントがアップロード完了後、ファイルメタデータをサーバーへ登録
```

必須資料である履歴書が登録されると、Preparation は `READY` 状態になります。

### Question

質問生成は AI サーバーへの非同期依頼として開始されます。AI サーバーが失敗した場合でも、共通質問を利用して最低限の質問セットを構成できる fallback フローを持っています。

質問セットは基本的に合計 7 問を目標に構成されます。

- AI カスタム質問: 最大 4 問
- 共通質問: 残りの質問数

### Answer

回答動画は S3 に保存され、DB には file key、file URL、content type、size、STT status などのメタデータを保存します。

回答の状態は次のように遷移します。

```text
UPLOADED -> STT_PROCESSING -> STT_COMPLETED
                         └-> STT_FAILED
```

回答動画のアップロード先は、質問 ID 単位ではなく面接 ID 単位で管理できるように設計しています。

```text
answers/{intvId}/{uuid}-{originalFileName}
```

この構造により、1 つの面接セッションに属する複数の回答動画を同じ S3 prefix 配下で確認できます。

### Feedback

AI サーバーが質問別フィードバックを callback で送信すると、Feedback モジュールは `questionSetId` から `intvId` を取得し、フィードバック結果を保存します。

すでに完了済みの feedback callback は重複処理しないよう、冪等性チェックを行います。

Feedback 保存後には `FeedbackSavedEvent` を発行し、Report モジュールがレポート生成可能かどうかを再確認します。

### Report

Report モジュールは次の 2 つのイベントを契機にレポート生成を試みます。

- `InterviewFinishedEvent`
- `FeedbackSavedEvent`

7 問すべてのフィードバックが成功すると、即座に AI レポート生成を依頼します。スケジューラートリガーでは、最小フィードバック数を基準に強制生成または失敗処理を行います。

AI サーバー callback で受け取った総合評価は `reports` テーブルに保存されます。

## 主な API

### Auth

| Method | Endpoint | 説明 |
|---|---|---|
| `POST` | `/api/v1/auth/login/{provider}` | OAuth ログイン |
| `POST` | `/api/v1/auth/reissue` | トークン再発行 |
| `POST` | `/api/v1/auth/logout` | ログアウト |

### Interview

| Method | Endpoint | 説明 |
|---|---|---|
| `POST` | `/api/v1/intvs` | 面接作成 |
| `PATCH` | `/api/v1/intvs/{intvId}` | 面接タイトル変更 |
| `PATCH` | `/api/v1/intvs/{intvId}/start` | 面接開始 |
| `PATCH` | `/api/v1/intvs/{intvId}/pause` | 面接一時停止 |
| `PATCH` | `/api/v1/intvs/{intvId}/resume` | 面接再開 |
| `PATCH` | `/api/v1/intvs/{intvId}/finish` | 面接終了 |
| `GET` | `/api/v1/intvs/stats` | 完了面接の日別統計 |

### Preparation

| Method | Endpoint | 説明 |
|---|---|---|
| `POST` | `/api/v1/preparations/{intvId}/files/presigned-url` | 準備資料アップロード URL 発行 |
| `POST` | `/api/v1/preparations/{intvId}/files` | 準備資料メタデータ登録 |

### Question

| Method | Endpoint | 説明 |
|---|---|---|
| `POST` | `/api/v1/intvs/{intvId}/questions` | AI 質問生成依頼 |
| `GET` | `/api/v1/intvs/{intvId}/questions` | 質問一覧照会 |
| `POST` | `/internal/v1/questions/callback` | AI 質問生成 callback |

### Answer

| Method | Endpoint | 説明 |
|---|---|---|
| `POST` | `/api/v1/intvs/{questionSetId}/answers/presigned-url` | 回答動画アップロード URL 発行 |
| `POST` | `/api/v1/intvs/{questionSetId}/answers` | 回答メタデータ登録 |
| `POST` | `/api/v1/answers/{answerId}/analysis` | STT 分析依頼 |
| `POST` | `/api/v1/intvs/{questionSetId}/gaze` | gaze segment 転送 |
| `POST` | `/internal/v1/answers/stt/callback` | STT callback |

### Feedback / Report

| Method | Endpoint | 説明 |
|---|---|---|
| `POST` | `/internal/v1/feedbacks/callback` | 質問別 feedback callback |
| `GET` | `/api/v1/report/list` | レポート一覧照会 |
| `GET` | `/api/v1/report/detail/{intvId}` | レポート詳細照会 |
| `DELETE` | `/api/v1/report/{intvId}` | レポート削除 |
| `POST` | `/internal/v1/reports/callback` | レポート生成 callback |

## 実行方法

### 必要環境

- Java 17
- Docker
- PostgreSQL
- AWS S3 bucket
- AI server URL

### 環境変数

`application.yml` は次の環境変数を使用します。

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gamyeon
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
AWS_REGION=ap-northeast-2
AI_SERVER_URL=http://localhost:8000
```

JWT secret、OAuth client 情報、S3 bucket 設定は、実行環境に合わせて別途設定する必要があります。

### ローカル実行

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### ビルド

```bash
./gradlew clean build
```

### フォーマット適用

```bash
./gradlew spotlessApply
```

### Docker 実行

```bash
./gradlew clean bootJar
docker build -t gamyeon-spring .
docker run -p 8080:8080 gamyeon-spring
```

## ポートフォリオで強調できる点

### 1. AI サーバーとの非同期連携

質問生成、STT 分析、フィードバック、レポート生成をすべて外部 AI サーバーと callback ベースで連携しています。Spring サーバーは各リクエストの状態を保存し、callback 結果に応じてドメイン状態を遷移させます。

### 2. イベントベースのレポート生成

面接終了とフィードバック保存をイベントとして分離しました。Report モジュールはイベントを購読して生成条件を判断するため、Interview / Feedback / Report の責務を疎結合に保てます。

### 3. Presigned URL ベースのファイルアップロード

大容量の PDF や動画ファイルを Spring サーバーが直接受け取らず、S3 presigned URL によってクライアントが直接アップロードする構成にしました。サーバーはメタデータとアクセス URL のみを管理します。

### 4. ドメイン中心の状態管理

`Intv`、`Preparation`、`Answer`、`Report` などのドメインオブジェクトが状態遷移メソッドを持つため、不正な状態変更を防ぎやすい構造になっています。

### 5. Port ベースの依存分離

application layer は repository、storage、AI client の具体実装を直接知らず、port interface に依存します。これにより、テストや外部インフラの差し替えがしやすくなっています。

## Troubleshooting

### 1. 回答動画が S3 で質問ごとのフォルダに分散する問題

問題:

回答動画の presigned URL を発行する際、最初は `questionSetId` を S3 key の path segment として利用していました。

```text
answers/{questionSetId}/video/{uuid}-{fileName}
```

この構造では、1 つの面接が 7 問で構成される場合、S3 bucket 上に質問ごとのフォルダが 7 個作成されます。そのため、運用時に「1 つの面接に属する回答動画」を一覧で確認しづらい問題がありました。

解決:

クライアント API は既存のまま `questionSetId` を受け取り、サーバー内部で `questionSetId` から `intvId` を取得する方式に変更しました。S3 key 生成時のみ `intvId` を利用します。

```text
answers/{intvId}/{uuid}-{fileName}
```

これにより、フロントエンドの API 契約を変更せずに、S3 上では面接単位で回答動画を管理できるようにしました。

### 2. AI callback の重複処理問題

問題:

AI サーバーとの連携は callback ベースで行われます。ネットワーク再送や AI サーバー側の retry により、同じ質問に対する feedback callback が複数回届く可能性があります。

単純に callback を受け取るたびに保存すると、同じ質問に対して複数の feedback が作成され、Report 生成条件や集計結果が不安定になります。

解決:

`FeedbackWebhookService` で `questionSetId` 基準の完了済み feedback が存在するか先に確認し、すでに処理済みの callback は無視するようにしました。

```java
boolean alreadyProcessed = feedbackPersistence.existsCompletedByQuestionSetId(questionSetId);
if (alreadyProcessed) {
    return;
}
```

callback API は外部システムと接続される境界であるため、冪等性をアプリケーションレベルで保証するように設計しました。

### 3. レポート生成タイミングの競合問題

問題:

レポート生成は `InterviewFinishedEvent` と `FeedbackSavedEvent` の両方を契機に実行されます。つまり、面接終了イベントと最後の feedback 保存イベントが近いタイミングで発生すると、同じ面接に対して report 生成リクエストが重複実行される可能性があります。

解決:

`ReportGenerateService` では `findByIntvIdWithLock` を使い、Report row に pessimistic write lock を取得してから状態を確認します。

```java
Report report = loadReportPort.findByIntvIdWithLock(intvId).orElseGet(...);
if (report.getStatus() != ReportStatus.IN_PROGRESS) {
    return;
}
```

これにより、複数イベントが同時に到達しても 1 つの transaction だけが report を処理し、すでに処理済みの report は即時に return するようにしました。

### 4. AI レポート callback の JSONB 保存問題

問題:

AI サーバーから受け取る report 詳細データはネストされた JSON 構造です。これを Hibernate JSONB カラムに DTO オブジェクトのまま保存しようとすると、型変換の過程で例外が発生したり、DB カラムと JSONB 内部の値がずれたりする可能性があります。

解決:

`ReportCallbackService` では callback DTO を `ObjectMapper.convertValue` で `Map<String, Object>` に変換した後、同じ Map から `total_score`, `answered_count` を抽出するようにしました。

```java
Map<String, Object> reportDataMap = objectMapper.convertValue(detail, Map.class);
```

これにより、JSONB に保存されるデータと別カラムに保存される要約値の source を 1 つに統一しました。

### 5. AI 質問生成に失敗すると面接進行が止まる問題

問題:

質問生成は外部 AI サーバーに依存します。AI サーバーが応答しない、またはエラーを返すと、ユーザーは質問を受け取れず面接を進められません。

解決:

`QuestionSetApplicationService` では AI 質問生成リクエストが失敗すると `status = "FAIL"` として質問生成ロジックを呼び出し、カスタム質問は 0 件として扱います。その後、共通質問 repository から必要な件数の質問を取得し、質問セットを構成します。

この fallback により、AI カスタム質問生成に失敗しても面接自体は進行できます。

### 6. 大容量ファイルアップロード時のサーバー負荷問題

問題:

履歴書 PDF や回答動画を Spring サーバーが直接 multipart で受け取ると、サーバーメモリ使用量とネットワーク負荷が大きくなります。特に回答動画はサイズが大きくなりやすいため、API サーバーがファイル転送のボトルネックになる可能性があります。

解決:

ファイルアップロードは S3 presigned URL ベースで設計しました。サーバーはファイル拡張子、content type、size を検証し、presigned URL と file key のみを発行します。実際のファイル body はクライアントが S3 に直接アップロードします。

これにより、Spring サーバーはファイルデータではなく metadata のみを管理するようになり、アップロード負荷を下げつつ storage の責務を S3 に分離しました。

## 今後の改善案

- 質問順序カラムを明示的に保存し、面接再開位置の計算を安定化する
- 回答重複防止を DB unique constraint で補強する
- STT 分析 job queue と retry 状態を別テーブルで管理する
- 内部 callback API に API key または signature 検証を追加する
- Report 生成失敗理由をユーザーに表示できるよう、エラー情報を詳細化する
- OpenAPI ドキュメントを自動生成する

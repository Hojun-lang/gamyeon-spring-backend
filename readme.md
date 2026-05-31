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

### 1. `spotlessJavaCheck` が失敗する

症状:

```text
Execution failed for task ':spotlessJavaCheck'
The following files had format violations
```

原因:

Java コードが google-java-format の規則に従っていない場合に発生します。

解決方法:

```bash
./gradlew spotlessApply
./gradlew clean build
```

### 2. Gradle wrapper の lock ファイル権限エラー

症状:

```text
java.io.FileNotFoundException:
~/.gradle/wrapper/dists/.../gradle-*.zip.lck
Operation not permitted
```

原因:

実行環境で `~/.gradle` キャッシュディレクトリへアクセスできない、または sandbox 環境でホームディレクトリへの書き込みが制限されている場合に発生します。

解決方法:

- ローカルターミナルで直接実行する
- `~/.gradle` ディレクトリの権限を確認する
- CI 環境では Gradle cache の権限を明示的に設定する

### 3. PostgreSQL に接続できない

症状:

```text
Connection refused
FATAL: password authentication failed
```

原因:

`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` が誤っている、または PostgreSQL が起動していない場合に発生します。

解決方法:

```bash
docker ps
```

DB コンテナが存在しない場合は、PostgreSQL を先に起動し、環境変数を再確認します。

### 4. AI サーバー呼び出しに失敗する

症状:

```text
AI question generation request failed
STT analysis request failed
AI report generation request failed
```

原因:

`AI_SERVER_URL` が誤っている、または AI サーバーが起動していない場合に発生します。

解決方法:

```bash
curl http://localhost:8000/health
```

AI サーバーが別のコンテナネットワークに存在する場合は、`localhost` ではなくサービス名を使用します。

### 5. S3 presigned URL アップロードに失敗する

症状:

```text
403 Forbidden
SignatureDoesNotMatch
AccessDenied
```

原因:

AWS region、bucket policy、IAM 権限、または presigned URL 生成時の content type と実際のアップロードリクエストの `Content-Type` が一致しない場合に発生することがあります。

解決方法:

- `AWS_REGION` の値を確認する
- S3 bucket policy と IAM 権限を確認する
- presigned URL 発行時の `contentType` とアップロード時の `Content-Type` を一致させる

### 6. Feedback / Report テスト時に Docker 依存のエラーが発生する

症状:

テストまたはローカル実行中に DB / 外部サービス接続エラーが発生します。

解決方法:

```bash
docker ps
```

必要な DB または外部サービスコンテナが起動しているか確認します。

### 7. Report が生成されない

原因:

Report 生成は、すべてのフィードバックが成功した場合、またはスケジューラー基準の最小フィードバック数を満たした場合に進行します。

確認項目:

- 面接が `FINISHED` 状態か
- Feedback callback が保存されているか
- `FeedbackSavedEvent` が発行されているか
- `reports` レコードが `IN_PROGRESS` 状態か
- AI report callback が正常に届いているか

## 今後の改善案

- 質問順序カラムを明示的に保存し、面接再開位置の計算を安定化する
- 回答重複防止を DB unique constraint で補強する
- STT 分析 job queue と retry 状態を別テーブルで管理する
- 内部 callback API に API key または signature 検証を追加する
- Report 生成失敗理由をユーザーに表示できるよう、エラー情報を詳細化する
- OpenAPI ドキュメントを自動生成する

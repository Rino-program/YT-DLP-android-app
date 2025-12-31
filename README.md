# yt-dlp ダウンローダー - Androidアプリ

yt-dlpとffmpegを使用して、スマートフォン上で直接動画をダウンロードできるAndroidアプリです。

## 機能

### 📥 ダウンロード機能
- **URLベースのダウンロード**: YouTube、Twitter、Instagramなど、yt-dlpがサポートするほぼすべてのサイトに対応
- **詳細なオプション設定**: yt-dlpとffmpegの詳細なオプションをUIから設定可能
- **進捗表示**: ダウンロード進捗をリアルタイムで表示
- **エラー表示**: エラーメッセージを見やすく表示

### 🔧 バイナリ管理
- **自動ダウンロード**: アプリ初回起動時にyt-dlpとffmpegを自動ダウンロード
- **バージョン管理**: インストール済みバイナリのバージョン表示
- **更新機能**: ワンタップでバイナリを最新版に更新
- **削除機能**: 不要なバイナリをアンインストール

### ⚙️ 設定機能
- **ダウンロードフォルダ指定**: 保存先フォルダをカスタマイズ
- **プリセット設定**: よく使うオプションをデフォルト設定として保存
- **自動更新設定**: バイナリの自動更新有効/無効
- **プロキシ設定**: HTTP/HTTPS/SOCKS4/SOCKS5プロキシに対応

### 📊 履歴管理
- **ダウンロード履歴**: 過去のダウンロード情報を保存・表示
- **ログ表示**: ダウンロード実行ログをリアルタイムで表示
- **クリアオプション**: 履歴をクリア可能

### 🎯 キューイングシステム（複数ダウンロード対応）
- **バッチ処理**: 複数のURLを順番にダウンロード
- **プレイリスト対応**: YouTubeプレイリスト、Twitter リストなどを自動検出
- **ファイル入力**: テキストファイルからURLリストをインポート
- **優先度管理**: キュー内のアイテムの優先度を設定可能
- **キャンセル機能**: キュー内のアイテムをキャンセル可能

### 📈 ダウンロード統計
- **統計情報**: 総ダウンロード数、成功率、失敗数を表示
- **データ転送量**: 総ダウンロード量を表示
- **平均速度**: 平均ダウンロード速度を計算・表示
- **リセット機能**: 統計情報をリセット可能

## インストール

### 前提条件
- Android 6.0 (API 24) 以上
- 十分なストレージ容量（yt-dlp: 約5MB、ffmpeg: 約20MB）
- インターネット接続

### ビルド方法

```bash
# プロジェクトをクローン
git clone <repository>
cd app

# Gradleでビルド
./gradlew build

# エミュレータまたは実機にインストール
./gradlew installDebug
```

## プロジェクト構造

```
com/example/ytdlpapp/
├── data/
│   ├── db/
│   │   ├── entity/          # RoomエンティティDB
│   │   ├── dao/             # Database Access Objects
│   │   └── AppDatabase.kt   # Room Database
│   └── repository/          # リポジトリパターン実装
├── domain/
│   ├── model/               # ドメインモデル
│   └── usecase/             # ビジネスロジック
│       ├── BinaryManager.kt # バイナリ管理エンジン
│       └── DownloadEngine.kt# ダウンロード実行エンジン
└── ui/
    ├── screen/              # Compose画面
    ├── viewmodel/           # MVVM ViewModel
    ├── navigation/          # ナビゲーション定義
    └── theme/               # UIテーマ
```

## 主要コンポーネント

### BinaryManager
yt-dlpとffmpegのダウンロード、更新、削除を管理します。

```kotlin
val binaryManager = BinaryManager(context)
binaryManager.downloadAndInstallYtdlp()
binaryManager.updateYtdlp()
binaryManager.removeBinary("yt-dlp")
```

### DownloadEngine
実際のダウンロード処理を実行します。

```kotlin
val downloadEngine = DownloadEngine(context, binaryManager, repository)
downloadEngine.downloadWithYtdlp(
    url = "https://www.youtube.com/watch?v=...",
    format = "best",
    outputPath = "/storage/emulated/0/Downloads",
    ytdlpOptions = "-x --audio-format mp3",
    ffmpegOptions = ""
)
```

### SettingsRepository
アプリ設定をSharedPreferencesに保存します。

## 高度な使用例

### キューイングシステム（複数ダウンロード）

```kotlin
val queueManager = QueueManager(...)

// URLを1つずつキューに追加
queueManager.addToQueue(QueuedDownload(
    url = "https://www.youtube.com/watch?v=video1",
    format = "best",
    outputPath = "/storage/emulated/0/Downloads",
    priority = 1
))

// プレイリスト全体をダウンロード
queueManager.addPlaylistToQueue(
    url = "https://www.youtube.com/playlist?list=PLxxxxx",
    outputPath = "/storage/emulated/0/Downloads"
)

// テキストファイルからURLリストをインポート
queueManager.addFromFile(
    filePath = "/storage/emulated/0/urls.txt",
    outputPath = "/storage/emulated/0/Downloads"
)

// キュー処理を開始
queueManager.startProcessing()

// キュー処理を停止
queueManager.stopProcessing()
```

### プロキシ設定

```kotlin
val proxyRepository = ProxyRepository(context)

val proxySettings = ProxySettings(
    enabled = true,
    protocol = "socks5",
    host = "proxy.example.com",
    port = 1080,
    username = "user",
    password = "pass"
)

proxyRepository.saveProxySettings(proxySettings)
```

### ダウンロード統計

```kotlin
val statisticsRepository = StatisticsRepository(context)

// 統計情報を取得
val stats = statisticsRepository.getStatistics()
println("成功率: ${stats?.successRate}%")
println("総転送量: ${stats?.totalBytesDownloaded} bytes")
println("平均速度: ${stats?.averageSpeed} KB/s")

// 統計情報をリセット
statisticsRepository.reset()
```

## 使用方法

### 1. 初回起動
- アプリを起動するとバイナリ管理画面が表示されます
- 「インストール」ボタンをタップしてyt-dlpとffmpegをダウンロード

### 2. 単一ダウンロード
- メイン画面にURLを入力
- ダウンロード形式とオプションを設定（オプション）
- 「ダウンロード開始」をタップ
- 進捗はリアルタイムで表示されます

### 3. バッチ処理（複数URL）
- メイン画面の「バッチ処理」ボタンをタップ
- **方法A: 1つずつ追加**
  - URLを入力フィールドに入力
  - 「追加」ボタンをタップ
- **方法B: ファイルからインポート**
  - テキストファイルにURLを1行ずつ記入
  - ファイルパスを入力
  - 「インポート」ボタンをタップ
- 「処理開始」をタップしてキューを実行

### 4. プロキシ設定
- 設定画面の「プロキシ設定」をタップ
- プロキシを有効にする
- プロトコル、ホスト、ポートを入力
- 認証が必要な場合はユーザー名とパスワードを入力
- 「保存」をタップ

### 5. 統計情報の確認
- メイン画面の「統計」ボタンをタップ
- ダウンロード成功率、転送量、平均速度を確認
- 必要に応じて「統計情報をリセット」で初期化

### 6. 設定
- 歯車アイコンをタップして設定画面を開く
- ダウンロードフォルダやデフォルトオプションを設定

## オプション設定例

### yt-dlpオプション
```
-x --audio-format mp3 --audio-quality 192K   # 音声抽出（MP3形式）
-f "best[height<=480]" --no-warnings        # 480p以下の最高品質
-w --restrict-filenames                      # ファイル名制限
```

### ffmpegオプション
```
-c:v libx264 -preset fast -crf 23            # H.264エンコード
-c:a aac -b:a 128k                           # AACオーディオ128kbps
-vf "scale=1280:-1"                          # 縮小処理
```

## 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose
- **Database**: Room
- **依存性注入**: (構造は準備、実装はDIコンテナ追加予定)
- **非同期処理**: Kotlin Coroutines
- **パターン**: MVVM + クリーンアーキテクチャ

## API レファレンス

### BinaryManager API

#### downloadAndInstallYtdlp(): Result<String>
yt-dlpをダウンロード・インストールします。
- **戻り値**: インストール完了時にファイルパスを返す

#### downloadAndInstallFfmpeg(): Result<String>
ffmpegをダウンロード・インストールします。

#### updateYtdlp(): Result<String>
yt-dlpを最新版に更新します。

#### isBinaryInstalled(binaryName: String): Boolean
バイナリがインストールされているか確認します。

#### removeBinary(binaryName: String): Boolean
バイナリをアンインストールします。

### DownloadEngine API

#### downloadWithYtdlp(url, format, outputPath, ytdlpOptions, ffmpegOptions): Result<DownloadInfo>
動画をダウンロードします。

#### processWithFfmpeg(inputFiles, outputFile, ffmpegOptions): Result<String>
ffmpegでファイル処理します。

## トラブルシューティング

### バイナリのダウンロードに失敗する
- インターネット接続を確認
- ストレージの空き容量を確認（最低50MB推奨）

### ダウンロードが遅い
- YouTube等の一部サイトはレート制限される場合があります
- `--socket-timeout`オプションを増やしてみてください

### ffmpegが見つからない
- バイナリ管理画面でffmpegがインストールされていることを確認
- ストレージの空き容量を確認

## 今後の拡張予定

- [x] キューイングシステム（複数ダウンロード対応）
- [x] プロキシ設定
- [x] バッチファイル処理（URLリスト入力）
- [x] ダウンロード統計情報
- [ ] キューイングシステムのバックグラウンド実行
- [ ] ウィジェット対応
- [ ] クラウドストレージへの自動アップロード
- [ ] ダウンロード予約機能

## ライセンス

このプロジェクトはMITライセンス下で公開されています。

yt-dlpは [Public Domain](https://github.com/yt-dlp/yt-dlp) で提供されています。
ffmpegはLGPLライセンスで提供されています。

## サポート

問題が発生した場合は、GitHubのIssuesセクションで報告してください。

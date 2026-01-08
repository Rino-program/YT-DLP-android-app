# yt-dlp ダウンローダー - Androidアプリ

Androidスマートフォンでyt-dlpとffmpegを使って動画・音声をダウンロードできるシンプルなアプリです。

## 機能

### 📥 ダウンロード機能
- URLを入力して動画をダウンロード
- YouTube、Twitter、Instagram など、yt-dlp対応の多数のサイトに対応
- 音声のみ（MP3）モード
- ダウンロード進捗をリアルタイム表示
- 実行ログの表示

### 🔧 バイナリ管理
- yt-dlpの自動ダウンロード・インストール
- FFmpegの自動ダウンロード・インストール（オプション）
- バイナリの削除機能

## インストール

### 前提条件
- Android 7.0 (API 24) 以上
- インターネット接続

### APKのインストール
1. `build/outputs/apk/debug/YtDlpApp-debug.apk` をスマートフォンに転送
2. 「不明なアプリのインストール」を許可
3. APKをインストール

### ビルド方法

```bash
# プロジェクトをクローン
git clone <repository>
cd app

# Gradleでビルド
./gradlew assembleDebug

# APKは build/outputs/apk/debug/ に生成されます
```

## 使い方

### 1. 初回起動
- アプリを起動したら、右上の歯車アイコンから「バイナリ管理」を開く
- 「yt-dlp」の「インストール」ボタンをタップ
- （オプション）「FFmpeg」もインストール（音声抽出に必要）

### 2. ダウンロード
- URLを入力欄に貼り付け
- 音声のみが必要な場合は「音声のみ（MP3）」をチェック
- 「ダウンロード開始」ボタンをタップ

### 3. 保存場所
- ダウンロードしたファイルは `/storage/emulated/0/Downloads/YtDlp/` に保存されます

## プロジェクト構造

```
com/example/ytdlpapp/
├── MainActivity.kt          # アプリのエントリーポイント
├── core/
│   ├── BinaryManager.kt     # yt-dlp/ffmpegのダウンロード・管理
│   └── DownloadManager.kt   # ダウンロード処理
├── model/
│   └── Models.kt            # データモデル
└── ui/
    ├── screen/
    │   └── MainScreen.kt    # メイン画面UI
    └── theme/
        └── Theme.kt         # アプリテーマ
```

## 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose + Material3
- **HTTP**: OkHttp
- **非同期処理**: Kotlin Coroutines + Flow

## トラブルシューティング

### ダウンロードが始まらない
- yt-dlpがインストールされているか確認
- インターネット接続を確認

### 音声抽出ができない
- FFmpegがインストールされているか確認

### 「権限が必要」と表示される
- Android設定から、ストレージ権限を許可

## ライセンス

このプロジェクトはMITライセンス下で公開されています。

- yt-dlp: [Unlicense](https://github.com/yt-dlp/yt-dlp)
- FFmpeg: LGPLライセンス

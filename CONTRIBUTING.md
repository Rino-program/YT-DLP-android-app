# コントリビューションガイド

このプロジェクトへのコントリビューションを歓迎します！

## 開発環境のセットアップ

### 必要なツール
- Android Studio (最新版)
- JDK 17+
- Android SDK 34+
- Gradle 8.0+

### セットアップ手順

1. リポジトリをクローン
```bash
git clone <repository-url>
cd app
```

2. Android Studioで プロジェクトを開く

3. Gradleの同期を待つ（自動）

4. build.gradleで依存関係がダウンロードされるのを待つ

## 開発フロー

### ブランチ戦略
- `main`: リリース版
- `develop`: 開発版
- `feature/*`: 機能開発
- `bugfix/*`: バグ修正

### 機能追加の手順

1. `develop`ブランチから新しいブランチを作成
```bash
git checkout develop
git pull origin develop
git checkout -b feature/new-feature
```

2. コードを実装
   - クリーンアーキテクチャに従う
   - テストコードも一緒に作成
   - Kotlin Style Guideに従う

3. テストを実行
```bash
./gradlew test          # Unit tests
./gradlew connectedTest # UI tests
```

4. コミット
```bash
git add .
git commit -m "feat: Add new feature description"
```

5. プッシュしてPRを作成
```bash
git push origin feature/new-feature
```

## コーディング規約

### Kotlin

```kotlin
// Good: 明確な命名
fun downloadVideoFromUrl(url: String): Result<DownloadInfo>

// Bad: 不明確
fun dl(u: String): Result<Any>
```

### ファイル構成
- エンティティは`entity/`に
- DAO は`dao/`に
- Repository は`repository/`に
- ビジネスロジックは`usecase/`に
- UI要素は`screen/`に

### テスト
- Unit testsは`src/test/`に
- UI testsは`src/androidTest/`に
- テストクラス名は対象クラス名 + "Test"

```kotlin
class DownloadEngineTest {
    @Test
    fun testDownloadSuccess() {
        // Arrange
        val testUrl = "https://example.com"
        
        // Act
        val result = downloadEngine.download(testUrl)
        
        // Assert
        assertTrue(result.isSuccess)
    }
}
```

## PR Checklistプリー

PRを送信する前に確認してください：

- [ ] テストが追加されている
- [ ] すべてのテストがパスしている
- [ ] コード品質が確認されている
- [ ] ドキュメントが更新されている（必要に応じて）
- [ ] CHANGELOGが更新されている

## Issue Reporting

バグを報告する際は、以下の情報を含めてください：

1. **現象**: 何が起きたか
2. **期待動作**: 本来の動作
3. **再現手順**: バグを再現できる手順
4. **環境**:
   - Androidバージョン
   - デバイスモデル
   - アプリバージョン

## リリースプロセス

1. `develop`で最終テスト
2. バージョンを更新 (`build.gradle.kts`)
3. `CHANGELOG.md`を更新
4. `main`にマージ
5. タグをプッシュ
6. リリースノートを作成

##質問やヘルプ

質問がある場合は、GitHubの Discussions セクションで聞いてください。

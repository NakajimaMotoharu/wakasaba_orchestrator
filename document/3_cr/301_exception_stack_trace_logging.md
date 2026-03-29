# 変更管理資料 No.301：ワークフロー例外発生時のスタックトレースログ出力

## 基本情報

| 項目       | 内容                                   |
|----------|--------------------------------------|
| 変更管理No   | 301                                  |
| 変更概要     | ワークフロー内で例外発生時、そのスタックトレースをログ出力する機能の追加 |
| ステータス    | 適用済み（安定稼働確認済み）                       |
| 変更対象ファイル | `Main.java`                          |
| 完全修飾クラス名 | `com.wks.main.Main`                  |

---

## 変更背景・目的

### 変更前の課題

変更前の実装では、`WksWorkFlow.execScheduledJob(args)` 実行中に例外が発生した場合、
その例外はそのまま `main` メソッドの `throws` を通じて JVM へ委譲されていた。

このとき `outLog()` に到達しないため、ログファイルは一切生成されない仕様となっており、
以下の問題が存在していた。

- **障害発生時にスタックトレースがファイルとして残らない**：JVM 出力（標準エラー）に出るのみで永続化されない
- **デバッグ情報の損失**：バッチ開始時刻・それまでの実行ログ（途中まで蓄積された `Main.log`）も出力されず捨てられる

### 変更目的

ワークフロー実行中に例外が発生した場合でも、
スタックトレースを含む実行ログをファイルとして出力し、障害調査を可能にする。

---

## 変更内容

### 変更ファイル：`Main.java`

#### 変更箇所：`main` メソッド内

**変更前**

```java
public class Main {
	public static void main(String[] args) throws IOException {
		// ワークフローに従い各サーバにアクセス・処理実行
		WksWorkFlow.execScheduledJob(args);
	}
}
```

**変更後**

```java
public class Main {
	public static void main(String[] args) throws IOException {
		try {
			// ワークフローに従い各サーバにアクセス・処理実行
			WksWorkFlow.execScheduledJob(args);
		} catch (Exception e) {
			// メモリ上に文字列を書き込むためのバッファ作成
			StringWriter sw = new StringWriter();
			// StringWriterに書き込むためのPrintWriterを作成
			PrintWriter pw = new PrintWriter(sw);
			// スタックトレースをPrintWriterへ出力
			e.printStackTrace(pw);
			// StringWriterに溜まった内容を1つの文字列として取得、ログへ追記
			log.add(sw.toString());
		}
	}
}
```

#### 使用クラス・インポート

本変更に伴い、以下のインポートが追加された。

| クラス            | パッケージ     |
|----------------|-----------|
| `StringWriter` | `java.io` |
| `PrintWriter`  | `java.io` |

---

## 変更による動作の差異

### 例外発生時の動作比較

| 観点              | 変更前                  | 変更後                               |
|-----------------|----------------------|-----------------------------------|
| スタックトレースの出力先    | 標準エラー（JVM デフォルト動作）のみ | `Main.log` へ追記 → ログファイルへ出力        |
| バッチ終了時刻ログ       | 出力されない               | `LOG_END_TIME` が出力される             |
| ログファイルの生成       | **生成されない**           | **生成される**                         |
| 例外発生後の処理続行      | `main` を抜けて JVM 終了   | `catch` ブロック後に終了時刻記録・`outLog` へ進む |
| ログファイル内容（例外発生時） | —                    | 開始時刻 / 途中ログ / スタックトレース / 終了時刻     |

### 正常時の動作

正常系（例外なし）の動作に変更はない。

---

## 例外処理フロー（変更後）

```
main(args) ← args.length == 3
  │
  ├─ log に開始時刻を追記
  │
  ├─ try
  │    └─ WksWorkFlow.execScheduledJob(args)
  │         └─ 各サーバへの SSH/Bash 処理...
  │
  ├─ catch (Exception e)
  │    └─ e.printStackTrace(pw) → sw.toString() → log.add(...)
  │         ※ 例外発生時のみ通過。正常時はスキップ。
  │
  ├─ log に終了時刻を追記（正常・異常どちらでも実行）
  │
  └─ outLog(...) を呼び出しログファイルを出力（正常・異常どちらでも実行）
```

---

## 影響範囲

| 対象             | 影響                                                  |
|----------------|-----------------------------------------------------|
| `Main.main`    | `execScheduledJob` 呼び出しを try-catch で囲むよう変更          |
| `WksWorkFlow`  | 変更なし（`throws` 宣言はそのまま維持）                            |
| `SshCommand`   | 変更なし                                                |
| `WksConstants` | 変更なし                                                |
| ログファイル出力仕様     | 例外発生時もログファイルが生成されるよう動作変更                            |
| 既存ドキュメント（要更新）  | `001_MAIN.md`、`101_MAIN.md`、`092_ERROR_HANDLING.md` |

---

## ドキュメント更新要否

| ドキュメント                       | 更新箇所（概要）                                                                                   |
|------------------------------|--------------------------------------------------------------------------------------------|
| `0_rd/001_MAIN.md`           | MN-03：`execScheduledJob` 呼び出しを try-catch で囲む旨を追記                                           |
|                              | MN-04：例外発生時もログファイルが生成される旨に記述変更                                                             |
|                              | 例外要件テーブル：`main` の `throws` 宣言から `JSchException`・`InterruptedException` を除外（catch で吸収されるため） |
|                              | 制約・注意事項：「例外発生時にログファイルは生成されない」の記述を削除                                                        |
| `1_ui/101_MAIN.md`           | 処理フロー図：`execScheduledJob` 呼び出し部分に try-catch ブロックを追加                                        |
|                              | 設計上の注意点：ログファイル未生成ケースの記述を変更                                                                 |
| `0_rd/092_ERROR_HANDLING.md` | 例外伝播経路：`Main.main` での catch を反映した経路図に更新                                                    |
|                              | ログ未出力ケーステーブル：「処理途中で例外が発生」行の記述を変更                                                           |

---

## 備考

- キャッチ対象は `Exception`（検査例外・非検査例外を含む全例外）。
- スタックトレースの文字列化には `StringWriter` + `PrintWriter` を使用し、`e.printStackTrace(pw)` → `sw.toString()` の手順で
  `log` に追加する。
- `outLog()` 自体が `IOException` をスローしうる点は変更前後で変わらず、その場合は引き続き JVM に委譲される。

# wakasaba_orchestrator 要件定義書：BashExec

## 基本情報

| 項目       | 内容                                                                     |
|----------|------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.BashExec`                                                |
| ファイル名    | `BashExec.java`                                                        |
| 種別       | class（public）                                                          |
| 責務       | Java の `ProcessBuilder` を使用してローカルサーバ（自サーバ）上でBashコマンドを実行する。標準出力をログに記録する |
| 主な依存クラス  | `Main`、`WksConstants`                                                  |

---

## 要求機能

### BE-01：ローカルOSパッケージ更新コマンドの実行

- `sudo apt update` をローカルサーバ上で実行できること。
- コマンド実行は `ProcessBuilder` を用いて `sh -c <コマンド>` 形式で行うこと。
- プロセス完了まで同期的に待機すること（`process.waitFor()`）。
- 実行コマンドおよびプロセスの標準出力をグローバルログへ記録すること。

### BE-02：ローカルOSパッケージアップグレードコマンドの実行

- `sudo apt upgrade -y` をローカルサーバ上で実行できること。
- `BE-01` と同様の実行・待機・ログ記録を行うこと。

### BE-03：ローカルサーバの遅延再起動

- `(sleep 60 && sudo shutdown -r now) &` をローカルサーバ上でバックグラウンド実行できること。
- プロセスの終了を待機しないこと（`waitFor()` を呼ばない）。これにより、バッチ処理が終了した60秒後にサーバが再起動する。
- `waitFor()` を呼ばないため `InterruptedException` は発生しない（`update()` / `upgrade()` とは異なる）。
- 実行コマンドをグローバルログへ記録すること（標準出力の取得は不要）。

### BE-04：ローカルBashコマンドの共通実行

- `ProcessBuilder` を用いて `sh -c <コマンド>` 形式でローカルBashコマンドを実行する共通処理を持つこと。
- 実行コマンド文字列（`$ <コマンド>` 形式）をグローバルログへ記録すること。
- プロセスの標準出力を全行読み取り、グローバルログへ記録すること。

---

## 要求インタフェース

### フィールド

| フィールド名 | 型                   | 修飾子                    | 初期値        | 要件                                  |
|--------|---------------------|------------------------|------------|-------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` | `Main.log` への参照。コマンドと標準出力のログ記録に使用する |

### メソッド

| メソッド名        | 戻り値型   | 修飾子              | 要件概要                                         |
|--------------|--------|------------------|----------------------------------------------|
| `update`     | `void` | `public static`  | `CMD_UPDATE` を引数に `runCommand` を呼び出すラッパー     |
| `upgrade`    | `void` | `public static`  | `CMD_UPGRADE` を引数に `runCommand` を呼び出すラッパー    |
| `shutdown`   | `void` | `public static`  | 60秒遅延バックグラウンド再起動コマンドを起動する。`waitFor()` は不要    |
| `runCommand` | `void` | `private static` | コマンド実行・出力読み取り・ログ記録の共通処理。`waitFor()` で完了を待機する |

---

## SshCommand.shutdown() との比較

| 項目     | `SshCommand.shutdown()`   | `BashExec.shutdown()`                  |
|--------|---------------------------|----------------------------------------|
| コマンド   | `sudo shutdown -r now`    | `(sleep 60 && sudo shutdown -r now) &` |
| 実行方式   | SSH / 即時実行                | Bash / バックグラウンド・60秒遅延                  |
| プロセス待機 | `SshExec.execute()` で完了待ち | `start()` のみ（待機なし）                     |
| 目的     | リモートサーバの即時再起動             | 自サーバをバッチ終了後に再起動する                      |

---

## 例外要件

| メソッド名      | 例外クラス                  | 想定発生状況                     |
|------------|------------------------|----------------------------|
| `update`   | `IOException`          | プロセス起動失敗・標準出力読み取り失敗        |
| `update`   | `InterruptedException` | `process.waitFor()` 中の割り込み |
| `upgrade`  | `IOException`          | プロセス起動失敗・標準出力読み取り失敗        |
| `upgrade`  | `InterruptedException` | `process.waitFor()` 中の割り込み |
| `shutdown` | `IOException`          | プロセス起動失敗                   |

---

## 制約・注意事項

- `runCommand` では `process.waitFor()` の後に `getInputStream()`
  で出力を読み取る。大量の出力が発生するとバッファオーバーフローによるデッドロックのリスクがある点に留意すること。
- `SshExec` と異なり実行済みフラグを持たないため、同じコマンドを複数回呼ぶことが可能である。
- `shutdown()` は `InterruptedException` をスローしないが、`update()` / `upgrade()` はスローすること。
- 標準出力の読み取りには文字コード UTF-8 を使用すること。

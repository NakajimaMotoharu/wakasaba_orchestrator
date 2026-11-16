# wakasaba_orchestrator 要件定義書：BashExec

## 基本情報

| 項目             | 内容                                                                                                              |
|------------------|-------------------------------------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.BashExec`                                                                                           |
| ファイル名       | `BashExec.java`                                                                                                   |
| 種別             | class（public）                                                                                                   |
| 責務             | Java の `ProcessBuilder` を使用してローカルサーバ（自サーバ）上でBashコマンドを実行する。標準出力をログに記録する |
| 主な依存クラス   | `AplLog`、`WksConstants`                                                                                          |

---

## 要求機能

### BE-01：ローカルOSパッケージ更新コマンドの実行

- `sudo apt update` をローカルサーバ上で実行できること。
- コマンド実行は `ProcessBuilder` を用いて `sh -c <コマンド>` 形式で行うこと。
- プロセス完了まで同期的に待機すること（`process.waitFor()`）。コマンド・標準出力のログ追加も終了後となる。
- 実行コマンドおよびプロセスの標準出力をAplLogへ渡して記録すること。

### BE-02：ローカルOSパッケージアップグレードコマンドの実行

- `sudo apt upgrade -y` をローカルサーバ上で実行できること。
- `BE-01` と同様の実行・待機・ログ記録を行うこと。

### BE-03：ローカルサーバの遅延再起動

- `(sleep 60 && sudo shutdown -r now) &` をローカルサーバ上でバックグラウンド実行できること。
- プロセスの終了を待機しないこと（`waitFor()` を呼ばない）。これにより、バッチ処理が終了した60秒後にサーバが再起動する。
- `waitFor()` を呼ばないため `InterruptedException` は発生しない（`update()` / `upgrade()` とは異なる）。
- 実行コマンドをAplLogへ渡して記録すること（標準出力の取得は不要）。

### BE-04：ローカルBashコマンドの共通実行

- `ProcessBuilder` を用いて `sh -c <コマンド>` 形式でローカルBashコマンドを実行する共通処理を持つこと。
- outLogFlagがtrueの場合はコマンドをAplLogのCMDへ渡し、localhostで記録すること。
- プロセスの標準出力を全行読み取り、String[]で返すこと。outLogFlagがtrueの場合のみAplLogへ各行を渡すこと。

### BE-05：管理端末での固定待機

`public static void waitOneMin() throws IOException, InterruptedException`。
`runCommand(CMD_WAIT_ONE_MIN, false)` を呼び、管理端末の `sleep 60` を同期待機する。
両サービス停止後に1回、全台への再起動指示後に1回利用する。接続先でのsleepは行わない。

### BE-06：UNIX時刻取得

`public static long getNowTime() throws IOException, InterruptedException`。
`runCommand(CMD_GET_TIME, false)` の先頭行を `Long.parseLong` で変換して返す。
コマンドは `date '+%s'` で、単位は秒。復帰待ちの開始時刻と現在時刻に使用する。
先頭行の存在と整数形式を前提とし、不正値なら非検査例外がMainへ伝播する。

---

## ログ出力制御

| 呼び出し元              | outLogFlag | ログ                                           |
|-------------------------|------------|------------------------------------------------|
| update / upgrade        | true       | 終了後にlocalhostのCMD、標準出力各行のOUT      |
| waitOneMin / getNowTime | false      | コマンド・結果を記録しない。待機・戻り値は維持 |
| shutdown                | 対象外     | バックグラウンド起動後にlocalhostのCMDのみ     |

---

## 要求インタフェース

### フィールド

なし。ログはAplLogを通して追加する。

### メソッド

| メソッド名   | 戻り値型   | 修飾子           | 要件概要                                                                     |
|--------------|------------|------------------|------------------------------------------------------------------------------|
| `update`     | `void`     | `public static`  | `CMD_UPDATE` を引数に `runCommand` を呼び出すラッパー                        |
| `upgrade`    | `void`     | `public static`  | `CMD_UPGRADE` を引数に `runCommand` を呼び出すラッパー                       |
| `shutdown`   | `void`     | `public static`  | 60秒遅延バックグラウンド再起動コマンドを起動する。`waitFor()` は不要         |
| `waitOneMin` | `void`     | `public static`  | 管理端末でsleep 60の完了を待つ                                               |
| `getNowTime` | `long`     | `public static`  | dateの先頭行をUNIX秒として返す                                               |
| `runCommand` | `String[]` | `private static` | コマンド実行・出力読み取り・ログ記録の共通処理。`waitFor()` で完了を待機する |

---

## SshCommand.shutdown() との比較

| 項目         | `SshCommand.shutdown()`        | `BashExec.shutdown()`                  |
|--------------|--------------------------------|----------------------------------------|
| コマンド     | `sudo shutdown -r now`         | `(sleep 60 && sudo shutdown -r now) &` |
| 実行方式     | SSH / 即時実行                 | Bash / バックグラウンド・60秒遅延      |
| プロセス待機 | `SshExec.execute()` で完了待ち | `start()` のみ（待機なし）             |
| 目的         | リモートサーバの即時再起動     | 自サーバをバッチ終了後に再起動する     |

---

## 例外要件

| メソッド名 | 例外クラス             | 想定発生状況                           |
|------------|------------------------|----------------------------------------|
| `update`   | `IOException`          | プロセス起動失敗・標準出力読み取り失敗 |
| `update`   | `InterruptedException` | `process.waitFor()` 中の割り込み       |
| `upgrade`  | `IOException`          | プロセス起動失敗・標準出力読み取り失敗 |
| `upgrade`  | `InterruptedException` | `process.waitFor()` 中の割り込み       |
| `shutdown` | `IOException`          | プロセス起動失敗                       |

---

## 制約・注意事項

- ログはAplLog.getInstance()へ渡す。日時・サーバIP・ログレベルは受付時に付与され、専用スレッドが随時ファイルへ出力する。
- サーバごとの複数行を一括排他しないため、並列更新中は行が混在する。


- 並列更新中はSSH実行前の時刻取得 `getNowTime()` も各更新スレッドから呼ばれる。各呼び出しのプロセス・出力収集リストはローカルであり、ログのみを共有する。自端末のOS更新は最後に直列実行する。


- `runCommand` では `process.waitFor()` の後に `getInputStream()`
  で出力を読み取る。大量の出力が発生するとバッファオーバーフローによるデッドロックのリスクがある点に留意すること。
- `SshExec` と異なり実行済みフラグを持たないため、同じコマンドを複数回呼ぶことが可能である。
- `shutdown()` は `InterruptedException` をスローしないが、`update()` / `upgrade()` はスローすること。
- 標準出力の読み取りには文字コード UTF-8 を使用すること。

- `runCommand` の戻り値は標準出力の行配列（空出力は長さ0）。outLogFlagがtrueの場合のみコマンドと各行をAplLogへ渡す。
- `waitOneMin`・`getNowTime` のプロセス起動・読み取り失敗は `IOException`、完了待ち割り込みは `InterruptedException`
  として上位へ伝播する。

## 備考（将来対応）

- 将来対応1（ローカル大量出力時の停止リスク）：BashExecは終了待ちの後に標準出力を読み取るため、パイプが満杯になると子プロセスとJava側が相互待ちになる可能性がある。現在の運用では許容し、今回の変更では未解消とする。
- 将来対応2（SSH標準出力の逐次ログ化）：SshExecは標準出力を配列に蓄積し、コマンド終了後にSshCommandからAplLogへ渡す。実行中の進捗は記録しない現行仕様を許容し、将来対応1とあわせて見直す。
- 上記2件は解消済みではなく、明示的に保留した事項である。

# wakasaba_orchestrator 基本設計書 補足：例外・エラーハンドリング設計

## 概要

例外の伝播経路と、AplLogによるERR記録・終了処理を定義する。

---

## 基本方針

- 各部品は個別の自動回復を行わず、業務例外をMainまで伝播する。
- MainはワークフローのExceptionを捕捉し、AplLog.outExceptionLog(localhost, e)へ渡す。
- AplLogはprintStackTraceで例外全体を文字列化し、型・メッセージ・発生箇所・原因・Suppressedを各行のERRとして受付する。
- MainはSYS終了ログ追加→endOfLogOutput→joinで受付済みログの出力完了を待つ。
- 初回getInstanceのオープン失敗はワークフロー用catchの外でIOExceptionとして伝播する。Errorは捕捉しない。
- ログスレッドのsleep割り込みは内部でERRを追加して継続する。通知後は追加受付しない。Mainのjoin割り込みは上位へ伝播する。
- PrintStreamの書き込み・クローズ時のエラーフラグ検査は追加しない。

---

## 例外の種類と発生箇所

### 検査例外（Checked Exception）

| 例外クラス                               | 主な発生箇所                          | 原因                                                  |
|------------------------------------------|---------------------------------------|-------------------------------------------------------|
| `FileNotFoundException`（`IOException`） | `SshCommand.wgetPaperMc`              | 管理端末上のPaperMCバージョンファイルのオープン失敗   |
| `IOException`                            | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの読み込み失敗                        |
| `IOException`                            | `SshExec.execute`                     | SSH標準出力の読み取り失敗                             |
| `IOException`                            | `BashExec.runCommand`                 | ローカルプロセスの起動・出力読み取り失敗              |
| `IOException`                            | `Curl.exec`                           | HTTPリクエストのネットワーク障害                      |
| `FileNotFoundException`（`IOException`） | `AplLog.getInstance`                  | ログ初回オープン失敗                                  |
| `InterruptedException`                   | `Main`のAplLog.join                   | 出力終了待ち中の割り込み                              |
| `InterruptedException`                   | `SshExec.execute`                     | SSH待機中の割り込み                                   |
| `InterruptedException`                   | `SshCommand.waitForBecomeActive`      | ポーリングスリープ中の割り込み                        |
| `InterruptedException`                   | `BashExec.runCommand`                 | `process.waitFor()` 中の割り込み                      |
| `InterruptedException`                   | `Curl.exec`                           | HTTPリクエスト待機中の割り込み                        |
| `JSchException`                          | `SshExec.isAlive`                     | SSHセッション生成エラー（接続失敗は吸収して `false`） |
| `JSchException`                          | `SshExec.execute`                     | SSH接続・チャンネル接続失敗                           |

### 非検査例外（Unchecked Exception）

| 例外クラス                   | 主な発生箇所                          | 原因                                                  |
|------------------------------|---------------------------------------|-------------------------------------------------------|
| `RuntimeException`           | `PaperUrlGen.getPaperMcVersion`       | 全系列に正式版が見つからない（空の一覧を含む）        |
| `RuntimeException`           | `UpdateThread.exceptionDetection`     | 子スレッドが保持したその他Exceptionを原因付きで再送出 |
| `RuntimeException`           | `SshCommand.mount` / `umount`         | マウント有無が期待状態と不一致                        |
| `RuntimeException`           | `SshCommand.waitForBecomeActive`      | 失敗したSSH接続試行後、指定秒数の経過を検出           |
| `NumberFormatException`      | `BashExec.getNowTime`                 | UNIX時刻の先頭行をlongに変換できない                  |
| `NumberFormatException`      | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの2行目が整数値でない                 |
| `IndexOutOfBoundsException`  | `ConnectionInformation.getCiFromFile` | 接続情報ファイルが4行未満                             |
| `JacksonException`（非検査） | `PaperUrlGen` 全メソッド              | JSONパース失敗（APIレスポンス形式の変更等）           |
| `NullPointerException`       | `PaperUrlGen` 全メソッド              | JSONのキーが存在しない場合                            |

---

## 異常時運用

- 初期疎通不可では対象のAPLとlocalhostのERR（Servers Not Active）を記録して保守せず復帰する。
- マウント状態不一致、SSH復帰待ち超過、PaperMC正式版未検出等は例外で後続工程を中止する。
- 正式版未検出時のメッセージはOTHER_PAPERMC_EXCEPTION（No official version was found.）。
- 自動復旧・ロールバックは行わず、運用者がログを確認して手動復旧する。
- PaperMCの例外でもSchubertを起動しない。初期疎通確認の接続失敗以外は成功扱いにしない。
- バージョンファイル出力到達前の例外時は以前の内容が残る。ファイル形式・出力時点はNo.308から変更しない。

---

## 並列更新の例外伝播（No.310）

子スレッドは例外を保持して終了し、親は全join後に引数順で検出する。
JSchException・IOException・InterruptedExceptionは同じ例外を再送出し、その他ExceptionはRuntimeException(cause)とする。
開始済みのほかの更新をキャンセルせず、例外の集約も行わない。親のjoin割り込み時も子のキャンセルは追加しない。

```
SshExec / BashExec / Curl / PaperUrlGen 等
 → SshCommand / UpdateThread / WksWorkFlow
 → Main.catch(Exception)
 → AplLog.outExceptionLog(localhost, e)
 → キューへ各行ERR追加 → AplLog.runがファイルへ随時出力
 → Main: SYS終了 → 受付終了 → join

AplLog初回オープン失敗 → Main.throws IOException → JVM
AplLog.join割り込み → Main.throws InterruptedException → JVM
```

## 特例：`SshExec.isAlive()` の例外吸収

`SshCommand.waitForBecomeActive()` → `SshExec.isAlive()` の流れにおいて、  
`session.connect(60000)` が投げる `JSchException`（接続不可）は `isAlive()` 内で `false` として吸収する。

これにより `shutdown` 後の再起動待ちがポーリングで実現される。  
ただし、秘密鍵ファイル読み込みエラー等のセッション生成に起因する `JSchException` は吸収せず上位に伝播させる。

詳細は [106_SSH_EXEC.md](./106_SSH_EXEC.md) を参照。

---

## ログ未出力・不完全出力のケース

| 状況                 | 結果                                                            |
|----------------------|-----------------------------------------------------------------|
| 引数が6つでない      | USAGEのみ。AplLogとファイルは作成しない                         |
| 初回ログオープン失敗 | 開始ログ前にIOException。ワークフローを開始しない               |
| 業務例外             | それまでのログと例外各行ERR、SYS終了を記録して終了              |
| 終了通知後の追加     | 受付せず破棄する。全発行処理完了後に通知する運用前提            |
| SSH実行途中の例外    | execute未完了の標準出力配列はAplLogへ渡されず、未記録となり得る |

---

## 備考（将来対応）

- 将来対応1（ローカル大量出力時の停止リスク）：BashExecは終了待ちの後に標準出力を読み取るため、パイプが満杯になると子プロセスとJava側が相互待ちになる可能性がある。現在の運用では許容し、今回の変更では未解消とする。
- 将来対応2（SSH標準出力の逐次ログ化）：SshExecは標準出力を配列に蓄積し、コマンド終了後にSshCommandからAplLogへ渡す。実行中の進捗は記録しない現行仕様を許容し、将来対応1とあわせて見直す。
- 上記2件は解消済みではなく、明示的に保留した事項である。

APIレスポンス変更時の診断やHTTPステータス検証は従来どおり今後の改善候補とする。

# wakasaba_orchestrator 要件定義書：AplLog

## 基本情報

| 項目             | 内容                                                           |
|------------------|----------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.AplLog`                                          |
| ファイル名       | `AplLog.java`                                                  |
| 種別             | class（public）、Threadを継承                                  |
| 責務             | ログの受付・整形・例外変換・単一スレッドによるファイル随時出力 |
| 主な依存クラス   | `WksConstants`                                                 |
| 利用元           | `Main`、`SshCommand`、`BashExec`、`WksWorkFlow`                |

---

## 要求機能

### AL-01：単一インスタンスと自動起動

privateコンストラクタと同期したgetInstanceで唯一のインスタンスを返すこと。
初回取得で出力先を開き、出力スレッドを自動起動すること。

### AL-02：行単位の整形・並列受付

各ログ行へ日時・サーバIP・レベルを付け、WksConstants.LOG_FORMATで整形すること。SYS・APL・CMD・OUT・ERRを提供すること。
並列更新からの受付に対応し、OUT配列は各要素を順に追加できること。

### AL-03：例外の完全な記録

例外全体をprintStackTraceで文字列化し、各行へERRヘッダを付けること。
型・メッセージ・原因・Suppressedを保持し、スタック配列が空でも記録すること。

### AL-04：随時出力・終了制御

未出力ログを先頭から出力し、排出後に1秒休止すること。終了通知はvolatileで出力スレッドへ伝えること。
通知後は追加を受け付けず、残ログ排出後にファイルを閉じること。
呼び出し側は全ログ発行処理の完了後に最終ログを追加し、通知後のjoinで完了を待つこと。

---

## 要求インタフェース

### フィールド

| フィールド名       | 型             | 修飾子             | 初期値・役割                                         |
|--------------------|----------------|--------------------|------------------------------------------------------|
| `log`              | `List<String>` | `private final`    | `new CopyOnWriteArrayList<>()`。未出力ログのみを保持 |
| `continuationFlag` | `boolean`      | `private volatile` | true。受付・出力継続フラグ                           |
| `ps`               | `PrintStream`  | `private final`    | コンストラクタで生成した出力先                       |
| `instance`         | `AplLog`       | `private static`   | null。唯一のインスタンス                             |

### メソッド

| メソッド                                                 | 戻り値型 | 修飾子                       | 処理                                        |
|----------------------------------------------------------|----------|------------------------------|---------------------------------------------|
| `AplLog()`                                               | -        | `private`                    | 日時を埋めた出力先ファイルをオープン        |
| `getInstance()`                                          | `AplLog` | `public static synchronized` | 初回生成・start、以後同一オブジェクトを返す |
| `run()`                                                  | `void`   | `public`、Override           | リスト先頭から書き出し、終了時にclose       |
| `outSystemLog(String ip, String log)`                    | `void`   | `public`                     | SYS                                         |
| `outApplicationLog(String ip, String log)`               | `void`   | `public`                     | APL                                         |
| `outCommandLog(String ip, String log)`                   | `void`   | `public`                     | CMD、本文に `$ ` を付加                     |
| `outOutputLog(String ip, String log)`                    | `void`   | `public`                     | OUT                                         |
| `outOutputLog(String ip, String[] logs)`                 | `void`   | `public`                     | 配列各要素をOUTとして追加                   |
| `outExceptionLog(String ip, String log)`                 | `void`   | `public`                     | ERR                                         |
| `outExceptionLog(String ip, Exception exception)`        | `void`   | `public`                     | 例外全体を行単位のERRへ変換                 |
| `endOfLogOutput()`                                       | `void`   | `public`                     | continuationFlagをfalseへ変更               |
| `addLog(String ip, String logLevel, String logContents)` | `void`   | `private`                    | 受付可否判定・日時取得・整形・追加          |
| `getDateTime()`                                          | `String` | `private static`             | Asia/Tokyoの日時文字列生成                  |

---

## ログ出力要件

| 項目             | 内容                                                                            |
|------------------|---------------------------------------------------------------------------------|
| 書式             | `[日時 サーバIP ログレベル]: 内容` ＋ LF                                        |
| 日時             | 受付時の管理端末時刻。`yyyyMMddHHmmss`、Asia/Tokyo                              |
| サーバIP欄       | SSH操作対象の `ci.host()`。ホスト名の場合はそのまま。ローカル処理は `localhost` |
| レベル           | `SYS`・`APL`・`CMD`・`OUT`・`ERR`                                               |
| CMD本文          | `$ ` ＋コマンド                                                                 |
| 出力先           | 管理端末の `/home/mini/wakasaba_orchestrator/log/log_%s.txt`                    |
| ファイル名の日時 | AplLog初回生成時の日時。終了時刻ではない                                        |
| 文字コード・更新 | `PrintStream(String)` の既定値。新規作成または上書き                            |
| 書き出し         | 受付済みログを専用スレッドで随時出力。リスト排出後は1秒休止                     |
| 終了             | 最終ログ追加→終了通知→join。残ログ排出後にclose                                 |

```text
[20261116120000 localhost SYS]: BATCH MAINTENANCE START
[20261116120001 192.168.1.10 CMD]: $ sudo apt update
[20261116120005 192.168.1.10 OUT]: Hit:1 ...
[20261116120006 localhost ERR]: java.lang.RuntimeException: update failed
[20261116120006 localhost ERR]: Caused by: java.io.IOException: read failed
[20261116120007 localhost SYS]: BATCH MAINTENANCE END
```

---

## 利用順序

```
1. Mainで6引数を検証
2. AplLog.getInstance(): 初回だけ生成・ファイルオープン・自動start
3. MainがlocalhostのSYS開始ログを追加
4. ワークフロー実行。各部品が接続先とレベルを指定して追加
5. Mainがワークフロー例外を捕捉した場合は例外全体をERRとして追加
6. 全ログ発行処理の完了後、MainがlocalhostのSYS終了ログを追加
7. endOfLogOutput(): 受付終了を通知（この呼び出しだけでは完了待ちしない）
8. AplLogは受付済みログを排出し、PrintStreamをcloseして終了
9. Mainはjoin()で出力スレッドの終了を待って復帰
```

---

## 例外要件

| 箇所                        | 例外                  | 扱い                                               |
|-----------------------------|-----------------------|----------------------------------------------------|
| コンストラクタ・getInstance | FileNotFoundException | ファイルオープン失敗を呼び出し側へ伝播             |
| runのsleep                  | InterruptedException  | localhostのERRへ追加して継続（通知後は受付しない） |
| 呼び出し側のjoin            | InterruptedException  | Mainのthrowsで上位へ伝播                           |

PrintStreamの書き込み・クローズ時のエラーフラグ検査は行わない。

---

## 制約・注意事項

- ログの所有者はAplLogのみとし、各部品はgetInstance経由で追加する。Mainの公開ログリスト・終了時一括出力APIは使用しない。
- 並列更新中も同一インスタンスへ追加する。各行に接続先を付けるが、複数行を一括排他しないためサーバ間のログは混在する。
- 受付済みログの随時書き出しであり、すべてのコマンド出力の逐次取得や厳密な即時性を保証するものではない。
- 正常な終了順序は全ログ発行処理の完了→最終ログ追加→endOfLogOutput→join。終了通知後の追加は受け付けない。
- ログディレクトリの存在、作成・上書き権限と十分な容量を前提とする。親ディレクトリの作成とPrintStreamの書き込みエラーフラグ検査は行わない。
- 1回のバッチ実行で1インスタンスを使用し、終了後の再利用・再起動は行わない。

---

## 備考（将来対応）

- 将来対応1（ローカル大量出力時の停止リスク）：BashExecは終了待ちの後に標準出力を読み取るため、パイプが満杯になると子プロセスとJava側が相互待ちになる可能性がある。現在の運用では許容し、今回の変更では未解消とする。
- 将来対応2（SSH標準出力の逐次ログ化）：SshExecは標準出力を配列に蓄積し、コマンド終了後にSshCommandからAplLogへ渡す。実行中の進捗は記録しない現行仕様を許容し、将来対応1とあわせて見直す。
- 上記2件は解消済みではなく、明示的に保留した事項である。

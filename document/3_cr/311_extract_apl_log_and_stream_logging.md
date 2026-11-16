# 変更管理資料 No.311：AplLogへのログ出力集約と随時ファイル出力

## 基本情報

| 項目             | 内容                                                                                                                                                        |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 変更管理No       | 311                                                                                                                                                         |
| 変更概要         | ログ部品AplLogを追加し、日時・サーバIP・ログレベル付きの非同期ファイル出力、例外の行単位記録、各部品のログ呼び出し移管を実施                                |
| ステータス       | 適用済み（ソース確認・PGレビュー・設計書反映済み。実機検証未実施）                                                                                          |
| 追加ファイル     | `AplLog.java`                                                                                                                                               |
| 変更対象ファイル | `Main.java`、`SshCommand.java`、`BashExec.java`、`WksWorkFlow.java`、`WksConstants.java`                                                                    |
| 完全修飾クラス名 | `com.wks.util.AplLog`、`com.wks.main.Main`、`com.wks.cmd.SshCommand`、`com.wks.util.BashExec`、`com.wks.workflow.WksWorkFlow`、`com.wks.parts.WksConstants` |

---

## 変更背景・目的

### 変更前の課題

No.310では、Mainの共有ログリストへ各部品が追記し、バッチ終了後にMain.outLogで全件をファイルへ出力していた。
実行途中のファイル確認ができず、並列更新中のログも接続先との対応を行単位で識別できなかった。
ログ管理・日時生成・例外の文字列化もMainに集中していた。

### 変更目的

- ログの受付・整形・ファイル出力をAplLogへ集約する。
- 単一の出力スレッドが受付済みログを随時書き出し、バッチ実行中もファイルを確認できるようにする。
- 各行に日時、サーバIP、ログレベルを付ける。
- 例外の型・メッセージ・発生箇所・原因・Suppressedを行単位のERRログとして記録する。
- 全処理完了後に最終ログを追加し、残ログ出力とクローズの完了を待つ。

---

## 変更内容

ソース例は変更箇所の抜粋であり、関係しない処理は省略する。
`PaperUrlGen.java` の差分は書式整理のみであり、本変更の機能対象には含めない。

### 追加ファイル：`AplLog.java`

#### 追加クラス：ログ受付・出力スレッド

```java
public class AplLog extends Thread {
    private final List<String> log = new CopyOnWriteArrayList<>();
    private volatile boolean continuationFlag = true;
    private final PrintStream ps;
    private static AplLog instance = null;

    private AplLog() throws FileNotFoundException {
        // ログ出力用PrintStreamを初期化
        ps = new PrintStream(String.format(WksConstants.PATH_EXEC_LOG, getDateTime()));
    }

    @Override
    public void run() {
        // ログ出力用監視スレッド
    }

    public static synchronized AplLog getInstance() throws FileNotFoundException {
        // すでにインスタンス作成済みの場合、作成済みインスタンスを返却
        if (instance != null) return instance;
        // インスタンス新規作成
        instance = new AplLog();
        // インスタンスのログ出力を開始
        instance.start();
        // 作成したインスタンスを返却
        return instance;
    }
}
```

コンストラクタをprivateとし、`getInstance()` を同期して初回生成の競合を防ぐ。
初回取得時にファイルをオープンし、自動で `start()` する。呼び出し側は再度startしない。
出力先は `PATH_EXEC_LOG` のまま、ファイル名の日時は終了時刻ではなく初回インスタンス生成時刻となる。
文字コードは `PrintStream(String)` の既定値を使用し、ファイルが存在する場合は上書きする。

#### 追加処理：受付と書き出し

```java
public class AplLog extends Thread {
    private void addLog(String ip, String logLevel, String logContents) {
        // ログ出力が停止されている場合は追加不可
        if (!continuationFlag) return;
        // 現在時刻を取得
        String nowTime = getDateTime();
        // フォーマットに従いログリストに追記
        log.add(String.format(WksConstants.LOG_FORMAT, nowTime, ip, logLevel, logContents));
    }

    public void run() {
        // ログ継続フラグがtrueか、未出力ログが残っていれば継続
        while (continuationFlag || !log.isEmpty()) {
            // 未出力ログがなくなるまでログ出力
            while (!log.isEmpty()) {
                // ログリストの先頭を取り出して書き込み
                ps.print(log.removeFirst());
            }

            try {
                // 未出力ログがなくなったら1秒休止
                sleep(1000);
            } catch (InterruptedException e) {
                // 例外ログを出力
                outExceptionLog(WksConstants.OTHER_IP_LOCALHOST, e);
            }
        }

        // PrintStreamをクローズ
        ps.close();
    }

    public void endOfLogOutput() {
        continuationFlag = false;
    }
}
```

- ログ追加時に管理端末の日時を取得し、フォーマット済み文字列を内部リストへ追加する。
- フラグの可視性はvolatile、並列追記はCopyOnWriteArrayListで保証する。
- 出力スレッドは先頭から取り出して書き込み、リストを排出した後に1秒休止する。厳密な即時出力は保証しない。
- 終了通知後も受付済みログが残っていれば出力し、ループ終了後にPrintStreamを閉じる。
- 終了通知以降の追加は受け付けない。Mainでログ発行処理の完了を待ち、最終ログ追加→終了通知→joinの順で利用する。
- sleep中の割り込みはlocalhostのERRとして追加し、出力ループを継続する。終了通知後の割り込みは追加受付の対象外となる。

#### ログ種別・書式

| メソッド                         | レベル | 内容                               |
|----------------------------------|--------|------------------------------------|
| `outSystemLog(ip, log)`          | SYS    | バッチ開始・終了など               |
| `outApplicationLog(ip, log)`     | APL    | 疎通確認など                       |
| `outCommandLog(ip, log)`         | CMD    | コマンド文字列に `$ ` を付加       |
| `outOutputLog(ip, log)`          | OUT    | 標準出力1行                        |
| `outOutputLog(ip, logs)`         | OUT    | 配列を1行ずつ追加                  |
| `outExceptionLog(ip, log)`       | ERR    | エラーメッセージ1行                |
| `outExceptionLog(ip, exception)` | ERR    | スタックトレース全体を行単位で追加 |

```text
[20261116120000 localhost SYS]: BATCH MAINTENANCE START
[20261116120001 192.168.1.10 APL]: Checking Connection
[20261116120002 192.168.1.10 CMD]: $ sudo apt update
[20261116120005 192.168.1.10 OUT]: Hit:1 ...
[20261116120006 localhost ERR]: java.lang.RuntimeException: update failed
[20261116120006 localhost ERR]: Caused by: java.io.IOException: read failed
[20261116120007 localhost SYS]: BATCH MAINTENANCE END
```

並び順は `日時 サーバIP ログレベル`。日時は `yyyyMMddHHmmss`、Asia/Tokyo。
IP欄にはSSH操作対象の `ci.host()` を渡し、管理端末の処理には `localhost` を用いる。
hostがホスト名で設定されている場合はその文字列を出力し、IPアドレスへ解決しない。
複数サーバのログは混在し、コマンド・出力・例外の複数行を一括で排他しない。

#### 追加処理：例外全体の行単位出力

```java
public class AplLog extends Thread {
    public void outExceptionLog(String ip, Exception exception) {
        // メモリ上に文字列を書き込むためのバッファ作成
        StringWriter sw = new StringWriter();
        // StringWriterに書き込むためのPrintWriterを作成
        PrintWriter pw = new PrintWriter(sw);
        // スタックトレースをPrintWriterへ出力
        exception.printStackTrace(pw);
        // 出力をリストに変換
        List<String> lines = sw.toString().lines().toList();

        // stackTrace各行に対して実行
        for (String line : lines) {
            // 各行を例外ログとして出力
            outExceptionLog(ip, line);
        }
    }
}
```

`printStackTrace(PrintWriter)` による文字列化で、例外型・メッセージ・Caused by・Suppressedを保持する。
改行は `String.lines()` で分割し、各行へERRヘッダを付ける。スタック配列が空でも例外の見出しは記録する。

---

### 変更ファイル：`Main.java`

#### 変更前

```java
public class Main {
    public static final List<String> log = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        if (args.length == 6) {
            log.add(String.format(WksConstants.LOG_START_TIME, getDateTime()));
            try {
                WksWorkFlow.execScheduledJob(args);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                log.add(sw.toString());
            }
            log.add(String.format(WksConstants.LOG_END_TIME, getDateTime()));
            outLog(String.format(WksConstants.PATH_EXEC_LOG, getDateTime()));
        }
    }
}
```

#### 変更後

```java
public class Main {
    /**
     * エントリポイント
     *
     * @param args 6つ要求(すべてサーバの接続用データ)
     * @throws IOException          ログファイル書き込み失敗
     * @throws InterruptedException ログファイル書き込み失敗
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 6) {
            // 引数が6つあるとき正常実行開始
            // 開始ログ書き込み
            AplLog.getInstance().outSystemLog(WksConstants.OTHER_IP_LOCALHOST, WksConstants.LOG_START_MESSAGE);

            try {
                // ワークフローに従い各サーバにアクセス・処理実行
                WksWorkFlow.execScheduledJob(args);
            } catch (Exception e) {
                // 例外をログに書き込み
                AplLog.getInstance().outExceptionLog(WksConstants.OTHER_IP_LOCALHOST, e);
            }
            // 終了ログ書き込み
            AplLog.getInstance().outSystemLog(WksConstants.OTHER_IP_LOCALHOST, WksConstants.LOG_FINISH_MESSAGE);
            // ログファイルクロージング
            AplLog.getInstance().endOfLogOutput();
            // ログ書込完了待合
            AplLog.getInstance().join();
        } else {
            // 引数が6つないときは引数不足時のメッセージを出力し終了
            System.out.println(WksConstants.OTHER_ARGS_MSG);
        }

    }
}
```

Mainの共有フィールド `log`、`outLog`、`getDateTime` を削除する。
6引数の検証、ワークフローのException捕捉は維持し、開始・ERR・終了をAplLogへ渡す。
`join()` の割り込みに伴う `InterruptedException` をthrowsへ追加する。
初回ファイルオープン失敗はワークフロー実行前のIOExceptionとして上位へ伝播する。
引数不正時はUSAGEのみ表示し、AplLogの生成・ファイル作成を行わない。

---

### 変更ファイル：`SshCommand.java`

各部品の共有ログ参照を削除し、AplLog呼び出しへ置換する。

```java
public class SshCommand {
    private static String[] runCommand(ConnectionInformation ci, String cmd) throws JSchException, InterruptedException, IOException {
        // Active待機
        waitForBecomeActive(ci);

        // コマンド記述
        SshExec sshExec = new SshExec(ci, cmd);

        // 実行コマンドをlogに出力
        AplLog.getInstance().outCommandLog(ci.host(), cmd);

        // コマンド実行
        String[] ret = sshExec.execute();

        // 返り値をログに追加
        AplLog.getInstance().outOutputLog(ci.host(), ret);

        // 返り値を返却
        return ret;
    }
}
```

`movePaperMc` 内で直接SshExecを操作する各コマンドも、実行前にCMD、終了後にOUT配列を記録する。
SSH標準出力は従来どおりコマンド終了後の一括受け渡しであり、実行中の逐次取得は今回実装しない。
PaperMCバージョンファイルの仕様は変更しない。

---

### 変更ファイル：`BashExec.java`

共通実行メソッドを `runCommand(String cmd, boolean outLogFlag)` へ変更する。

| 呼び出し元                  | フラグ             | ログ                                                         |
|-----------------------------|--------------------|--------------------------------------------------------------|
| `update` / `upgrade`        | true               | プロセス終了後、localhostのCMDと標準出力各行のOUT            |
| `waitOneMin` / `getNowTime` | false              | コマンド・標準出力はログに追加しない。戻り値や待機処理は維持 |
| `shutdown`                  | 共通処理を使わない | 従来の遅延再起動を起動し、localhostのCMDを追加               |

終了待ち後に標準出力を読み取る順序は今回変更しない。

---

### 変更ファイル：`WksWorkFlow.java`

- 初期疎通確認前は `outApplicationLog(ci.host(), OTHER_CONNECTION_CHECK)` で対象を記録する。
- 疎通不可時は `outExceptionLog(localhost, OTHER_NOT_ALIVE_MSG)` で文字列ERRを記録し、保守せず復帰する。
- リモートおよび管理端末の区切りログは廃止し、各行のIP欄で識別する。
- `isAllAlive` のthrowsへログ初期化に伴うFileNotFoundExceptionを追加する。
- No.310の並列更新・join・例外再送出、および保守工程の順序は変更しない。

---

### 変更ファイル：`WksConstants.java`

| 定数名                    | 変更後の値                                                 |
|---------------------------|------------------------------------------------------------|
| `LOG_LEVEL_SYSTEM`        | `SYS`                                                      |
| `LOG_LEVEL_APPLICATIONS`  | `APL`                                                      |
| `LOG_LEVEL_COMMAND`       | `CMD`                                                      |
| `LOG_LEVEL_OUTPUT`        | `OUT`                                                      |
| `LOG_LEVEL_EXCEPTION`     | `ERR`                                                      |
| `LOG_PARTS_DOL`           | `$ `                                                       |
| `LOG_PARTS_LF`            | `\n`                                                       |
| `LOG_FORMAT`              | `[%s %s %s]: %s` ＋ LF                                     |
| `LOG_START_MESSAGE`       | `BATCH MAINTENANCE START`                                  |
| `LOG_FINISH_MESSAGE`      | `BATCH MAINTENANCE END`                                    |
| `OTHER_IP_LOCALHOST`      | `localhost`                                                |
| `OTHER_NOT_ALIVE_MSG`     | `Servers Not Active`（WARNING接頭辞を削除しERR種別で表現） |
| `OTHER_CONNECTION_CHECK`  | `Checking Connection`（接続先はIP欄へ移動）                |
| `OTHER_PAPERMC_EXCEPTION` | `No official version was found.`（Exception接頭辞を削除）  |

旧定数 `LOG_START_TIME`・`LOG_END_TIME`・`LOG_COMMAND`・`LOG_SPLIT`・`LOG_THIS_SERVER` は削除する。

---

## 変更による動作の差異

| 観点                   | 変更前                      | 変更後                                |
|------------------------|-----------------------------|---------------------------------------|
| ログ所有者             | Mainの公開リスト            | AplLogのprivateリスト                 |
| 書き出し時点           | 終了時に全件出力            | 受付済みログを専用スレッドで随時出力  |
| ファイル名の時刻       | 終了時刻                    | AplLog初回生成時刻                    |
| ログ行                 | 内容ごとの独自形式          | 日時・サーバIP・レベル付き共通形式    |
| 接続先識別             | 初期確認と区切りログ        | 各行のIP欄                            |
| 例外                   | Mainが文字列化し1要素で保持 | AplLogが全体を文字列化し各行へERR付加 |
| 固定待機・時刻取得ログ | コマンド・結果を記録        | Bashのフラグfalseで抑止               |
| 終了処理               | Main.outLog                 | 最終ログ追加→endOfLogOutput→join      |
| 文字コード             | Main.outLogでUTF-8指定      | AplLogのPrintStream既定値             |

---

## 変更後の処理フロー

```
Main: 6引数検証
  不正 → USAGE表示のみ
  正常 → getInstance（初回生成・ファイルオープン・自動start）
         → SYS開始 → ワークフロー
各部品: SYS / APL / CMD / OUT / ERRを受付キューへ追加
AplLog: キューを先頭からファイルへ出力 → 1秒休止 → 再確認（別スレッド）
Main: ワークフロー例外ならERRを追加
      → SYS終了 → endOfLogOutput → join
AplLog: 受付済みログを排出 → ファイルをclose → 終了
```

---

## 影響範囲

| 対象                                    | 影響                                                          |
|-----------------------------------------|---------------------------------------------------------------|
| Main・SshCommand・BashExec・WksWorkFlow | ログ呼び出し先と出力レベル・接続先を移管                      |
| AplLog                                  | 新規。単一インスタンス・出力スレッド・例外整形・終了管理      |
| WksConstants                            | ログ書式・レベル・文言を更新                                  |
| SshExec                                 | ソース変更なし。逐次出力は未対応                              |
| PaperUrlGen                             | 処理変更なし。正式版未検出のメッセージ定数変更が反映される    |
| 保守機能                                | 6引数・全機疎通・共有操作・OS並列更新・再起動の工程は変更なし |
| 過去CR                                  | No.301～310は変更履歴として維持し、現行設計書のみ更新         |

---

## ドキュメント更新結果

| ドキュメント群                                                                              | 更新結果                                                             |
|---------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `0_rd/010_APL_LOG.md`、`1_ui/110_APL_LOG.md`、`2_ss/210_AplLog.md`                          | 新規。単一インスタンス・書式・例外整形・随時出力・終了手順を定義済み |
| `0_rd/000_SYSTEM_OVERVIEW.md`、`1_ui/100_SYSTEM_OVERVIEW.md`、`2_ss/200_SYSTEM_OVERVIEW.md` | クラス構成、ログ形式・初回生成時刻・ライフサイクルを反映済み         |
| `0_rd/001_MAIN.md`、`1_ui/101_MAIN.md`、`2_ss/201_Main.md`                                  | 旧API削除、開始/ERR/終了通知・joinを反映済み                         |
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md`         | APLの疎通確認、ERRの全体中止、区切り廃止を反映済み                   |
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`              | CMD実行前・OUT終了後の移管と保留事項を反映済み                       |
| `0_rd/007_BASH_EXEC.md`、`1_ui/107_BASH_EXEC.md`、`2_ss/207_BashExec.md`                    | outLogFlag、待機・時刻取得のログ抑止と保留事項を反映済み             |
| `0_rd/006_SSH_EXEC.md`、`1_ui/106_SSH_EXEC.md`、`2_ss/206_SshExec.md`                       | 既存の配列返却仕様維持と将来対応を補足済み                           |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md`        | ログレベル・書式・開始終了・その他メッセージ変更を反映済み           |
| `0_rd/092_ERROR_HANDLING.md`、`1_ui/192_ERROR_HANDLING.md`、`2_ss/292_ERROR_HANDLING.md`    | ERR行単位記録・初期化失敗・終了待機・保留事項を反映済み              |
| `2_ss/204_PaperUrlGen.md`                                                                   | 正式版未検出時の具体的なメッセージを更新済み                         |

---

## 備考

- 将来対応1（ローカル大量出力時の停止リスク）：BashExecは終了待ちの後に標準出力を読み取るため、パイプが満杯になると子プロセスとJava側が相互待ちになる可能性がある。現在の運用では許容し、今回の変更では未解消とする。
- 将来対応2（SSH標準出力の逐次ログ化）：SshExecは標準出力を配列に蓄積し、コマンド終了後にSshCommandからAplLogへ渡す。実行中の進捗は記録しない現行仕様を許容し、将来対応1とあわせて見直す。
- 上記2件は解消済みではなく、明示的に保留した事項である。

- 全ログ発行処理の完了後、Mainで最終ログを追加してから終了通知し、joinでファイルクローズを待つ。
- 終了通知後のログ追加は破棄する。終了通知と追加が競合しない利用順序を前提とする。
- 終了したインスタンスの再生成・再起動は行わない。1回のバッチ実行を1プロセスで扱う。
- 親のjoin割り込み時の子キャンセルやErrorの捕捉は追加しない。
- PrintStreamの書き込み・クローズ時のエラーフラグ検査は追加しない。出力先の存在・権限・十分な容量を前提とする。
- PGレビュー完了。実機へのSSH・OS更新・再起動は未実施。

- `gradlew.bat --offline build --rerun-tasks` 成功。テストタスクは `NO-SOURCE`。
- PGレビューのローカル試験で、同時初回取得の唯一性、並列追記の全件出力、終了前の実ファイル出力、残ログ排出・close、例外の型・原因・Suppressed保持を確認した。
- 全体レビューでは、安全な検証用出力先で設定ファイル読込失敗時の開始/ERR/終了とログスレッド停止を確認した。実機検証を代替するものではない。

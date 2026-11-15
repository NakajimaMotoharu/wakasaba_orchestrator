# 変更管理資料 No.310：リモートサーバOS更新の並列化

## 基本情報

| 項目             | 内容                                                                                                                                    |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| 変更管理No       | 310                                                                                                                                     |
| 変更概要         | リモート6台のOS更新を並列化し、全スレッドの終了待ち・例外の親スレッドへの伝播および共有ログのスレッドセーフ化を実施                     |
| ステータス       | 適用済み（ソース確認・PGレビュー・設計書反映済み。実機検証未実施）                                                                      |
| 変更対象ファイル | `WksWorkFlow.java`、`Main.java`、`SshCommand.java`、`BashExec.java`                                                                     |
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`、`com.wks.workflow.UpdateThread`、`com.wks.main.Main`、`com.wks.cmd.SshCommand`、`com.wks.util.BashExec` |

---

## 変更背景・目的

### 変更前の課題

変更管理No.309で、全機疎通を前提として工程単位で全台を保守する方式へ変更した。
OS更新工程もサーバ0～5を直列に処理しており、あるサーバの更新中はほかのサーバの更新を開始できなかった。
また、共有ログは同期のない `ArrayList` であり、そのままでは並列タスクからの追記に対応できない。

### 変更目的

- リモートサーバ1台につき1スレッドを作成し、OS更新工程の所要時間短縮を図る。
- 同一サーバ内の `update` → `upgrade` の順序は維持する。
- 全更新スレッドの終了と例外有無を確認してから、再起動工程へ進む。
- 子スレッドの失敗を親スレッドへ伝え、従来どおりMainで異常終了ログを記録する。
- 複数スレッドから共有ログへ安全に追記できるようにする。

---

## 変更内容

ソース例は変更箇所の抜粋であり、関係しない処理は省略する。

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：`allUpdate` メソッド

**変更前**

```java
public class WksWorkFlow {
    private static void allUpdate(ConnectionInformation[] cis)
            throws JSchException, IOException, InterruptedException {
        for (ConnectionInformation ci : cis) {
            SshCommand.update(ci);
            SshCommand.upgrade(ci);
        }
    }
}
```

**変更後**

```java
public class WksWorkFlow {
    private static void allUpdate(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // 並列化用UpdateThreadの配列を作成
        UpdateThread[] updateThread = new UpdateThread[cis.length];

        // 各サーバについて実行
        for (int i = 0; i < cis.length; i++) {
            // Threadをサーバ情報を用いて初期化
            updateThread[i] = new UpdateThread(cis[i]);
            // Updateを並列実行
            updateThread[i].start();
        }

        // 各UpdateThreadに対して実行
        for (UpdateThread thread : updateThread) {
            // 各サーバのUpdateが完了するまで待機
            thread.join();
        }

        // 各UpdateThreadに対して実行
        for (UpdateThread thread : updateThread) {
            // Updateで例外が発生していた場合吸収
            thread.exceptionDetection();
        }
    }
}
```

- 接続情報配列と同じ長さの `UpdateThread[]` を作成する。
- 引数順に各スレッドを生成・`start()` し、リモート6台の更新を並行して実行する。全台の厳密な同時開始は保証しない。
- 全スレッドに対して順に `join()` した後、再度引数順に `exceptionDetection()` を呼ぶ。
- 保存された例外がなければ `allUpdate` から復帰し、次の全台再起動工程へ進む。
- 例外が検出された場合は再起動以降を実行しない。ほかのスレッドは先に開始済みであり、即時キャンセルは行わない。

#### 追加クラス：`UpdateThread`

`WksWorkFlow.java` 内のトップレベルクラスとして追加する。publicクラスや内部クラスではなく、
同一パッケージから利用する `Thread` のサブクラスである。

```java
class UpdateThread extends Thread {
    /**
     * サーバ情報
     */
    private final ConnectionInformation ci;
    /**
     * JSchExceptionが発生していた場合の一時格納先
     */
    private JSchException jSchException = null;
    /**
     * IOExceptionが発生していた場合の一時格納先
     */
    private IOException ioException = null;
    /**
     * InterruptedExceptionが発生していた場合の一時格納先
     */
    private InterruptedException interruptedException = null;
    /**
     * その他Exceptionが発生していた場合の一時格納先
     */
    private Exception exception = null;

    /**
     * コンストラクタ サーバ情報を用いて初期化
     *
     * @param ci サーバ情報
     */
    public UpdateThread(ConnectionInformation ci) {
        // サーバ情報フィールドを引数で初期化
        this.ci = ci;
    }

    /**
     * Updateの実行
     */
    @Override
    public void run() {
        try {
            // UPDATEコマンドの実行
            SshCommand.update(ci);
            // UPGRADEコマンドの実行
            SshCommand.upgrade(ci);
        } catch (JSchException e) {
            // JSchExceptionが発生した場合例外格納
            jSchException = e;
        } catch (IOException e) {
            // IOExceptionが発生した場合例外格納
            ioException = e;
        } catch (InterruptedException e) {
            // InterruptedExceptionが発生した場合例外格納
            interruptedException = e;
        } catch (Exception e) {
            // その他Exceptionが発生した場合例外格納
            exception = e;
        }
    }

    /**
     * 例外が発生していた場合、throwsする
     *
     * @throws JSchException SSHコマンド実行に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     * @throws IOException Bashコマンド実行に失敗
     */
    public void exceptionDetection() throws JSchException, IOException, InterruptedException {
        // JSchExceptionが発生していた場合throws
        if (jSchException != null) throw jSchException;
        // IOExceptionが発生していた場合throws
        if (ioException != null) throw ioException;
        // InterruptedExceptionが発生していた場合throws
        if (interruptedException != null) throw interruptedException;
        // その他Exceptionが発生していた場合throws
        if (exception != null) throw new RuntimeException(exception);
    }
}
```

| 例外                                     | 子スレッド内の保持先   | 親スレッドでの扱い                                     |
|------------------------------------------|------------------------|--------------------------------------------------------|
| `JSchException`                          | `jSchException`        | 同じ例外を再送出                                       |
| `IOException`                            | `ioException`          | 同じ例外を再送出                                       |
| `InterruptedException`                   | `interruptedException` | 同じ例外を再送出                                       |
| その他の `Exception`（実行時例外を含む） | `exception`            | `new RuntimeException(exception)` で原因を保持して送出 |

`update` が例外で終了したサーバでは、そのスレッドの `upgrade` は実行しない。
正常に進行しているほかのサーバの更新は継続し、全スレッド終了後に例外検出を行う。
複数台で失敗した場合、引数順で最初に例外を再送出した時点で終了し、例外の集約は行わない。
`Error` は `Exception` の捕捉対象外とする。

---

### 変更ファイル：`Main.java`

#### 変更箇所：共有ログの宣言と初期化

**変更前**

```java
public class Main {
    public static final ArrayList<String> log = new ArrayList<>();
}
```

**変更後**

```java
public class Main {
    public static final List<String> log = new CopyOnWriteArrayList<>();
}
```

宣言型を `List<String>`、実体を `CopyOnWriteArrayList<String>` とし、
複数の更新スレッドから同じリストへ追記できるようにする。
開始・終了時刻、スタックトレース、ログファイルの出力先・形式は変更しない。

---

### 変更ファイル：`SshCommand.java`・`BashExec.java`・`WksWorkFlow.java`

#### 変更箇所：共有ログ参照の型

各クラスの共有ログ参照を、`ArrayList<String>` から `List<String>` へ変更する。
以下は `SshCommand` の例であり、ほかの2クラスも同様である。

**変更前**

```java
public class SshCommand {
    private static final ArrayList<String> log = Main.log;
}
```

**変更後**

```java
public class SshCommand {
    private static final List<String> log = Main.log;
}
```

- 各クラスは従来どおり同一の `Main.log` を参照する。
- SSHのコマンド実行・標準出力取得は変更せず、時刻取得の `BashExec.getNowTime` を含め追記先だけを変更する。
- ローカルの標準出力収集用 `ArrayList` は変更しない。
- `add`・`addAll` 単位の書き込みは安全だが、コマンド行とその出力を一体として排他するわけではない。
  各サーバのログは混在し、記録順序は実行タイミングに依存する。新しい接続先ラベルは追加しない。

---

## 変更による動作の差異

| 観点               | 変更前                                        | 変更後                                               |
|--------------------|-----------------------------------------------|------------------------------------------------------|
| OS更新工程         | サーバ0のupdate・upgrade完了後、サーバ1へ進む | サーバ0～5それぞれのスレッドで更新                   |
| 同一サーバ内       | update → upgrade                              | 変更なし                                             |
| 再起動への移行     | 直列ループの正常完了後                        | 全スレッドのjoinと例外検出が正常完了した後           |
| 更新中の例外       | 呼び出し元へそのまま伝播                      | 子で保存し、全スレッド終了後に親で再送出             |
| ほかの更新への影響 | 例外以降のサーバは未実行                      | 開始済みのほかの更新は終了まで待つ。キャンセルしない |
| 共有ログ           | ArrayList                                     | List型のCopyOnWriteArrayList                         |
| ログ順序           | 直列の実行順                                  | 更新中はサーバ間の順序が不定                         |
| 並列化対象         | なし                                          | リモート6台のOS更新工程のみ                          |

---

## 変更後の処理フロー

```
全機疎通確認 → 両サービス停止・共通60秒待機 → 共有アンマウント・確認
→ allUpdate
    サーバ0: update → upgrade
    サーバ1: update → upgrade
    サーバ2: update → upgrade
    サーバ3: update → upgrade
    サーバ4: update → upgrade
    サーバ5: update → upgrade
    （上記6スレッドが並行動作）
    → 全スレッドjoin → 引数順にexceptionDetection
       例外なし: 全台再起動 → 復帰待ち → 共有再マウント
                 → PaperMCバックアップ・取得・配置
                 → PaperMC・Schubert起動 → 管理端末メンテナンス → 終了ログ
       例外あり: 後続工程中止 → Mainでスタックトレース・終了ログ出力 → 手動復旧
```

---

## 影響範囲

| 対象                                    | 影響                                                         |
|-----------------------------------------|--------------------------------------------------------------|
| リモートサーバ0～5（NAS含む）           | OS更新をサーバ別スレッドで並列化                             |
| 管理端末                                | 自端末のOS更新は並列化せず、従来どおり最後に実行             |
| 共有操作・再起動・復帰確認・アプリ操作  | 並列化しない。工程順序も変更なし                             |
| Main・SshCommand・BashExec・WksWorkFlow | 共有ログの宣言型をListへ統一                                 |
| 起動引数・接続ファイル・定数            | 変更なし（6引数、既存4行形式）                               |
| 異常時運用                              | 親で例外再送出後は従来どおり後続停止・手動復旧               |
| 過去CR                                  | No.309等は当時の履歴として変更せず、現行設計書へNo.310を反映 |

---

## ドキュメント更新結果

| ドキュメント群                                                                              | 更新結果                                                           |
|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `0_rd/000_SYSTEM_OVERVIEW.md`、`1_ui/100_SYSTEM_OVERVIEW.md`、`2_ss/200_SYSTEM_OVERVIEW.md` | OS更新限定の並列化、全join・例外経路、共有ログを反映済み           |
| `0_rd/001_MAIN.md`、`1_ui/101_MAIN.md`、`2_ss/201_Main.md`                                  | List型・CopyOnWriteArrayListのログ定義を反映済み                   |
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md`         | 並列実行・完了待ち・例外検出、同一ファイルのUpdateThreadを反映済み |
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`              | 共有ログ参照型と並列呼び出し時のログ仕様を反映済み                 |
| `0_rd/007_BASH_EXEC.md`、`1_ui/107_BASH_EXEC.md`、`2_ss/207_BashExec.md`                    | 共有ログ参照型と並列更新中の時刻取得を反映済み                     |
| `0_rd/092_ERROR_HANDLING.md`、`1_ui/192_ERROR_HANDLING.md`、`2_ss/292_ERROR_HANDLING.md`    | 子で保持→全join→親で再送出→Mainの例外経路を反映済み                |

---

## 備考

- スレッド開始は順番に行うが、あるサーバの更新完了を待って次を開始するわけではない。
- 全スレッドの終了後に例外検出するため、更新失敗は即時にほかのタスクを止める契機にはならない。
- `join()` 中に親スレッドが割り込まれた場合は `InterruptedException` を上位へ委譲する。子スレッドのキャンセル処理は追加しない。
- ログのスレッドセーフ化は書き込み競合による欠落・破損を防ぐもので、サーバ別のログ整列は保証しない。
- PGレビューで、IOException保存、その他Exception伝播、共有ログの非同期追記を確認済み。
- 実機へのSSH・OS更新・再起動は本変更管理の作成時に実施しない。

- `gradlew.bat --offline build --rerun-tasks` 成功。テストタスクは `NO-SOURCE`。
- PGレビュー時のローカル確認では、共有ログへの6スレッド・計6,000行の追記で欠落・nullがないことと、保持した4種類の例外の再送出を確認した。実機の更新処理を検証したものではない。

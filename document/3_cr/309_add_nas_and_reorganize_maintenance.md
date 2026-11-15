# 変更管理資料 No.309：NASサーバ追加および全台メンテナンスワークフロー再編

## 基本情報

| 項目             | 内容                                                                                                                                                         |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 変更管理No       | 309                                                                                                                                                          |
| 変更概要         | NASサーバ追加、全機疎通を前提とした工程単位のメンテナンス、共有のアンマウント・再マウント、管理端末での待機および復帰待ち上限の追加                          |
| ステータス       | 適用済み（ソース確認・PGレビュー・設計書反映済み。実機検証未実施）                                                                                           |
| 変更対象ファイル | `Main.java`、`WksWorkFlow.java`、`SshCommand.java`、`SshExec.java`、`BashExec.java`、`WksConstants.java`                                                     |
| 完全修飾クラス名 | `com.wks.main.Main`、`com.wks.workflow.WksWorkFlow`、`com.wks.cmd.SshCommand`、`com.wks.util.SshExec`、`com.wks.util.BashExec`、`com.wks.parts.WksConstants` |

---

## 変更背景・目的

### 変更前の課題

管理対象はリモートサーバ5台と管理端末であり、各サーバの保守処理を最後まで実行してから次のサーバへ進んでいた。
変更管理No.305による非アクティブサーバの個別スキップでは、共有提供元のNASを含む全体の依存関係を扱えない。
また、NASを保守する前の共有解除と、全台復帰後の共有再接続が必要となった。

### 変更目的

- NASをサーバ5として追加し、接続情報ファイルを6つ受け付ける。
- 全機疎通を保守開始条件とし、1台でも疎通不可なら全体の保守処理を開始しない。
- 同じメンテナンス時間帯に全台を工程単位で処理する。各工程内の処理は直列であり、並列実行は行わない。
- NAS以外のリモートサーバで `/nas`・`/share` を解除・復旧し、期待するマウント有無を確認する。
- 停止後と再起動指示後の固定待機を管理端末に集約し、再起動後の復帰待ちに上限を設ける。

---

## 変更内容

ソース例は変更箇所の抜粋であり、関係しないログ処理・コメント等は省略する。

### 変更ファイル：`Main.java`

#### 変更箇所：`main` の引数検証

**変更前**

```java
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 5) {
            WksWorkFlow.execScheduledJob(args);
        } else {
            System.out.println(WksConstants.OTHER_ARGS_MSG);
        }
    }
}
```

**変更後**

```java
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 6) {
            WksWorkFlow.execScheduledJob(args);
        } else {
            System.out.println(WksConstants.OTHER_ARGS_MSG);
        }
    }
}
```

引数数のJavadoc・コメントとUSAGEも6へ変更する。接続ファイルの4行形式は変更しない。

---

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：サーバ単位から工程単位への再編

**変更前**

```
サーバ0の全工程
→ サーバ1（停止・待機・OS更新・バックアップ・取得・配置・再起動・起動）
→ サーバ2の全工程
→ サーバ3（停止・待機・OS更新・再起動・起動）
→ サーバ4の全工程 → 管理端末
各サーバの初期疎通不可時は、そのサーバだけをスキップ
```

**変更後**

```java
public class WksWorkFlow {
    public static void execScheduledJob(String[] servers)
            throws IOException, InterruptedException, JSchException {
        ConnectionInformation[] cis = setConnectionInformation(servers);
        if (!isAllAlive(cis)) {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
            return;
        }
        systemStop(cis);
        allUMount(cis);
        allUpdate(cis);
        allReboot(cis);
        BashExec.waitOneMin();
        allRebootComplete(cis);
        allMount(cis);
        systemPreSetup(cis);
        systemStart(cis);
        log.add(String.format(WksConstants.LOG_SPLIT, WksConstants.LOG_THIS_SERVER));
        BashExec.update();
        BashExec.upgrade();
        BashExec.shutdown();
    }
}
```

- `setConnectionInformation` は6ファイルを先に読み込み、引数順で `ConnectionInformation[]` に格納する。
- `isAllAlive` は各確認前に `Checking Connection: %s` を記録する。最初の疎通不可で `false` を返す。
- 疎通不可時は `WARNING: Servers Not Active` を記録して復帰する。サービス停止、OS更新、管理端末メンテナンスも行わない。
- `allUpdate` はサーバ0～5に対し、1台ずつ `update` → `upgrade` を実行する。
- `allReboot` で全台へ再起動を指示した後、管理端末で一度だけ60秒待つ。
- `allRebootComplete` はサーバ0～5を順番に確認し、各呼び出しで `SERVER_REBOOT_WAIT_LIMIT`（600秒）を指定する。
- `allUMount`・`allMount` は `SERVER_NAS`（5）を除くサーバ0～4を対象とする。管理端末は含まない。
- `systemPreSetup` は全台復帰・全共有再マウント後にPaperMCのバックアップ・取得・SHA検証・配置を行う。
- `systemStart` はPaperMC、Schubertの順に起動する。

#### 変更箇所：サービス停止後の共通待機

```java
public class WksWorkFlow {
    private static void systemStop(ConnectionInformation[] cis)
            throws JSchException, IOException, InterruptedException {
        SshCommand.stopPaperMC(cis[WksConstants.SERVER_PAPER]);
        SshCommand.stopSchubert(cis[WksConstants.SERVER_SCHUBERT]);
        BashExec.waitOneMin();
    }
}
```

停止コマンドは順に実行するが、固定待機は両方の完了後に一度だけ行う。停止状態の動的ポーリングは行わない。

---

### 変更ファイル：`SshCommand.java`

#### 変更箇所：マウント・アンマウントと状態確認

```java
public class SshCommand {
    public static void mount(ConnectionInformation ci)
            throws JSchException, InterruptedException, IOException {
        runCommand(ci, WksConstants.CMD_MOUNT_NAS);
        runCommand(ci, WksConstants.CMD_MOUNT_SHARE);
        if (runCommand(ci, WksConstants.CMD_MOUNT_STATUS_NAS).length == 0) {
            throw new RuntimeException(WksConstants.OTHER_MOUNT_STATUS_EXCEPTION);
        }
        if (runCommand(ci, WksConstants.CMD_MOUNT_STATUS_SHARE).length == 0) {
            throw new RuntimeException(WksConstants.OTHER_MOUNT_STATUS_EXCEPTION);
        }
    }

    public static void umount(ConnectionInformation ci)
            throws JSchException, InterruptedException, IOException {
        runCommand(ci, WksConstants.CMD_UMOUNT_NAS);
        runCommand(ci, WksConstants.CMD_UMOUNT_SHARE);
        if (runCommand(ci, WksConstants.CMD_MOUNT_STATUS_NAS).length != 0) {
            throw new RuntimeException(WksConstants.OTHER_MOUNT_STATUS_EXCEPTION);
        }
        if (runCommand(ci, WksConstants.CMD_MOUNT_STATUS_SHARE).length != 0) {
            throw new RuntimeException(WksConstants.OTHER_MOUNT_STATUS_EXCEPTION);
        }
    }
}
```

`runCommand` は従来の実行・ログ追記に加え、標準出力の `String[]` を返す。
使用中等で解除できず `findmnt` に出力が残る場合は例外で中止する。解除のリトライは行わない。
共有元の一致や終了コードは検証せず、状態確認コマンドが利用可能であることを環境前提とする。

#### 変更箇所：復帰待ちの公開・上限指定

```java
public class SshCommand {
    public static void waitForBecomeActive(ConnectionInformation ci, int limit)
            throws JSchException, InterruptedException, IOException {
        long startUnixTime = BashExec.getNowTime();
        SshExec sshExec = new SshExec(ci, WksConstants.CMD_DO_NOTHING);
        while (!sshExec.isAlive()) {
            if (startUnixTime + limit < BashExec.getNowTime()) {
                throw new RuntimeException(String.format(
                        WksConstants.OTHER_REBOOT_EXCEPTION, ci.toString()));
            }
            Thread.sleep(1000);
        }
    }

    public static void waitForBecomeActive(ConnectionInformation ci)
            throws JSchException, InterruptedException, IOException {
        waitForBecomeActive(ci, Integer.MAX_VALUE);
    }
}
```

`limit` の単位は秒。再起動後の明示待機は600、通常のコマンド実行前待機は1引数版（`Integer.MAX_VALUE` 秒）を用いる。
失敗した接続試行の後に期限超過を確認するため、600秒は厳密な締切ではない。最後の試行で接続できれば期限後でも正常復帰として続行する。

SSH経由の `waitOneMin(ConnectionInformation)` は削除し、管理端末の `BashExec.waitOneMin()` に移す。

---

### 変更ファイル：`SshExec.java`

#### 変更箇所：`isAlive` の接続タイムアウト

**変更前**：`session.connect()`

**変更後**

```java
public class SshExec {
    public boolean isAlive() throws JSchException {
        Session session = getSessionInstance();
        try {
            session.connect(60000);
        } catch (JSchException e) {
            return false;
        }
        session.disconnect();
        return true;
    }
}
```

疎通確認の接続タイムアウトを60000ミリ秒（60秒）に固定する。
初回疎通確認と復帰待ちで共用する。通常のコマンド実行用 `execute()` は変更しない。

---

### 変更ファイル：`BashExec.java`

#### 変更箇所：管理端末での固定待機・UNIX時刻取得

```java
public class BashExec {
    public static void waitOneMin() throws IOException, InterruptedException {
        runCommand(WksConstants.CMD_WAIT_ONE_MIN);
    }

    public static long getNowTime() throws IOException, InterruptedException {
        return Long.parseLong(runCommand(WksConstants.CMD_GET_TIME)[0]);
    }
}
```

`runCommand(String)` の戻り値を `void` から `String[]` に変更し、標準出力の全行をログと配列の両方に格納して返す。
`getNowTime` は `date '+%s'` の先頭行を `long` に変換する。既存のOS更新・遅延再起動方式は維持する。

---

### 変更ファイル：`WksConstants.java`

| 定数名                                          | 変更後の値                                    | 用途                             |
|-------------------------------------------------|-----------------------------------------------|----------------------------------|
| `SERVER_TEST` / `SERVER_PAPER` / `SERVER_LLM`   | `0` / `1` / `2`                               | 接続情報配列の添字               |
| `SERVER_SCHUBERT` / `SERVER_VPN` / `SERVER_NAS` | `3` / `4` / `5`                               | 接続情報配列の添字               |
| `CMD_MOUNT_NAS` / `CMD_UMOUNT_NAS`              | `sudo mount /nas` / `sudo umount /nas`        | NAS共有操作                      |
| `CMD_MOUNT_STATUS_NAS`                          | `findmnt /nas`                                | NAS共有のマウント有無確認        |
| `CMD_MOUNT_SHARE` / `CMD_UMOUNT_SHARE`          | `sudo mount /share` / `sudo umount /share`    | SHARE共有操作                    |
| `CMD_MOUNT_STATUS_SHARE`                        | `findmnt /share`                              | SHARE共有のマウント有無確認      |
| `CMD_GET_TIME`                                  | `date '+%s'`                                  | UNIX時刻（秒）取得               |
| `OTHER_ARGS_MSG`                                | `The length of the arguments must be 6.`      | 必須引数数を5から6へ変更         |
| `OTHER_NOT_ALIVE_MSG`                           | `WARNING: Servers Not Active`                 | 個別スキップから全体中止へ変更   |
| `OTHER_CONNECTION_CHECK`                        | `Checking Connection: %s`                     | 初期疎通の確認対象               |
| `OTHER_MOUNT_STATUS_EXCEPTION`                  | `The mount status is not normal.`             | マウント有無が期待と異なる場合   |
| `SERVER_REBOOT_WAIT_LIMIT`                      | `600`                                         | 各サーバの再起動後復帰待ち（秒） |
| `OTHER_REBOOT_EXCEPTION`                        | `The server did not restart successfully: %s` | 復帰待ち超過。接続情報を埋め込む |

---

## 変更による動作の差異

| 観点                            | 変更前                         | 変更後                                       |
|---------------------------------|--------------------------------|----------------------------------------------|
| 対象                            | リモート5台＋管理端末          | リモート6台＋管理端末                        |
| 疎通不可                        | 該当サーバだけスキップ         | 全体の保守を開始しない                       |
| 工程                            | 1台の全処理完了後に次サーバ    | 工程ごとに対象全台へ直列実行                 |
| 共有                            | 制御なし                       | サーバ0～4で解除・状態確認・再接続・状態確認 |
| 停止後待機                      | 各サービス停止後、接続先で60秒 | 両停止コマンド完了後、管理端末で1回60秒      |
| 再起動後待機                    | コマンド実行前の接続待ち       | 全台指示後60秒＋各台600秒目安の接続待ち      |
| PaperMCバックアップ・取得・配置 | 再起動前                       | 全台復帰・全共有再マウント後                 |
| リモート識別ログ                | 各サーバ開始区切り             | 初期疎通確認対象を記録                       |

### 実行インタフェース

```bash
java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file> <server3_file> <server4_file> <server5_file>
```

---

## 変更後の処理フロー

```
6ファイル読込 → 全機初期疎通確認
  疎通不可 → 対象ログ＋警告 → 保守せずMainへ復帰 → 終了ログ出力
  全機疎通可:
    PaperMC停止 → Schubert停止 → 管理端末で60秒待機
    → サーバ0～4: /nas・/shareアンマウント、状態確認
    → サーバ0～5: update → upgrade
    → サーバ0～5: 再起動指示 → 管理端末で60秒待機
    → サーバ0～5: SSH復帰待ち（各600秒目安）
    → サーバ0～4: /nas・/shareマウント、状態確認
    → PaperMCバックアップ → 最新正式版取得・バージョンファイル出力 → SHA検証・配置
    → PaperMC起動 → Schubert起動
    → 管理端末: update → upgrade → 60秒遅延再起動を予約
    → 終了ログ出力
例外発生 → 後続工程中止 → Mainでスタックトレース・終了ログ出力 → 手動復旧
```

---

## 影響範囲

| 対象                 | 影響                                                                 |
|----------------------|----------------------------------------------------------------------|
| 起動手順             | NAS用ファイルを6番目に追加。ファイル形式は変更なし                   |
| サーバ0～4           | 共有の解除・復旧を追加し、役割固有の処理を工程単位に移動             |
| サーバ5（NAS）       | OS更新・再起動・疎通待ちに追加。自身への共有操作は除外               |
| 管理端末             | 共有操作の対象外。固定待機と時刻取得を追加。OSメンテナンスは最後     |
| 異常時運用           | 例外で後続停止。個別回復・自動ロールバックは行わず手動対応           |
| No.305のスキップ仕様 | 今回の全機疎通前提へ置換。過去CRは履歴として維持                     |
| No.307・308          | 正式版選択・バージョンファイル形式は維持。実行時点のみ再起動後へ移動 |

---

## ドキュメント更新結果

| ドキュメント群                                                                                 | 更新結果                                                       |
|------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `0_rd/000_SYSTEM_OVERVIEW.md`、`1_ui/100_SYSTEM_OVERVIEW.md`、`2_ss/200_SYSTEM_OVERVIEW.md`    | NAS追加、6引数、工程単位フロー、全機疎通・異常時方針を反映済み |
| `0_rd/001_MAIN.md`、`1_ui/101_MAIN.md`、`2_ss/201_Main.md`                                     | 必須引数数を6へ更新済み                                        |
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md`            | 全工程・ヘルパーメソッド・共有操作・復帰待ちを反映済み         |
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`                 | 待機公開・上限、返却値、マウント状態確認を反映済み             |
| `0_rd/006_SSH_EXEC.md`、`1_ui/106_SSH_EXEC.md`、`2_ss/206_SshExec.md`                          | 疎通確認の60秒タイムアウトを反映済み                           |
| `0_rd/007_BASH_EXEC.md`、`1_ui/107_BASH_EXEC.md`、`2_ss/207_BashExec.md`                       | ローカル固定待機、時刻取得、標準出力配列返却を反映済み         |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md`           | 追加・変更定数を反映済み                                       |
| `0_rd/090_SERVER_FILE_SPEC.md`、`1_ui/190_SERVER_FILE_SPEC.md`、`2_ss/290_SERVER_FILE_SPEC.md` | 6引数の起動例とNAS対応を反映済み                               |
| `0_rd/092_ERROR_HANDLING.md`、`1_ui/192_ERROR_HANDLING.md`、`2_ss/292_ERROR_HANDLING.md`       | 状態不一致・復帰待ち超過、後続中止・手動復旧を反映済み         |

---

## 備考

- 「全台同時メンテナンス」は同一時間帯の工程単位処理を意味し、並列処理を意味しない。
- NAS以外のリモート5台には `/nas`・`/share` の設定と必要なコマンド・権限が存在する前提とする。
- 復帰待ちは600秒を厳密な締切とせず、最後の接続試行が成功すれば後続へ進む。
- 疎通確認はSSH接続可否であり、boot_idやNAS共有サービスの準備完了を直接確認しない。共有は再マウント後に有無を確認する。
- マウント状態は `findmnt` の標準出力の有無で判定する。確認コマンド自体の失敗や共有元の取り違え検知は追加しない。
- 例外時はSchubertの起動を含めて残りの工程を実行しない。ログ確認後、運用者が手動復旧する。
- PGレビューは完了。実機へのSSH・再起動・共有操作は本変更管理作成時に実行しない。

- `gradlew.bat --offline build --rerun-tasks` 成功。テストタスクは `NO-SOURCE` で、自動テストおよび実機検証は実施していない。

# 変更管理資料 No.305：非アクティブサーバの処理スキップ

## 基本情報

| 項目       | 内容                                                                                   |
|----------|--------------------------------------------------------------------------------------|
| 変更管理No   | 305                                                                                  |
| 変更概要     | 処理開始時点で疎通不可のリモートサーバについて、SSHコマンド実行をスキップする                                             |
| ステータス    | 適用済み（ソース確認・設計書反映済み）                                                                  |
| 変更対象ファイル | `WksWorkFlow.java`、`SshCommand.java`、`WksConstants.java`                             |
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`、`com.wks.cmd.SshCommand`、`com.wks.parts.WksConstants` |

---

## 変更背景・目的

### 変更前の課題

変更前のリモートサーバ処理では、各SSHコマンド実行前に `waitForBecomeActive` により接続可能になるまで待機していた。

この制御は、サーバ再起動後にSSH接続可能状態へ戻るまで待機する用途では有効である。一方で、処理開始時点から対象サーバが停止している場合や、
ネットワーク的に到達不能な場合も同じ待機処理に入るため、対象サーバが復旧しない限り処理が完了しない課題があった。

### 変更目的

各リモートサーバの処理開始時点で疎通確認を行い、もともと非アクティブのサーバに対する保守処理をスキップする。

これにより、任意のリモートサーバが停止している場合でも、ワークフロー全体が永続的な待機状態に陥らないようにする。

---

## 変更内容

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：`execScheduledJob` メソッド内、サーバ0～3の各処理ブロック

各リモートサーバの接続情報を読み込み、接続情報をログへ出力した後、`SshCommand.isAlive` による疎通確認を追加した。

疎通可能な場合のみ従来の保守処理を実行し、疎通不可の場合は対象サーバの処理を実行せず、警告メッセージをログへ追加する。

**変更後の処理例**

```java
public class WksWorkFlow {
    public static void execScheduledJob(String[] servers) {
        ConnectionInformation ci0 = ConnectionInformation.getCiFromFile(servers[0]);
        log.add(String.format(WksConstants.LOG_SPLIT, ci0));

        if (SshCommand.isAlive(ci0)) {
            SshCommand.update(ci0);
            SshCommand.upgrade(ci0);
            SshCommand.shutdown(ci0);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }
    }
}
```

同じ判定を、サーバ1（PaperMCサーバ）、サーバ2、サーバ3（Schubertサーバ）にも追加した。

---

### 変更ファイル：`SshCommand.java`

#### 追加メソッド：`isAlive`

```java
public static boolean isAlive(ConnectionInformation ci) throws JSchException {
    SshExec sshExec = new SshExec(ci, WksConstants.CMD_DO_NOTHING);

    return sshExec.isAlive();
}
```

`WksWorkFlow` からサーバ疎通確認を行うため、`SshExec.isAlive()` を呼び出す公開メソッドを追加した。

このメソッドは接続可否のみを判定し、SSHコマンドの実行や標準出力ログの追加は行わない。

---

### 変更ファイル：`WksConstants.java`

#### 追加定数：`OTHER_NOT_ALIVE_MSG`

```java
public static final String OTHER_NOT_ALIVE_MSG = "WARNING: Server Not Active";
```

処理開始時点で対象サーバへ疎通できない場合に、ログへ出力する警告メッセージを追加した。

---

## 変更による動作の差異

### 処理開始時点で対象サーバがアクティブな場合

従来どおり、対象サーバの保守処理を実行する。

サーバ再起動後の後続処理については、既存の `runCommand` 内で `waitForBecomeActive` が実行されるため、
再起動完了後にSSH接続可能となるまで待機する。

### 処理開始時点で対象サーバが非アクティブの場合

対象サーバの保守処理は実行しない。

ログには対象サーバの接続情報区切りを出力した後、以下の警告メッセージを追加する。

```text
WARNING: Server Not Active
```

その後、次のサーバ処理へ進む。

---

## 対象サーバ別の変更後フロー

### サーバ0

```
1. ConnectionInformation.getCiFromFile(servers[0]) → ci0
2. log に ci0 の接続情報を区切りログとして追記
3. SshCommand.isAlive(ci0)
4. true の場合のみ update → upgrade → shutdown を実行
5. false の場合は WARNING: Server Not Active をログへ追記し、サーバ1へ進む
```

### サーバ1（PaperMCサーバ）

```
1. ConnectionInformation.getCiFromFile(servers[1]) → ci1
2. log に ci1 の接続情報を区切りログとして追記
3. SshCommand.isAlive(ci1)
4. true の場合のみ PaperMC停止 → 60秒待機 → update → upgrade → バックアップ → PaperMC更新 → shutdown → PaperMC起動を実行
5. false の場合は WARNING: Server Not Active をログへ追記し、サーバ2へ進む
```

### サーバ2

```
1. ConnectionInformation.getCiFromFile(servers[2]) → ci2
2. log に ci2 の接続情報を区切りログとして追記
3. SshCommand.isAlive(ci2)
4. true の場合のみ update → upgrade → shutdown を実行
5. false の場合は WARNING: Server Not Active をログへ追記し、サーバ3へ進む
```

### サーバ3（Schubertサーバ）

```
1. ConnectionInformation.getCiFromFile(servers[3]) → ci3
2. log に ci3 の接続情報を区切りログとして追記
3. SshCommand.isAlive(ci3)
4. true の場合のみ Schubert停止 → 60秒待機 → update → upgrade → shutdown → Schubert起動を実行
5. false の場合は WARNING: Server Not Active をログへ追記し、自サーバ処理へ進む
```

---

## 影響範囲

| 対象                             | 影響                                         |
|--------------------------------|--------------------------------------------|
| `WksWorkFlow.execScheduledJob` | サーバ0～3の各処理ブロックに、処理開始時点の疎通確認とスキップ分岐を追加      |
| `SshCommand`                   | `isAlive` メソッドを追加し、ワークフローから疎通確認を呼び出せるように変更 |
| `WksConstants`                 | 非アクティブ時にログへ出力する `OTHER_NOT_ALIVE_MSG` を追加  |
| `SshExec`                      | 変更なし。既存の `isAlive()` による疎通判定を利用            |
| `Main`                         | 変更なし                                       |
| 実行インタフェース                      | 変更なし。コマンドライン引数数および接続情報ファイル形式に変更なし          |
| ログ出力                           | 非アクティブサーバ発生時、対象サーバの区切りログ後に警告メッセージが追加される    |
| 後続サーバ処理                        | 非アクティブサーバをスキップ後、次のサーバまたは自サーバ処理へ継続する        |

---

## 注意事項

- 本変更でスキップするのは、各サーバ処理ブロック開始時点で疎通不可のサーバである。
- 対象サーバが処理開始時点で疎通可能な場合は、従来どおり各SSHコマンド実行前に `waitForBecomeActive` が実行される。
- サーバ再起動後の `startPaperMC` および `startSchubert` では、既存仕様どおりSSH接続可能になるまで待機する。
- 非アクティブ判定時は対象サーバへの更新、バックアップ、停止、起動などの処理は一切実行しない。
- 自サーバ処理はSSHを利用しないため、本変更の疎通確認対象外である。

---

## ドキュメント更新結果

| ドキュメント群                                                                            | 更新結果                              |
|------------------------------------------------------------------------------------|-----------------------------------|
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md`  | サーバ0～3の処理開始時疎通確認と非アクティブ時スキップを追加済み |
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`       | `isAlive` メソッドの要求・設計を追加済み         |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md` | `OTHER_NOT_ALIVE_MSG` 定数を追加済み     |
| 各階層のシステム概要                                                                         | 非アクティブサーバをスキップして後続処理へ進む仕様を反映済み    |

---

## 備考

- 本変更は、処理開始時点から停止しているサーバによってワークフロー全体が完了しなくなる事象を回避するための変更である。
- サーバごとの実行順序、コマンドライン引数、接続情報ファイル仕様は変更しない。
- 非アクティブサーバが存在しても異常終了にはせず、警告ログを残して後続処理を継続する。

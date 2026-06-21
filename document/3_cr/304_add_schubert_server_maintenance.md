# 変更管理資料 No.304：管理対象サーバ追加およびSchubert保守処理の追加

## 基本情報

| 項目       | 内容                                                                                                       |
|----------|----------------------------------------------------------------------------------------------------------|
| 変更管理No   | 304                                                                                                      |
| 変更概要     | 管理対象サーバを1台追加し、OS更新・再起動およびSchubertシステムの停止・起動処理を追加                                                         |
| ステータス    | 適用済み（ソース確認・ビルド確認済み）                                                                                      |
| 変更対象ファイル | `Main.java`、`WksWorkFlow.java`、`SshCommand.java`、`WksConstants.java`                                     |
| 完全修飾クラス名 | `com.wks.main.Main`、`com.wks.workflow.WksWorkFlow`、`com.wks.cmd.SshCommand`、`com.wks.parts.WksConstants` |

---

## 変更背景・目的

### 変更前の課題

変更前のシステムは、3台のリモートサーバと自サーバを管理対象としていた。
リモートサーバの接続情報はコマンドライン引数として3ファイルを受け取り、以下の順序で保守処理を実行していた。

```
サーバ0 → サーバ1（PaperMC）→ サーバ2 → 自サーバ
```

新たに管理対象となったSchubertシステム稼働サーバはこのワークフローに含まれておらず、
OSパッケージ更新・再起動およびSchubertの停止・起動を別途実施する必要があった。

### 変更目的

Schubertシステム稼働サーバをサーバ3として既存ワークフローへ追加し、以下の処理を一括で自動実行できるようにする。

- Schubertシステムの停止
- 安全停止のための固定60秒待機
- OSパッケージリスト更新
- OSパッケージアップグレード
- サーバ再起動
- 再起動完了後のSchubertシステム起動

---

## 変更内容

### 変更ファイル：`Main.java`

#### 変更箇所：`main` メソッドの引数検証

**変更前**

```java
public class Main {
    public static void main(String[] args) {
        if (args.length == 3) {
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
    public static void main(String[] args) {
        if (args.length == 4) {
            WksWorkFlow.execScheduledJob(args);
        } else {
            System.out.println(WksConstants.OTHER_ARGS_MSG);
        }
    }
}
```

- 必須引数数を3から4へ変更した。
- Javadocおよび処理コメントの引数数も4へ更新した。

---

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：`execScheduledJob` メソッド内、サーバ3処理シーケンス

サーバ2の処理完了後、自サーバの処理開始前に以下の処理を追加した。

```java
public class WksWorkFlow {
    public static void execScheduledJob(String[] servers) {
        // サーバ3
        // サーバ3の接続情報を設定
        ConnectionInformation ci3 = ConnectionInformation.getCiFromFile(servers[3]);
        // サーバ3の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci3));
        // Schubert停止コマンドの実行
        SshCommand.stopSchubert(ci3);
        // 安全停止のため1分間待機
        SshCommand.waitOneMin(ci3);
        // updateコマンドの実行
        SshCommand.update(ci3);
        // upgradeコマンドの実行
        SshCommand.upgrade(ci3);
        // shutdownコマンドの実行
        SshCommand.shutdown(ci3);
        // Schubert起動コマンドの実行
        SshCommand.startSchubert(ci3);
    }
}
```

`shutdown` 後の `startSchubert` 呼び出しでは、既存の `runCommand` が実行前に `waitForBecomeActive` を呼び出すため、
サーバ3がSSH接続可能になるまで自動的に待機した後でSchubertを起動する。

---

### 変更ファイル：`SshCommand.java`

#### 追加メソッド：`startSchubert`

```java
public static void startSchubert(ConnectionInformation ci)
        throws JSchException, InterruptedException, IOException {
    runCommand(ci, WksConstants.CMD_SCHUBERT_START);
}
```

Schubert起動シェルを対象サーバ上で実行するラッパーメソッドを追加した。

#### 追加メソッド：`stopSchubert`

```java
public static void stopSchubert(ConnectionInformation ci)
        throws JSchException, InterruptedException, IOException {
    runCommand(ci, WksConstants.CMD_SCHUBERT_END);
}
```

Schubert停止シェルを対象サーバ上で実行するラッパーメソッドを追加した。

両メソッドとも既存の `runCommand` を経由するため、以下の共通処理が適用される。

- SSH接続可能になるまでのポーリング待機
- SSHコマンドの実行
- 実行コマンドおよび標準出力のログ記録
- `JSchException`・`InterruptedException`・`IOException` の上位伝播

---

### 変更ファイル：`WksConstants.java`

#### 追加したパス定数

| 定数名                              | 値                                              | 用途                  |
|----------------------------------|------------------------------------------------|---------------------|
| `PATH_PROD_SCHUBERT_APP`         | `/home/mini/schubert/`                         | Schubertアプリ格納ディレクトリ |
| `PATH_PROD_SCHUBERT_START_SHELL` | `PATH_PROD_SCHUBERT_APP + "start_schubert.sh"` | Schubert起動シェルパス     |
| `PATH_PROD_SCHUBERT_STOP_SHELL`  | `PATH_PROD_SCHUBERT_APP + "stop_schubert.sh"`  | Schubert停止シェルパス     |

#### 追加したコマンド定数

| 定数名                  | 値                                        | 用途             |
|----------------------|------------------------------------------|----------------|
| `CMD_SCHUBERT_START` | `"sh " + PATH_PROD_SCHUBERT_START_SHELL` | Schubert起動コマンド |
| `CMD_SCHUBERT_END`   | `"sh " + PATH_PROD_SCHUBERT_STOP_SHELL`  | Schubert停止コマンド |

#### 変更したメッセージ定数

| 定数名              | 変更前                                      | 変更後                                      |
|------------------|------------------------------------------|------------------------------------------|
| `OTHER_ARGS_MSG` | `The length of the arguments must be 3.` | `The length of the arguments must be 4.` |

---

## 変更による動作の差異

### 管理対象比較

| 観点           | 変更前                 | 変更後                       |
|--------------|---------------------|---------------------------|
| 必須引数数        | 3                   | 4                         |
| リモートサーバ数     | 3台                  | 4台                        |
| 処理対象         | サーバ0・サーバ1・サーバ2・自サーバ | サーバ0・サーバ1・サーバ2・サーバ3・自サーバ  |
| Schubert保守処理 | 対象外                 | 停止・60秒待機・OS更新・再起動・起動を自動実行 |

### 実行順序比較

**変更前**

```
サーバ0 → サーバ1（PaperMC）→ サーバ2 → 自サーバ
```

**変更後**

```
サーバ0 → サーバ1（PaperMC）→ サーバ2 → サーバ3（Schubert）→ 自サーバ
```

### 実行時間への影響

サーバ3に対して以下の処理が追加されるため、バッチ総所要時間は増加する。

- Schubert停止シェル実行時間
- 固定60秒の安全停止待機
- `apt update`・`apt upgrade -y` の実行時間
- サーバ再起動およびSSH再接続待機時間
- Schubert起動シェル実行時間

---

## 変更後のサーバ3処理フロー

```
[サーバ3 (servers[3]) ← Schubertシステム稼働サーバ]
  1. ConnectionInformation.getCiFromFile(servers[3]) → ci3
  2. log に ci3 の接続情報を区切りログとして追記
  3. SshCommand.stopSchubert(ci3)
       └─ sh /home/mini/schubert/stop_schubert.sh
  4. SshCommand.waitOneMin(ci3)
       └─ sleep 60
  5. SshCommand.update(ci3)
       └─ sudo apt update
  6. SshCommand.upgrade(ci3)
       └─ sudo apt upgrade -y
  7. SshCommand.shutdown(ci3)
       └─ sudo shutdown -r now
  8. SshCommand.startSchubert(ci3)
       ├─ waitForBecomeActive(ci3) によりSSH接続可能になるまで待機
       └─ sh /home/mini/schubert/start_schubert.sh
```

---

## 影響範囲

| 対象                             | 影響                                    |
|--------------------------------|---------------------------------------|
| `Main.main`                    | 必須コマンドライン引数数を3から4へ変更                  |
| `WksWorkFlow.execScheduledJob` | サーバ3のSchubert停止・OS更新・再起動・起動シーケンスを追加   |
| `SshCommand`                   | `startSchubert`・`stopSchubert` を追加    |
| `WksConstants`                 | Schubert関連パス・コマンド定数を追加し、引数エラーメッセージを変更 |
| `ConnectionInformation`        | 変更なし。既存の接続情報ファイル形式をサーバ3にも使用           |
| サーバ0・サーバ1・サーバ2の処理              | 変更管理No.303の変数名変更を除き、処理内容に変更なし         |
| 自サーバの処理                        | 処理内容に変更なし。サーバ3処理完了後に実行される             |
| 実行インタフェース                      | 接続情報ファイルパスを4つ指定する必要がある                |
| 既存ドキュメント                       | 要件定義・基本設計・詳細設計および補足資料へ反映済み            |

---

## ドキュメント更新結果

| ドキュメント群                                                                                      | 更新結果                                       |
|----------------------------------------------------------------------------------------------|--------------------------------------------|
| `0_rd/000_SYSTEM_OVERVIEW.md`、`1_ui/100_SYSTEM_OVERVIEW.md`、`2_ss/200_SYSTEM_OVERVIEW.md`    | 管理対象、実行コマンド例、処理順序、Schubertサーバの役割を追加済み      |
| `0_rd/001_MAIN.md`、`1_ui/101_MAIN.md`、`2_ss/201_Main.md`                                     | 必須引数数を3から4へ更新済み                            |
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md`            | サーバ3処理および全体の実行順序を追加済み                      |
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`                 | `startSchubert`・`stopSchubert` の要求・設計を追加済み |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md`           | Schubert関連定数および引数エラーメッセージ変更を反映済み           |
| `0_rd/090_SERVER_FILE_SPEC.md`、`1_ui/190_SERVER_FILE_SPEC.md`、`2_ss/290_SERVER_FILE_SPEC.md` | 接続情報ファイルを4つ指定する実行例とサーバ3の対応を追加済み            |
| 各階層の例外・エラーハンドリング資料                                                                           | 引数数の変更およびサーバ3の処理追加を反映済み                    |

---

## 備考

- サーバ3の接続情報ファイルは、既存サーバと同じ4行形式（host・port・user・key）を使用する。
- Schubertの停止・起動には `systemctl` ではなく、`/home/mini/schubert/` 配下のシェルスクリプトを使用する。
- Schubert停止後の待機は、PaperMCと同じ `SshCommand.waitOneMin` による固定60秒待機である。プロセス状態の動的確認は行わない。
- サーバ再起動後のSchubert起動は、`runCommand` 内の `waitForBecomeActive` によりSSH再接続可能になってから実行される。
- 引数が4つでない場合はワークフローを実行せず、`OTHER_ARGS_MSG` を標準出力へ表示して終了する。

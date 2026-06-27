# 変更管理資料 No.306：管理対象VPNサーバ追加

## 基本情報

| 項目       | 内容                                                                              |
|----------|---------------------------------------------------------------------------------|
| 変更管理No   | 306                                                                             |
| 変更概要     | 管理対象サーバを1台追加し、VPNサーバのOS更新・再起動処理を追加                                              |
| ステータス    | 適用済み（ソース確認・設計書反映済み）                                                             |
| 変更対象ファイル | `Main.java`、`WksWorkFlow.java`、`WksConstants.java`                              |
| 完全修飾クラス名 | `com.wks.main.Main`、`com.wks.workflow.WksWorkFlow`、`com.wks.parts.WksConstants` |

---

## 変更背景・目的

### 変更前の課題

変更前のシステムは、4台のリモートサーバと自サーバを管理対象としていた。
リモートサーバの接続情報はコマンドライン引数として4ファイルを受け取り、以下の順序で保守処理を実行していた。

```
サーバ0 → サーバ1（PaperMC）→ サーバ2 → サーバ3（Schubert）→ 自サーバ
```

新たに管理対象となったVPNサーバはこのワークフローに含まれておらず、OSパッケージ更新および再起動を別途実施する必要があった。

### 変更目的

VPNサーバをサーバ4として既存ワークフローへ追加し、以下の処理を一括で自動実行できるようにする。

- 処理開始時点のSSH疎通確認
- OSパッケージリスト更新
- OSパッケージアップグレード
- サーバ再起動
- 非アクティブ時の処理スキップ

---

## 変更内容

### 変更ファイル：`Main.java`

#### 変更箇所：`main` メソッドの引数検証

**変更前**

```java
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 4) {
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
        if (args.length == 5) {
            WksWorkFlow.execScheduledJob(args);
        } else {
            System.out.println(WksConstants.OTHER_ARGS_MSG);
        }
    }
}
```

- 必須引数数を4から5へ変更した。
- Javadocおよび処理コメントの引数数も5へ更新した。

---

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：`execScheduledJob` メソッド内、サーバ4処理シーケンス

サーバ3の処理完了後、自サーバの処理開始前に以下の処理を追加した。

```java
public class WksWorkFlow {
    public static void execScheduledJob(String[] servers)
            throws IOException, InterruptedException, JSchException {
        // サーバ4
        // サーバ4の接続情報を設定
        ConnectionInformation ci4 = ConnectionInformation.getCiFromFile(servers[4]);
        // サーバ4の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci4));
        // ci4がアクティブであれば実行
        if (SshCommand.isAlive(ci4)) {
            // updateコマンド実行
            SshCommand.update(ci4);
            // upgradeコマンド実行
            SshCommand.upgrade(ci4);
            // shutdownコマンド実行
            SshCommand.shutdown(ci4);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }
    }
}
```

サーバ4はVPNサーバとして扱い、追加のアプリケーション停止・起動処理は行わない。
既存の汎用Linuxサーバと同様に、`update`・`upgrade`・`shutdown` のみを実行する。

変更管理No.305で追加された非アクティブサーバスキップ仕様に従い、処理開始時点で `SshCommand.isAlive(ci4)` による疎通確認を行う。
疎通不可の場合はサーバ4への処理を実行せず、`OTHER_NOT_ALIVE_MSG` をログへ追記して自サーバ処理へ進む。

---

### 変更ファイル：`WksConstants.java`

#### 変更したメッセージ定数

| 定数名              | 変更前                                      | 変更後                                      |
|------------------|------------------------------------------|------------------------------------------|
| `OTHER_ARGS_MSG` | `The length of the arguments must be 4.` | `The length of the arguments must be 5.` |

コマンドライン引数数が5つでない場合に表示するUSAGEメッセージを、今回のサーバ追加後の必須引数数に合わせて更新した。

---

## 変更による動作の差異

### 管理対象比較

| 観点       | 変更前                      | 変更後                           |
|----------|--------------------------|-------------------------------|
| 必須引数数    | 4                        | 5                             |
| リモートサーバ数 | 4台                       | 5台                            |
| 処理対象     | サーバ0・サーバ1・サーバ2・サーバ3・自サーバ | サーバ0・サーバ1・サーバ2・サーバ3・サーバ4・自サーバ |
| サーバ4処理   | 対象外                      | VPNサーバとしてOS更新・アップグレード・再起動を実行  |

### 実行順序比較

**変更前**

```
サーバ0 → サーバ1（PaperMC）→ サーバ2 → サーバ3（Schubert）→ 自サーバ
```

**変更後**

```
サーバ0 → サーバ1（PaperMC）→ サーバ2 → サーバ3（Schubert）→ サーバ4（VPN）→ 自サーバ
```

### 実行インタフェース

変更後は接続情報ファイルパスを5つ指定する必要がある。

```bash
java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file> <server3_file> <server4_file>
```

---

## 変更後のサーバ4処理フロー

```
[サーバ4 (servers[4]) ← VPNサーバ]
  1. ConnectionInformation.getCiFromFile(servers[4]) → ci4
  2. log に ci4 の接続情報を区切りログとして追記
  3. SshCommand.isAlive(ci4)
       ├─ false: WARNING: Server Not Active をログへ追記し、自サーバ処理へ進む
       └─ true:
          4. SshCommand.update(ci4)
               └─ sudo apt update
          5. SshCommand.upgrade(ci4)
               └─ sudo apt upgrade -y
          6. SshCommand.shutdown(ci4)
               └─ sudo shutdown -r now
```

---

## 影響範囲

| 対象                             | 影響                                                   |
|--------------------------------|------------------------------------------------------|
| `Main.main`                    | 必須コマンドライン引数数を4から5へ変更                                 |
| `WksWorkFlow.execScheduledJob` | サーバ4の疎通確認・OS更新・再起動シーケンスを追加                           |
| `WksConstants`                 | 引数エラーメッセージを5引数用に変更                                   |
| `SshCommand`                   | 変更なし。既存の `isAlive`・`update`・`upgrade`・`shutdown` を利用 |
| `ConnectionInformation`        | 変更なし。既存の接続情報ファイル形式をサーバ4にも使用                          |
| サーバ0～3の処理                      | 変更なし                                                 |
| 自サーバの処理                        | 処理内容に変更なし。サーバ4処理完了後に実行される                            |
| 実行インタフェース                      | 接続情報ファイルパスを5つ指定する必要がある                               |
| 既存ドキュメント                       | 要件定義・基本設計・詳細設計および補足資料へ反映済み                           |

---

## ドキュメント更新結果

| ドキュメント群                                                                                      | 更新結果                             |
|----------------------------------------------------------------------------------------------|----------------------------------|
| `0_rd/000_SYSTEM_OVERVIEW.md`、`1_ui/100_SYSTEM_OVERVIEW.md`、`2_ss/200_SYSTEM_OVERVIEW.md`    | 管理対象、実行コマンド例、処理順序、VPNサーバの役割を追加済み |
| `0_rd/001_MAIN.md`、`1_ui/101_MAIN.md`、`2_ss/201_Main.md`                                     | 必須引数数を4から5へ更新済み                  |
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md`            | サーバ4処理および全体の実行順序を追加済み            |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md`           | 引数エラーメッセージ変更を反映済み                |
| `0_rd/090_SERVER_FILE_SPEC.md`、`1_ui/190_SERVER_FILE_SPEC.md`、`2_ss/290_SERVER_FILE_SPEC.md` | 接続情報ファイルを5つ指定する実行例とサーバ4の対応を追加済み  |
| 各階層の例外・エラーハンドリング資料                                                                           | 必須引数数の変更を反映済み                    |

---

## 備考

- サーバ4の接続情報ファイルは、既存サーバと同じ4行形式（host・port・user・key）を使用する。
- サーバ4はVPNサーバであり、OS更新・アップグレード・再起動のみを行う。
- VPNサービス固有の停止・起動コマンドは追加しない。
- サーバ4が処理開始時点で非アクティブの場合は異常終了にはせず、警告ログを残して自サーバ処理へ進む。
- コマンドライン引数数、処理順序、接続情報ファイル対応は、サーバ番号と `servers` 配列インデックスを一致させる。

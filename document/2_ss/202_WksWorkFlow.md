# クラス詳細設計：WksWorkFlow

## 基本情報

| 項目       | 内容                                                                                 |
|----------|------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                                     |
| ファイル名    | `WksWorkFlow.java`                                                                 |
| 種別       | class（public）                                                                      |
| 責務       | 4台のリモートサーバおよび自サーバに対する処理シーケンスを定義する。各サーバへの操作順序（update → upgrade → shutdown 等）を一元管理する |
| 依存クラス    | `Main`, `SshCommand`, `BashExec`, `ConnectionInformation`, `WksConstants`          |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                    | 初期値        | 説明                        |
|--------|---------------------|------------------------|------------|---------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` | `Main.log` への参照。ログ追記に使用する |

### フィールド詳細

#### `log`

```java
private static final ArrayList<String> log = Main.log;
```

- `Main.log` の参照を保持する。新しいリストを生成するわけではなく、同一オブジェクトを指している。
- このクラスではサーバ区切りログ（`WksConstants.LOG_SPLIT`）の追記にのみ使用する。

---

## メソッド一覧

| メソッド名              | 戻り値型   | 修飾子             | 説明                                 |
|--------------------|--------|-----------------|------------------------------------|
| `execScheduledJob` | `void` | `public static` | 5つの対象（リモート4台＋自サーバ）に対して定型処理を順番に実行する |

---

## メソッド詳細

### `execScheduledJob(String[] servers)`

```java
public static void execScheduledJob(String[] servers)
        throws IOException, InterruptedException, JSchException;
```

#### 処理フロー

本メソッドは以下の順序で処理を実行する。サーバ間の処理は**直列**であり、前サーバの処理完了後に次サーバの処理が開始される。

```
[サーバ0 (servers[0])]
  1. ConnectionInformation.getCiFromFile(servers[0]) → ci0
  2. log に ci0 情報を追記
  3. SshCommand.isAlive(ci0)
     ├─ false: WARNING: Server Not Active をログへ追記し、サーバ1へ進む
     └─ true:
        4. SshCommand.update(ci0)
        5. SshCommand.upgrade(ci0)
        6. SshCommand.shutdown(ci0)

[サーバ1 (servers[1]) ← PaperMCサーバ]
  7.  ConnectionInformation.getCiFromFile(servers[1]) → ci1
  8.  log に ci1 情報を追記
  9.  SshCommand.isAlive(ci1)
      ├─ false: WARNING: Server Not Active をログへ追記し、サーバ2へ進む
      └─ true:
         10. SshCommand.stopPaperMC(ci1)
         11. SshCommand.waitOneMin(ci1)
         12. SshCommand.update(ci1)
         13. SshCommand.upgrade(ci1)
         14. SshCommand.backupPaperMC(ci1)
         15. SshCommand.wgetPaperMc(ci1)
         16. SshCommand.movePaperMc(ci1)
         17. SshCommand.shutdown(ci1)
         18. SshCommand.startPaperMC(ci1)

[サーバ2 (servers[2])]
  19. ConnectionInformation.getCiFromFile(servers[2]) → ci2
  20. log に ci2 情報を追記
  21. SshCommand.isAlive(ci2)
      ├─ false: WARNING: Server Not Active をログへ追記し、サーバ3へ進む
      └─ true:
         22. SshCommand.update(ci2)
         23. SshCommand.upgrade(ci2)
         24. SshCommand.shutdown(ci2)

[サーバ3 (servers[3]) ← Schubertサーバ]
  25. ConnectionInformation.getCiFromFile(servers[3]) → ci3
  26. log に ci3 情報を追記
  27. SshCommand.isAlive(ci3)
      ├─ false: WARNING: Server Not Active をログへ追記し、自サーバへ進む
      └─ true:
         28. SshCommand.stopSchubert(ci3)
         29. SshCommand.waitOneMin(ci3)
         30. SshCommand.update(ci3)
         31. SshCommand.upgrade(ci3)
         32. SshCommand.shutdown(ci3)
         33. SshCommand.startSchubert(ci3)

[自サーバ (ローカル)]
  34. log に `LOG_SPLIT` フォーマットに `LOG_THIS_SERVER` を埋め込んだ区切りログを追記
  35. BashExec.update()
  36. BashExec.upgrade()
  37. BashExec.shutdown()
```

#### 各サーバの役割と処理内容

| 対象   | 接続方式 | 主な処理                                                                              |
|------|------|-----------------------------------------------------------------------------------|
| サーバ0 | SSH  | OSアップデート・アップグレード・再起動                                                              |
| サーバ1 | SSH  | PaperMC停止 → 60秒待機 → OSアップデート・アップグレード → バックアップ → PaperMC最新版取得・配置 → 再起動 → PaperMC起動 |
| サーバ2 | SSH  | OSアップデート・アップグレード・再起動                                                              |
| サーバ3 | SSH  | Schubert停止 → 60秒待機 → OSアップデート・アップグレード → 再起動 → Schubert起動                          |
| 自サーバ | Bash | OSアップデート・アップグレード・再起動（60秒後バックグラウンド）                                                |

#### 引数

| 引数名       | 型          | 説明                                                         |
|-----------|------------|------------------------------------------------------------|
| `servers` | `String[]` | 長さ4のサーバ接続情報ファイルパス配列。`servers[0]`〜`servers[3]` がそれぞれのサーバに対応 |

#### 例外

| 例外クラス                  | 発生条件                                   |
|------------------------|----------------------------------------|
| `IOException`          | ファイル読み込み失敗、SSH標準出力取得失敗、またはBashコマンド実行失敗 |
| `InterruptedException` | SSH・Bashコマンド実行中の割り込み                   |
| `JSchException`        | SSH接続・実行失敗                             |

---

## 設計上の注意点

- サーバ1はPaperMCサーバであり、PaperMC固有の処理（停止・バックアップ・更新・起動）が含まれる。
- サーバ3はSchubertサーバであり、停止・起動シェルを使用する。
- リモートサーバが処理開始時点で非アクティブの場合、そのサーバの保守処理は実行せず、警告ログを出して後続サーバへ進む。
- `SshCommand.waitOneMin(ci1)` および `SshCommand.waitOneMin(ci3)` は `CMD_WAIT_ONE_MIN`（`sleep 60`）をSSH経由で実行し、
  `SshExec.execute()` の完了までブロッキングする。
- 待機中にPaperMCのプロセス状態は監視せず、60秒経過後は停止状態にかかわらず後続処理へ進む。
- `SshCommand.shutdown()` 実行後もSSH接続が試みられる（`startPaperMC`・`startSchubert`）。これは再起動完了を待つ
  `waitForBecomeActive` が
  `SshCommand` 内で対応しているため成立する。
- 自サーバの `BashExec.shutdown()` はバックグラウンドで60秒後に実行されるため（`CMD_SLEEP_SHUTDOWN`
  ）、プロセス自体はすぐに終了し、その後にシャットダウンが発動する。

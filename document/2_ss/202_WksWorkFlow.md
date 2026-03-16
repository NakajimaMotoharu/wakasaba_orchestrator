# クラス詳細設計：WksWorkFlow

## 基本情報

| 項目       | 内容                                                                                 |
|----------|------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                                     |
| ファイル名    | `WksWorkFlow.java`                                                                 |
| 種別       | class（public）                                                                      |
| 責務       | 3台のリモートサーバおよび自サーバに対する処理シーケンスを定義する。各サーバへの操作順序（update → upgrade → shutdown 等）を一元管理する |
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

| メソッド名              | 戻り値型   | 修飾子             | 説明                                  |
|--------------------|--------|-----------------|-------------------------------------|
| `execScheduledJob` | `void` | `public static` | 4つのサーバ（リモート3台＋自サーバ）に対して定型処理を順番に実行する |

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
  1. ConnectionInformation.getCiFromFile(servers[0]) → ci1
  2. log に ci1 情報を追記
  3. SshCommand.update(ci1)
  4. SshCommand.upgrade(ci1)
  5. SshCommand.shutdown(ci1)

[サーバ1 (servers[1]) ← PaperMCサーバ]
  6.  ConnectionInformation.getCiFromFile(servers[1]) → ci2
  7.  log に ci2 情報を追記
  8.  SshCommand.stopPaperMC(ci2)
  9.  SshCommand.update(ci2)
  10. SshCommand.upgrade(ci2)
  11. SshCommand.backupPaperMC(ci2)
  12. SshCommand.wgetPaperMc(ci2)
  13. SshCommand.movePaperMc(ci2)
  14. SshCommand.shutdown(ci2)
  15. SshCommand.startPaperMC(ci2)

[サーバ2 (servers[2])]
  16. ConnectionInformation.getCiFromFile(servers[2]) → ci3
  17. log に ci3 情報を追記
  18. SshCommand.update(ci3)
  19. SshCommand.upgrade(ci3)
  20. SshCommand.shutdown(ci3)

[自サーバ (ローカル)]
  21. log に `LOG_SPLIT` フォーマットに `LOG_THIS_SERVER` を埋め込んだ区切りログを追記
  22. BashExec.update()
  23. BashExec.upgrade()
  24. BashExec.shutdown()
```

#### 各サーバの役割と処理内容

| 対象   | 接続方式 | 主な処理                                                                      |
|------|------|---------------------------------------------------------------------------|
| サーバ0 | SSH  | OSアップデート・アップグレード・再起動                                                      |
| サーバ1 | SSH  | PaperMC停止 → OSアップデート・アップグレード → バックアップ → PaperMC最新版取得・配置 → 再起動 → PaperMC起動 |
| サーバ2 | SSH  | OSアップデート・アップグレード・再起動                                                      |
| 自サーバ | Bash | OSアップデート・アップグレード・再起動（60秒後バックグラウンド）                                        |

#### 引数

| 引数名       | 型          | 説明                                                         |
|-----------|------------|------------------------------------------------------------|
| `servers` | `String[]` | 長さ3のサーバ接続情報ファイルパス配列。`servers[0]`〜`servers[2]` がそれぞれのサーバに対応 |

#### 例外

| 例外クラス                  | 発生条件                                   |
|------------------------|----------------------------------------|
| `IOException`          | ファイル読み込み失敗、SSH標準出力取得失敗、またはBashコマンド実行失敗 |
| `InterruptedException` | SSH・Bashコマンド実行中の割り込み                   |
| `JSchException`        | SSH接続・実行失敗                             |

---

## 設計上の注意点

- サーバ1はPaperMCサーバであり、他の2台とは異なりPaperMC固有の処理（停止・バックアップ・更新・起動）が含まれる。
- `SshCommand.shutdown()` 実行後もSSH接続が試みられる（`startPaperMC` 等）。これは再起動完了を待つ `waitForBecomeActive` が
  `SshCommand` 内で対応しているため成立する。
- 自サーバの `BashExec.shutdown()` はバックグラウンドで60秒後に実行されるため（`CMD_SLEEP_SHUTDOWN`
  ）、プロセス自体はすぐに終了し、その後にシャットダウンが発動する。

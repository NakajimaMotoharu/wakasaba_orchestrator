# wakasaba_orchestrator 基本設計書：WksWorkFlow

## 基本情報

| 項目       | 内容                                                                    |
|----------|-----------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                        |
| ファイル名    | `WksWorkFlow.java`                                                    |
| 種別       | class（public）                                                         |
| 責務       | 3台のリモートサーバおよび自サーバに対する処理シーケンスを定義・実行する。サーバ間の処理順序を一元管理する                 |
| 主な依存クラス  | `Main`、`SshCommand`、`BashExec`、`ConnectionInformation`、`WksConstants` |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                    | 説明                                   |
|--------|---------------------|------------------------|--------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` への参照。サーバ処理開始の区切りログ追記に使用する |

---

## メソッド一覧

| メソッド名              | 戻り値型   | 修飾子             | 説明                                   |
|--------------------|--------|-----------------|--------------------------------------|
| `execScheduledJob` | `void` | `public static` | 4つの対象（リモート3台＋自サーバ）に対して定型処理を直列に順次実行する |

---

## 処理フロー

### `execScheduledJob(String[] servers)`

各サーバの処理は**直列**であり、前サーバの全処理が完了してから次サーバへ進む。

```
[サーバ0 (servers[0])]
  1. ConnectionInformation.getCiFromFile(servers[0]) → ci1
  2. log に ci1 の接続情報を区切りログとして追記
  3. SshCommand.update(ci1)
  4. SshCommand.upgrade(ci1)
  5. SshCommand.shutdown(ci1)

[サーバ1 (servers[1]) ← PaperMC サーバ]
  6.  ConnectionInformation.getCiFromFile(servers[1]) → ci2
  7.  log に ci2 の接続情報を区切りログとして追記
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
  17. log に ci3 の接続情報を区切りログとして追記
  18. SshCommand.update(ci3)
  19. SshCommand.upgrade(ci3)
  20. SshCommand.shutdown(ci3)

[自サーバ（ローカル）]
  21. log に "this server" を区切りログとして追記
  22. BashExec.update()
  23. BashExec.upgrade()
  24. BashExec.shutdown()
```

### 各対象の役割と処理内容

| 対象   | 接続方式 | 主な処理内容                                                            |
|------|------|-------------------------------------------------------------------|
| サーバ0 | SSH  | OS アップデート・アップグレード・即時再起動                                           |
| サーバ1 | SSH  | PaperMC 停止 → OS 更新 → バックアップ → PaperMC/Pl3xMap 最新版取得・配置 → 再起動 → 起動 |
| サーバ2 | SSH  | OS アップデート・アップグレード・即時再起動                                           |
| 自サーバ | Bash | OS アップデート・アップグレード・60秒遅延バックグラウンド再起動                                |

---

## 例外

| 例外クラス                  | 発生条件                                |
|------------------------|-------------------------------------|
| `IOException`          | ファイル読込失敗・SSH 標準出力取得失敗・Bash コマンド実行失敗 |
| `InterruptedException` | SSH / Bash コマンド実行中の割り込み             |
| `JSchException`        | SSH 接続・実行失敗                         |

---

## 設計上の注意点

- サーバ1はPaperMCサーバであり、他の2台とは異なりPaperMC固有の処理（停止・バックアップ・更新・起動）が含まれる。
- `SshCommand.shutdown()` による再起動後も後続処理（`startPaperMC` 等）が続くが、`SshCommand` 内の `waitForBecomeActive`
  がポーリングで再起動完了を待機するため正常に動作する。
- 自サーバの `BashExec.shutdown()` は60秒遅延バックグラウンド実行のため、`shutdown()` 呼出し直後にプロセスが終了し、その60秒後にシャットダウンが発動する。

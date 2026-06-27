# wakasaba_orchestrator 基本設計書：WksWorkFlow

## 基本情報

| 項目       | 内容                                                                    |
|----------|-----------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                        |
| ファイル名    | `WksWorkFlow.java`                                                    |
| 種別       | class（public）                                                         |
| 責務       | 5台のリモートサーバおよび自サーバに対する処理シーケンスを定義・実行する。サーバ間の処理順序を一元管理する                 |
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
| `execScheduledJob` | `void` | `public static` | 6つの対象（リモート5台＋自サーバ）に対して定型処理を直列に順次実行する |

---

## 処理フロー

### `execScheduledJob(String[] servers)`

各サーバの処理は**直列**であり、前サーバの全処理が完了してから次サーバへ進む。

```
[サーバ0 (servers[0])]
  1. ConnectionInformation.getCiFromFile(servers[0]) → ci0
  2. log に ci0 の接続情報を区切りログとして追記
  3. SshCommand.isAlive(ci0)
     ├─ false: WARNING: Server Not Active をログへ追記し、サーバ1へ進む
     └─ true:
        4. SshCommand.update(ci0)
        5. SshCommand.upgrade(ci0)
        6. SshCommand.shutdown(ci0)

[サーバ1 (servers[1]) ← PaperMC サーバ]
  7.  ConnectionInformation.getCiFromFile(servers[1]) → ci1
  8.  log に ci1 の接続情報を区切りログとして追記
  9.  SshCommand.isAlive(ci1)
      ├─ false: WARNING: Server Not Active をログへ追記し、サーバ2へ進む
      └─ true:
         10. SshCommand.stopPaperMC(ci1)
         11. SshCommand.waitOneMin(ci1)  ← 安全停止のため固定60秒待機
         12. SshCommand.update(ci1)
         13. SshCommand.upgrade(ci1)
         14. SshCommand.backupPaperMC(ci1)
         15. SshCommand.wgetPaperMc(ci1)
         16. SshCommand.movePaperMc(ci1)
         17. SshCommand.shutdown(ci1)
         18. SshCommand.startPaperMC(ci1)

[サーバ2 (servers[2])]
  19. ConnectionInformation.getCiFromFile(servers[2]) → ci2
  20. log に ci2 の接続情報を区切りログとして追記
  21. SshCommand.isAlive(ci2)
      ├─ false: WARNING: Server Not Active をログへ追記し、サーバ3へ進む
      └─ true:
         22. SshCommand.update(ci2)
         23. SshCommand.upgrade(ci2)
         24. SshCommand.shutdown(ci2)

[サーバ3 (servers[3]) ← Schubert サーバ]
  25. ConnectionInformation.getCiFromFile(servers[3]) → ci3
  26. log に ci3 の接続情報を区切りログとして追記
  27. SshCommand.isAlive(ci3)
      ├─ false: WARNING: Server Not Active をログへ追記し、サーバ4へ進む
      └─ true:
         28. SshCommand.stopSchubert(ci3)
         29. SshCommand.waitOneMin(ci3)  ← 安全停止のため固定60秒待機
         30. SshCommand.update(ci3)
         31. SshCommand.upgrade(ci3)
         32. SshCommand.shutdown(ci3)
         33. SshCommand.startSchubert(ci3)

[サーバ4 (servers[4]) ← VPN サーバ]
  34. ConnectionInformation.getCiFromFile(servers[4]) → ci4
  35. log に ci4 の接続情報を区切りログとして追記
  36. SshCommand.isAlive(ci4)
      ├─ false: WARNING: Server Not Active をログへ追記し、自サーバへ進む
      └─ true:
         37. SshCommand.update(ci4)
         38. SshCommand.upgrade(ci4)
         39. SshCommand.shutdown(ci4)

[自サーバ（ローカル）]
  40. log に `LOG_SPLIT` フォーマットに `LOG_THIS_SERVER` を埋め込んだ区切りログを追記
  41. BashExec.update()
  42. BashExec.upgrade()
  43. BashExec.shutdown()
```

### 各対象の役割と処理内容

| 対象   | 接続方式 | 主な処理内容                                                                    |
|------|------|---------------------------------------------------------------------------|
| サーバ0 | SSH  | OS アップデート・アップグレード・即時再起動                                                   |
| サーバ1 | SSH  | PaperMC 停止 → 60秒待機 → OS 更新 → バックアップ → PaperMC/Pl3xMap 最新版取得・配置 → 再起動 → 起動 |
| サーバ2 | SSH  | OS アップデート・アップグレード・即時再起動                                                   |
| サーバ3 | SSH  | Schubert停止 → 60秒待機 → OS更新 → 再起動 → Schubert起動                              |
| サーバ4 | SSH  | OS アップデート・アップグレード・即時再起動                                                   |
| 自サーバ | Bash | OS アップデート・アップグレード・60秒遅延バックグラウンド再起動                                        |

---

## 例外

| 例外クラス                  | 発生条件                                |
|------------------------|-------------------------------------|
| `IOException`          | ファイル読込失敗・SSH 標準出力取得失敗・Bash コマンド実行失敗 |
| `InterruptedException` | SSH / Bash コマンド実行中の割り込み             |
| `JSchException`        | SSH 接続・実行失敗                         |

---

## 設計上の注意点

- サーバ1はPaperMCサーバであり、PaperMC固有の処理（停止・バックアップ・更新・起動）が含まれる。
- サーバ3はSchubertサーバであり、停止・起動シェルを使用する。
- サーバ4はVPNサーバであり、OS更新・アップグレード・再起動のみを行う。
- リモートサーバが処理開始時点で非アクティブの場合、そのサーバの保守処理は実行せず、警告ログを出して後続サーバへ進む。
- Schubert停止後の `waitOneMin` も固定60秒のブロッキング待機であり、停止状態の動的ポーリングは行わない。
- PaperMC停止後の `waitOneMin` は `sleep 60` をSSH経由で実行する固定時間のブロッキング待機であり、停止状態の動的ポーリングは行わない。
- 60秒経過後はPaperMCが停止していない場合でも異常停止状態として後続処理を継続する。
- `SshCommand.shutdown()` による再起動後も後続処理（`startPaperMC`・`startSchubert`）が続くが、`SshCommand` 内の
  `waitForBecomeActive`
  がポーリングで再起動完了を待機するため正常に動作する。
- 自サーバの `BashExec.shutdown()` は60秒遅延バックグラウンド実行のため、`shutdown()` 呼出し直後にプロセスが終了し、その60秒後にシャットダウンが発動する。

# 変更管理資料 No.302：PaperMC 停止後の安全停止待機処理の追加

## 基本情報

| 項目       | 内容                                     |
|----------|----------------------------------------|
| 変更管理No   | 302                                    |
| 変更概要     | PaperMC を停止した後、安全停止を確認するための定数時間待機処理を追加 |
| ステータス    | 適用済み（安定稼働確認済み）                         |
| 変更対象ファイル | `WksWorkFlow.java`                     |
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`         |

---

## 変更背景・目的

### 変更前の課題

変更前の実装では、サーバ1（PaperMC サーバ）のワークフローにおいて、
`SshCommand.stopPaperMC(ci2)` の直後に `SshCommand.update(ci2)` を実行していた。

`systemctl stop papermc` はデーモン停止命令の発行のみを行うため、
実際のプロセス終了・ファイルクローズ・セーブデータのフラッシュが完了するまでに
一定の時間を要する。停止完了前に後続処理が進んだ場合、
以下のリスクが存在していた。

- OS アップグレード・再起動処理がサーバプロセスの停止完了前に発動し、セーブデータ破損の恐れがある
- バックアップシェルが稼働中プロセスのファイルを対象とする可能性がある

### 変更目的

PaperMC のグレースフルシャットダウンが完了するための十分な猶予時間を設け、
後続処理（OS アップデート・バックアップ等）が安全な状態で実行されることを保証する。

### 待機時間の根拠

実稼働環境における PaperMC の停止所要時間を確認した結果、
通常のシャットダウンは **1 分以内に完了**することが確認されている。

そのため定数時間（1 分）の待機を採用し、以下の方針とした。

- **1 分以内に停止する場合**：安全に停止が完了した状態で後続処理へ進む
- **1 分を超えても停止しない場合**：異常停止（通常停止不可状態）と見なし、そのまま次のワークフローに進む

この方針に基づき、既存の `SshCommand.waitOneMin` を使用した定数時間待機を採用した。
動的なプロセス監視によるポーリングは行わない。

---

## 変更内容

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：`execScheduledJob` メソッド内、サーバ1処理シーケンス

**変更前**

```java
public class WksWorkFlow {
	public static void execScheduledJob(String[] servers) throws IOException, InterruptedException, JSchException {
		// PaperMC停止コマンドの実行
		SshCommand.stopPaperMC(ci2);
		// updateコマンドの実行
		SshCommand.update(ci2);
	}
}
```

**変更後**

```java
public class WksWorkFlow {
	public static void execScheduledJob(String[] servers) throws IOException, InterruptedException, JSchException {
		// PaperMC停止コマンドの実行
		SshCommand.stopPaperMC(ci2);
		// 安全停止のため1分間待機
		SshCommand.waitOneMin(ci2);
		// updateコマンドの実行
		SshCommand.update(ci2);
	}
}
```

---

## 変更による動作の差異

### サーバ1ワークフローの処理シーケンス比較

| ステップ | 変更前                  | 変更後                        |
|------|----------------------|----------------------------|
| 8    | `stopPaperMC(ci2)`   | `stopPaperMC(ci2)`         |
| 9    | `update(ci2)` ← 直接移行 | `waitOneMin(ci2)` ← **追加** |
| 10   | `upgrade(ci2)`       | `update(ci2)`              |
| 11   | `backupPaperMC(ci2)` | `upgrade(ci2)`             |
| 12   | `wgetPaperMc(ci2)`   | `backupPaperMC(ci2)`       |
| 13   | `movePaperMc(ci2)`   | `wgetPaperMc(ci2)`         |
| 14   | `shutdown(ci2)`      | `movePaperMc(ci2)`         |
| 15   | `startPaperMC(ci2)`  | `shutdown(ci2)`            |
| 16   | —                    | `startPaperMC(ci2)`        |

※ステップ番号は `execScheduledJob` 全体での通し番号ではなく、サーバ1処理内の相対順序。

### 実行時間への影響

| 観点               | 内容                                                       |
|------------------|----------------------------------------------------------|
| 追加待機時間           | 最大 60 秒（`sleep 60` による固定待機）                              |
| 最悪ケースのバッチ総所要時間   | 変更前 + 60 秒                                               |
| `waitOneMin` の実装 | `SshCommand.waitOneMin` → `CMD_WAIT_ONE_MIN`（`sleep 60`） |

---

## 変更後のサーバ1処理フロー

```
[サーバ1 (servers[1]) ← PaperMC サーバ]
  1.  ConnectionInformation.getCiFromFile(servers[1]) → ci2
  2.  log に ci2 の接続情報を区切りログとして追記
  3.  SshCommand.stopPaperMC(ci2)     ← PaperMC 停止命令
  4.  SshCommand.waitOneMin(ci2)      ← 安全停止待機（最大60秒）★追加
  5.  SshCommand.update(ci2)
  6.  SshCommand.upgrade(ci2)
  7.  SshCommand.backupPaperMC(ci2)
  8.  SshCommand.wgetPaperMc(ci2)
  9.  SshCommand.movePaperMc(ci2)
  10. SshCommand.shutdown(ci2)
  11. SshCommand.startPaperMC(ci2)
```

---

## 影響範囲

| 対象                              | 影響                                            |
|---------------------------------|-----------------------------------------------|
| `WksWorkFlow.execScheduledJob`  | サーバ1処理内に `waitOneMin` 呼び出しを1件追加               |
| `SshCommand.waitOneMin`         | 変更なし（既存メソッドをそのまま使用）                           |
| `WksConstants.CMD_WAIT_ONE_MIN` | 変更なし（`sleep 60` のまま）                          |
| サーバ0・サーバ2・自サーバの処理               | 変更なし                                          |
| 既存ドキュメント（要更新）                   | `002_WKS_WORK_FLOW.md`、`102_WKS_WORK_FLOW.md` |

---

## ドキュメント更新要否

| ドキュメント                      | 更新箇所（概要）                                                               |
|-----------------------------|------------------------------------------------------------------------|
| `0_rd/002_WKS_WORK_FLOW.md` | WF-03 のステップ一覧：ステップ3（`stopPaperMC`）とステップ4（`update`）の間に `waitOneMin` を追加 |
|                             | 制約・注意事項：1分待機の設計根拠（安全停止確認・異常停止時の継続方針）を追記                                |
| `1_ui/102_WKS_WORK_FLOW.md` | 処理フロー図（サーバ1ブロック）：ステップ8（`stopPaperMC`）直後に `waitOneMin` を追加              |
|                             | 各対象の役割と処理内容テーブル（サーバ1行）：「PaperMC 停止 → **1分待機** → OS 更新 → ...」に更新        |

---

## 備考

- `waitOneMin` は内部で `sleep 60` を SSH 経由で実行するため、
  オーケストレーター側のスレッドは `SshExec.execute()` の完了を待つ形となる（ブロッキング動作）。
- 1分待機中に SSH 接続が切れた場合は `waitForBecomeActive` によるポーリングで再接続されるため、後続処理は継続可能である。
- 将来的にプロセス監視による動的待機（`systemctl is-active` ポーリング等）への移行を検討する場合は、別途変更管理を起票すること。

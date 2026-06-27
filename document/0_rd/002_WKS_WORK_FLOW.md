# wakasaba_orchestrator 要件定義書：WksWorkFlow

## 基本情報

| 項目       | 内容                                                                    |
|----------|-----------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                        |
| ファイル名    | `WksWorkFlow.java`                                                    |
| 種別       | class（public）                                                         |
| 責務       | 5台のリモートサーバおよび自サーバに対する処理シーケンスを定義・実行する。サーバ間の処理順序を一元管理する                 |
| 主な依存クラス  | `Main`、`SshCommand`、`BashExec`、`ConnectionInformation`、`WksConstants` |

---

## 要求機能

### WF-01：処理シーケンスの一元管理・直列実行

- 全サーバ（サーバ0・サーバ1・サーバ2・サーバ3・サーバ4・自サーバ）への処理シーケンスを1箇所に集約して定義すること。
- 各サーバへの処理は直列に実行し、前サーバの全処理完了後に次サーバの処理を開始すること。
- 実行順序は サーバ0 → サーバ1 → サーバ2 → サーバ3 → サーバ4 → 自サーバ の順に固定すること。
- リモートサーバ（サーバ0～4）は、処理開始時点で `SshCommand.isAlive(ci)` による疎通確認を行い、疎通不可の場合は警告ログを記録して対象サーバの処理をスキップすること。

### WF-02：サーバ0 のOSメンテナンス（SSH）

コマンドライン引数 `servers[0]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.isAlive(ci)` を実行する
4. 疎通可能な場合のみ `SshCommand.update(ci)` を実行する
5. 疎通可能な場合のみ `SshCommand.upgrade(ci)` を実行する
6. 疎通可能な場合のみ `SshCommand.shutdown(ci)` を実行する
7. 疎通不可の場合は `WksConstants.OTHER_NOT_ALIVE_MSG` をログへ追記し、サーバ1の処理へ進む

### WF-03：サーバ1 のPaperMCサーバメンテナンス（SSH）

コマンドライン引数 `servers[1]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.isAlive(ci)` を実行する
4. 疎通可能な場合のみ `SshCommand.stopPaperMC(ci)` を実行する
5. 疎通可能な場合のみPaperMCの安全停止を待つため `SshCommand.waitOneMin(ci)` を実行する
6. 疎通可能な場合のみ `SshCommand.update(ci)` を実行する
7. 疎通可能な場合のみ `SshCommand.upgrade(ci)` を実行する
8. 疎通可能な場合のみ `SshCommand.backupPaperMC(ci)` を実行する
9. 疎通可能な場合のみ `SshCommand.wgetPaperMc(ci)` を実行する
10. 疎通可能な場合のみ `SshCommand.movePaperMc(ci)` を実行する
11. 疎通可能な場合のみ `SshCommand.shutdown(ci)` を実行する
    - 再起動後の接続待機は `SshCommand` 内の `waitForBecomeActive` が自動的に処理する（FR-04 ステップ8に相当）。
      `WksWorkFlow` 側での明示的な待機処理は不要
12. 疎通可能な場合のみ `SshCommand.startPaperMC(ci)` を実行する
13. 疎通不可の場合は `WksConstants.OTHER_NOT_ALIVE_MSG` をログへ追記し、サーバ2の処理へ進む

### WF-04：サーバ2 のOSメンテナンス（SSH）

コマンドライン引数 `servers[2]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.isAlive(ci)` を実行する
4. 疎通可能な場合のみ `SshCommand.update(ci)` を実行する
5. 疎通可能な場合のみ `SshCommand.upgrade(ci)` を実行する
6. 疎通可能な場合のみ `SshCommand.shutdown(ci)` を実行する
7. 疎通不可の場合は `WksConstants.OTHER_NOT_ALIVE_MSG` をログへ追記し、サーバ3の処理へ進む

### WF-05：サーバ3 のSchubertサーバメンテナンス（SSH）

コマンドライン引数 `servers[3]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.isAlive(ci)` を実行する
4. 疎通可能な場合のみ `SshCommand.stopSchubert(ci)` を実行する
5. 疎通可能な場合のみSchubertの安全停止を待つため `SshCommand.waitOneMin(ci)` を実行する
6. 疎通可能な場合のみ `SshCommand.update(ci)` を実行する
7. 疎通可能な場合のみ `SshCommand.upgrade(ci)` を実行する
8. 疎通可能な場合のみ `SshCommand.shutdown(ci)` を実行する
9. 疎通可能な場合のみ `SshCommand.startSchubert(ci)` を実行する
10. 疎通不可の場合は `WksConstants.OTHER_NOT_ALIVE_MSG` をログへ追記し、サーバ4の処理へ進む

### WF-06：サーバ4 のVPNサーバメンテナンス（SSH）

コマンドライン引数 `servers[4]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.isAlive(ci)` を実行する
4. 疎通可能な場合のみ `SshCommand.update(ci)` を実行する
5. 疎通可能な場合のみ `SshCommand.upgrade(ci)` を実行する
6. 疎通可能な場合のみ `SshCommand.shutdown(ci)` を実行する
7. 疎通不可の場合は `WksConstants.OTHER_NOT_ALIVE_MSG` をログへ追記し、自サーバ処理へ進む

### WF-07：自サーバのOSメンテナンス（Bash）

1. `WksConstants.LOG_SPLIT` のフォーマットに `WksConstants.LOG_THIS_SERVER` を埋め込んだ区切りログをグローバルログへ追記する
2. `BashExec.update()` を実行する
3. `BashExec.upgrade()` を実行する
4. `BashExec.shutdown()` を実行する

### WF-08：サーバ区切りログの記録

- 各サーバの処理開始前に、`WksConstants.LOG_SPLIT` のフォーマットに従い接続情報文字列（host・port・user）を埋め込んだ区切りログをグローバルログへ追記すること。
- 自サーバについては `WksConstants.LOG_THIS_SERVER` の文字列を区切りログへ追記すること。

---

## 要求インタフェース

### フィールド

| フィールド名 | 型                   | 修飾子                    | 初期値        | 要件                                   |
|--------|---------------------|------------------------|------------|--------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` | `Main.log` への参照。サーバ処理開始の区切りログ追記に使用する |

### メソッド

| メソッド名              | 戻り値型   | 修飾子             | 要件概要                                 |
|--------------------|--------|-----------------|--------------------------------------|
| `execScheduledJob` | `void` | `public static` | 6つの対象に対して定型処理を直列に順次実行する。引数は5つのファイルパス |

---

## 例外要件

| 例外クラス                  | 想定発生状況                              |
|------------------------|-------------------------------------|
| `IOException`          | 接続情報ファイル読込失敗・SSH出力読取失敗・Bashコマンド実行失敗 |
| `InterruptedException` | SSH/Bashコマンド実行中の割り込み                |
| `JSchException`        | SSH接続・実行失敗                          |

- 例外はすべて `throws` 宣言により `Main.main` へ委譲すること。

---

## 制約・注意事項

- サーバ1はPaperMCサーバであり、PaperMC固有の処理が含まれること。
- サーバ3はSchubertサーバであり、Schubert停止・起動シェルの実行を含むこと。
- サーバ4はVPNサーバであり、OS更新・アップグレード・再起動のみを行うこと。
- リモートサーバが処理開始時点で非アクティブの場合、そのサーバの保守処理は実行せず、警告ログを出して後続サーバへ進むこと。
- Schubert停止後は固定60秒待機し、停止状態の動的確認は行わず後続処理へ進むこと。
- PaperMC停止後はグレースフルシャットダウン、ファイルクローズおよびセーブデータのフラッシュ完了を待つため、固定60秒の待機を行うこと。
- 60秒経過時点で停止状態の動的確認は行わず、異常停止状態であっても後続処理を継続すること。
- `SshCommand.shutdown()` による再起動後も後続処理が続く場合は、`SshCommand` 内のポーリング機構が再起動完了を待機するため、
  `WksWorkFlow` 側での待機処理は不要である。
- 自サーバの `BashExec.shutdown()` は60秒遅延バックグラウンド実行であり、呼出し直後にバッチ処理は終了する。

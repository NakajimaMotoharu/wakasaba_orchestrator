# wakasaba_orchestrator 要件定義書：WksWorkFlow

## 基本情報

| 項目       | 内容                                                                    |
|----------|-----------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                        |
| ファイル名    | `WksWorkFlow.java`                                                    |
| 種別       | class（public）                                                         |
| 責務       | 3台のリモートサーバおよび自サーバに対する処理シーケンスを定義・実行する。サーバ間の処理順序を一元管理する                 |
| 主な依存クラス  | `Main`、`SshCommand`、`BashExec`、`ConnectionInformation`、`WksConstants` |

---

## 要求機能

### WF-01：処理シーケンスの一元管理・直列実行

- 全サーバ（サーバ0・サーバ1・サーバ2・自サーバ）への処理シーケンスを1箇所に集約して定義すること。
- 各サーバへの処理は直列に実行し、前サーバの全処理完了後に次サーバの処理を開始すること。
- 実行順序は サーバ0 → サーバ1 → サーバ2 → 自サーバ の順に固定すること。

### WF-02：サーバ0 のOSメンテナンス（SSH）

コマンドライン引数 `servers[0]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.update(ci)` を実行する
4. `SshCommand.upgrade(ci)` を実行する
5. `SshCommand.shutdown(ci)` を実行する

### WF-03：サーバ1 のPaperMCサーバメンテナンス（SSH）

コマンドライン引数 `servers[1]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.stopPaperMC(ci)` を実行する
4. `SshCommand.update(ci)` を実行する
5. `SshCommand.upgrade(ci)` を実行する
6. `SshCommand.backupPaperMC(ci)` を実行する
7. `SshCommand.wgetPaperMc(ci)` を実行する
8. `SshCommand.movePaperMc(ci)` を実行する
9. `SshCommand.shutdown(ci)` を実行する
    - 再起動後の接続待機は `SshCommand` 内の `waitForBecomeActive` が自動的に処理する（FR-04 ステップ8に相当）。
      `WksWorkFlow` 側での明示的な待機処理は不要
10. `SshCommand.startPaperMC(ci)` を実行する

### WF-04：サーバ2 のOSメンテナンス（SSH）

コマンドライン引数 `servers[2]` の接続情報を使用し、以下を順次実行すること。

1. 接続情報ファイルを読み込み `ConnectionInformation` を生成する
2. サーバ処理開始区切りログをグローバルログへ追記する
3. `SshCommand.update(ci)` を実行する
4. `SshCommand.upgrade(ci)` を実行する
5. `SshCommand.shutdown(ci)` を実行する

### WF-05：自サーバのOSメンテナンス（Bash）

1. `WksConstants.LOG_SPLIT` のフォーマットに `WksConstants.LOG_THIS_SERVER` を埋め込んだ区切りログをグローバルログへ追記する
2. `BashExec.update()` を実行する
3. `BashExec.upgrade()` を実行する
4. `BashExec.shutdown()` を実行する

### WF-06：サーバ区切りログの記録

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
| `execScheduledJob` | `void` | `public static` | 4つの対象に対して定型処理を直列に順次実行する。引数は3つのファイルパス |

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

- サーバ1はPaperMCサーバであり、他の2台とは異なりPaperMC固有の処理が含まれること。
- `SshCommand.shutdown()` による再起動後も後続処理が続く場合は、`SshCommand` 内のポーリング機構が再起動完了を待機するため、
  `WksWorkFlow` 側での待機処理は不要である。
- 自サーバの `BashExec.shutdown()` は60秒遅延バックグラウンド実行であり、呼出し直後にバッチ処理は終了する。

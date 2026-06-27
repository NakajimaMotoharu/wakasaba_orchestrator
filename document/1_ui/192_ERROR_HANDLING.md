# wakasaba_orchestrator 基本設計書 補足：例外・エラーハンドリング設計

## 概要

本アプリケーションにおける例外処理の設計方針と、各例外の種類・発生状況・伝播経路をまとめる。

---

## 基本方針

各業務メソッドでは個別の回復処理を行わず、例外を `WksWorkFlow.execScheduledJob` から `Main.main` まで伝播させる。  
`Main.main` はワークフロー実行を `try-catch` で囲み、`Exception`
を捕捉してスタックトレースをログへ保存した後、終了時刻記録とログファイル出力を行う。  
ただし、`Main.outLog` が送出する `IOException` は `main` で捕捉せずJVMへ伝播させる。

---

## 例外の種類と発生状況

### 検査例外（Checked Exception）

| 例外クラス                  | 主な発生箇所                                | 原因                               |
|------------------------|---------------------------------------|----------------------------------|
| `IOException`          | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの読み込み失敗                  |
| `IOException`          | `SshExec.execute`                     | SSH標準出力の読み取り失敗                   |
| `IOException`          | `BashExec.runCommand`                 | ローカルプロセスの起動・出力読み取り失敗             |
| `IOException`          | `Curl.exec`                           | HTTPリクエストのネットワーク障害               |
| `IOException`          | `Main.outLog`                         | ログファイルへの書き込み失敗                   |
| `InterruptedException` | `SshExec.execute`                     | SSH待機中の割り込み                      |
| `InterruptedException` | `SshCommand.waitForBecomeActive`      | ポーリングスリープ中の割り込み                  |
| `InterruptedException` | `BashExec.runCommand`                 | `process.waitFor()` 中の割り込み       |
| `InterruptedException` | `Curl.exec`                           | HTTPリクエスト待機中の割り込み                |
| `JSchException`        | `SshExec.isAlive`                     | SSHセッション生成エラー（接続失敗は吸収して `false`） |
| `JSchException`        | `SshExec.execute`                     | SSH接続・チャンネル接続失敗                  |

### 非検査例外（Unchecked Exception）

| 例外クラス                       | 主な発生箇所                                | 原因                        |
|-----------------------------|---------------------------------------|---------------------------|
| `NumberFormatException`     | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの2行目が整数値でない       |
| `IndexOutOfBoundsException` | `ConnectionInformation.getCiFromFile` | 接続情報ファイルが4行未満             |
| `JacksonException`（非検査）     | `PaperUrlGen` 全メソッド                   | JSONパース失敗（APIレスポンス形式の変更等） |
| `NullPointerException`      | `PaperUrlGen` 全メソッド                   | JSONのキーが存在しない場合           |

---

## 例外伝播経路

```
各ユーティリティクラス
  └─► SshCommand / BashExec
        └─► WksWorkFlow.execScheduledJob
              └─► Main.main
                    ├─ catch (Exception e)
                    ├─ スタックトレースを Main.log へ追記
                    ├─ 終了時刻を Main.log へ追記
                    └─ outLog() でログファイル出力

Main.outLog の IOException
  └─► Main.main の throws IOException
        └─► JVM
```

---

## 特例：`SshExec.isAlive()` の例外吸収

`SshCommand.waitForBecomeActive()` → `SshExec.isAlive()` の流れにおいて、  
`session.connect()` が投げる `JSchException`（接続不可）は `isAlive()` 内で `false` として吸収する。

これにより `shutdown` 後の再起動待ちがポーリングで実現される。  
ただし、秘密鍵ファイル読み込みエラー等のセッション生成に起因する `JSchException` は吸収せず上位に伝播させる。

詳細は [106_SSH_EXEC.md](./106_SSH_EXEC.md) を参照。

---

## ログ未出力のケース

以下の状況ではログファイルが出力されない（または不完全になる）。これらはすべて設計上許容される動作である。

| 状況                     | 結果                                         |
|------------------------|--------------------------------------------|
| `main` に渡す引数が5つでない     | 標準出力にUSAGEメッセージを表示して終了。ログファイルは生成されない       |
| ワークフロー処理途中で例外が発生       | スタックトレースと終了時刻を追記し、途中までの実行ログとともにログファイルへ出力する |
| ログファイルの出力先ディレクトリが存在しない | `IOException` が発生してJVMが終了                  |

---

## 設計上の改善余地（参考）

本設計では以下は対象外とするが、将来の改善候補として記録する。

- 各処理ステップを `try-catch` で囲み、一部サーバの失敗時に残りのサーバ処理を継続する設計にすることで可用性が向上する。
- `PaperUrlGen` の `JacksonException` / `NullPointerException` に対する明示的なハンドリングを追加することで、APIレスポンス変更時のデバッグが容易になる。
- `Curl.exec` におけるHTTPステータスコードの検証を追加することで、APIエラー時の原因特定が容易になる。

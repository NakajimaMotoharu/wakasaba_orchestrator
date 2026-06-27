# 補足資料：例外・エラーハンドリング方針

## 概要

本アプリケーションにおける例外処理の方針と、各例外の伝播経路をまとめる。

---

## 基本方針

各業務メソッドでは個別の回復処理を行わず、例外を `WksWorkFlow.execScheduledJob` から `Main.main` まで伝播させる。  
`Main.main` はワークフロー実行を `try-catch` で囲み、`Exception`
を捕捉してスタックトレースをログへ保存した後、終了時刻記録とログファイル出力を行う。  
ただし、`Main.outLog` が送出する `IOException` は `main` で捕捉されずJVMへ伝播する。

---

## 例外の種類と発生箇所

### 検査例外（Checked Exception）

| 例外クラス                  | 主な発生箇所                                | 原因                                                                           |
|------------------------|---------------------------------------|------------------------------------------------------------------------------|
| `IOException`          | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの読み込み失敗                                                              |
| `IOException`          | `SshExec.execute`                     | SSH標準出力の読み取り失敗                                                               |
| `IOException`          | `BashExec.runCommand`                 | ローカルプロセスの起動・出力読み取り失敗                                                         |
| `IOException`          | `Curl.exec`                           | HTTPリクエストのネットワーク障害                                                           |
| `IOException`          | `Main.outLog`                         | ログファイルへの書き込み失敗・出力先ディレクトリ不在またはファイル生成失敗                                        |
| `InterruptedException` | `SshExec.execute`                     | SSH待機中の割り込み                                                                  |
| `InterruptedException` | `SshCommand.waitForBecomeActive`      | ポーリングスリープ中の割り込み                                                              |
| `InterruptedException` | `BashExec.runCommand`                 | `process.waitFor()` 中の割り込み                                                   |
| `InterruptedException` | `Curl.exec`                           | HTTPリクエスト待機中の割り込み                                                            |
| `JSchException`        | `SshExec.isAlive`                     | SSHセッション生成エラー。`session.connect()` が投げるすべての `JSchException` を吸収して `false` を返す |
| `JSchException`        | `SshExec.execute`                     | SSH接続・チャンネル接続失敗                                                              |

### 非検査例外（Unchecked Exception）

| 例外クラス                       | 主な発生箇所                                | 原因                        |
|-----------------------------|---------------------------------------|---------------------------|
| `NumberFormatException`     | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの2行目が整数値でない       |
| `IndexOutOfBoundsException` | `ConnectionInformation.getCiFromFile` | 接続情報ファイルが4行未満             |
| `JacksonException` (非検査)    | `PaperUrlGen` 全メソッド                   | JSONパース失敗（APIレスポンス形式の変更等） |
| `NullPointerException`      | `PaperUrlGen` 全メソッド                   | JSONのキーが存在しない場合           |

---

## 例外伝播経路

```
各ユーティリティクラス
  └─► SshCommand / BashExec
        └─► WksWorkFlow.execScheduledJob
              └─► Main.main
                    ├─ catch (Exception e)
                    ├─ StringWriter + PrintWriter でスタックトレースを文字列化
                    ├─ Main.log にスタックトレースと終了時刻を追記
                    └─ outLog() でログファイル出力

Main.outLog の IOException
  └─► Main.main の throws IOException
        └─► JVM
```

---

## 特例：`SshExec.isAlive()` の例外吸収

`SshCommand.waitForBecomeActive()` → `SshExec.isAlive()` の流れにおいて、  
`session.connect()` が投げるすべての `JSchException` は `isAlive()` 内で `false` として吸収される。

```java
public class SshExec {
    public boolean isAlive() throws JSchException {
        // 略
        try {
            session.connect();
        } catch (JSchException e) {
            return false;  // 接続失敗 = サーバ未応答として扱う
        }
        // 略
    }
}
```

これにより `shutdown` 後の再起動待ちがポーリングで実現される。  
なお、`getSessionInstance()` 内での `JSchException`（秘密鍵読込失敗等）は吸収されず上位に伝播する。

---

## ログ未出力のケース

以下の状況ではログファイルが出力されない（または不完全になる）。

| 状況                     | 結果                                            |
|------------------------|-----------------------------------------------|
| `main` に渡す引数が5つでない     | `System.out` にUSAGEメッセージを出力して終了。ログファイルは生成されない |
| ワークフロー処理途中で例外が発生       | スタックトレースと終了時刻を追記し、途中までの実行ログとともにログファイルへ出力する    |
| ログファイルの出力先ディレクトリが存在しない | `IOException` が発生してJVMが終了                     |

---

## 改善の余地

- 各処理ステップを `try-catch` で囲み、一部サーバの失敗時に残りのサーバ処理を継続する設計にすることで可用性が向上する。
- `PaperUrlGen` の `JacksonException` / `NullPointerException` に対する明示的なハンドリングを追加することで、APIレスポンス変更時のデバッグが容易になる。
- `Curl.exec` におけるHTTPステータスコードの検証を追加することで、APIエラー時の原因特定が容易になる。

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

| 例外クラス                               | 主な発生箇所                          | 原因                                                                                                         |
|------------------------------------------|---------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `FileNotFoundException`（`IOException`） | `SshCommand.wgetPaperMc`              | 管理端末上のPaperMCバージョンファイルのオープン失敗                                                          |
| `IOException`                            | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの読み込み失敗                                                                               |
| `IOException`                            | `SshExec.execute`                     | SSH標準出力の読み取り失敗                                                                                    |
| `IOException`                            | `BashExec.runCommand`                 | ローカルプロセスの起動・出力読み取り失敗                                                                     |
| `IOException`                            | `Curl.exec`                           | HTTPリクエストのネットワーク障害                                                                             |
| `IOException`                            | `Main.outLog`                         | ログファイルへの書き込み失敗・出力先ディレクトリ不在またはファイル生成失敗                                   |
| `InterruptedException`                   | `SshExec.execute`                     | SSH待機中の割り込み                                                                                          |
| `InterruptedException`                   | `SshCommand.waitForBecomeActive`      | ポーリングスリープ中の割り込み                                                                               |
| `InterruptedException`                   | `BashExec.runCommand`                 | `process.waitFor()` 中の割り込み                                                                             |
| `InterruptedException`                   | `Curl.exec`                           | HTTPリクエスト待機中の割り込み                                                                               |
| `JSchException`                          | `SshExec.isAlive`                     | SSHセッション生成エラー。`session.connect(60000)` が投げるすべての `JSchException` を吸収して `false` を返す |
| `JSchException`                          | `SshExec.execute`                     | SSH接続・チャンネル接続失敗                                                                                  |

### 非検査例外（Unchecked Exception）

| 例外クラス                  | 主な発生箇所                          | 原因                                                  |
|-----------------------------|---------------------------------------|-------------------------------------------------------|
| `RuntimeException`          | `PaperUrlGen.getPaperMcVersion`       | 全系列に正式版が見つからない（空の一覧を含む）        |
| `RuntimeException`          | `UpdateThread.exceptionDetection`     | 子スレッドが保持したその他Exceptionを原因付きで再送出 |
| `RuntimeException`          | `SshCommand.mount` / `umount`         | マウント有無が期待状態と不一致                        |
| `RuntimeException`          | `SshCommand.waitForBecomeActive`      | 失敗したSSH接続試行後、指定秒数の経過を検出           |
| `NumberFormatException`     | `BashExec.getNowTime`                 | UNIX時刻の先頭行をlongに変換できない                  |
| `NumberFormatException`     | `ConnectionInformation.getCiFromFile` | 接続情報ファイルの2行目が整数値でない                 |
| `IndexOutOfBoundsException` | `ConnectionInformation.getCiFromFile` | 接続情報ファイルが4行未満                             |
| `JacksonException` (非検査) | `PaperUrlGen` 全メソッド              | JSONパース失敗（APIレスポンス形式の変更等）           |
| `NullPointerException`      | `PaperUrlGen` 全メソッド              | JSONのキーが存在しない場合                            |

---

## 異常時運用（No.309）

- 初期疎通不可は例外とせず、最初の失敗対象のログと `WARNING: Servers Not Active` を残して保守せず復帰する。
- アンマウント後に `findmnt` 出力が残る場合、マウント後に出力がない場合は `OTHER_MOUNT_STATUS_EXCEPTION` で中止する。
- 再起動後は各サーバ600秒目安でSSH復帰を待ち、失敗した接続試行後に期限超過なら `OTHER_REBOOT_EXCEPTION`
  に対象接続情報を埋めて中止する。最後の試行で成功した場合は続行する。
- 時刻取得・待機で発生した `IOException`・`InterruptedException` もMainへ伝播する。
- 例外発生時は後続工程を実行しない。PaperMC固有の例外でもSchubertを起動しない。自動復旧・ロールバックは行わず、ログを確認して手動復旧する。
- `SshExec.execute()` の終了コード取得は追加しない。

---

## 並列更新の例外伝播（No.310）

- 更新スレッド内の例外は保存し、全スレッドの終了待ち後に親スレッドで引数順に検出する。
- `JSchException`・`IOException`・`InterruptedException` は同じ例外を再送出し、その他の `Exception` は原因を保持した
  `RuntimeException` で再送出する。
- 親スレッドで例外を再送出した場合は、再起動以降を実行せず、従来どおりMainでログ出力して手動復旧する。
- 更新失敗時もほかの開始済みタスクはキャンセルせず、終了を待つ。複数の失敗は集約せず、引数順で最初に検出した例外を再送出する。

```
子: UpdateThread.run()
  → update → upgrade
  → catch: 例外を対応フィールドへ保存して終了
親: WksWorkFlow.allUpdate()
  → 全スレッドjoin
  → 引数順にexceptionDetection()
  → 検査例外はそのまま、その他ExceptionはRuntimeException(cause)で再送出
  → WksWorkFlow.execScheduledJob → Main.main
  → スタックトレース・終了時刻記録 → ログ出力 → 手動復旧
```

`join()` 自体の割り込みは親の `InterruptedException` として上位へ委譲する。子スレッドのキャンセルは行わない。
`Error` は捕捉対象外。共有ログは `List<String>` 型の `CopyOnWriteArrayList` を使用し、並列追記に対応するが、サーバ間の記録順は不定である。

---

## 例外伝播経路

PaperMC正式版未検出時は `RuntimeException(OTHER_PAPERMC_EXCEPTION)`（メッセージ：
`Exception: No official version was found.`）を送出する。
バージョンファイルの `new PrintStream(PATH_PAPERMC_VERSION)` で発生する `FileNotFoundException` は `IOException`
として伝播する。
両者ともワークフローの後続処理を中断し、既存の `Main.main` でスタックトレースをログへ記録する。

出力到達前の例外発生時はバージョンファイルを更新せず、既存内容が残る。
出力先ディレクトリの存在と作成・上書き権限、十分なディスク容量を前提とし、`PrintStream` の書き込み・クローズ時のエラーフラグ確認は行わない。

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
`session.connect(60000)` が投げるすべての `JSchException` は `isAlive()` 内で `false` として吸収される。

```java
public class SshExec {
    public boolean isAlive() throws JSchException {
        // 略
        try {
            session.connect(60000);
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

| 状況                                         | 結果                                                                                 |
|----------------------------------------------|--------------------------------------------------------------------------------------|
| `main` に渡す引数が6つでない                 | `System.out` にUSAGEメッセージを出力して終了。ログファイルは生成されない             |
| ワークフロー処理途中で例外が発生             | スタックトレースと終了時刻を追記し、途中までの実行ログとともにログファイルへ出力する |
| ログファイルの出力先ディレクトリが存在しない | `IOException` が発生してJVMが終了                                                    |

---

## 改善の余地

- `PaperUrlGen` の `JacksonException` / `NullPointerException` に対する明示的なハンドリングを追加することで、APIレスポンス変更時のデバッグが容易になる。
- `Curl.exec` におけるHTTPステータスコードの検証を追加することで、APIエラー時の原因特定が容易になる。

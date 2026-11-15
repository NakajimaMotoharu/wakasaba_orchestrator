# wakasaba_orchestrator 基本設計書：システム全体概要

## 目次

- [システム概要](#システム概要)
- [パッケージ構成](#パッケージ構成)
- [クラス一覧](#クラス一覧)
- [クラス間依存関係](#クラス間依存関係)
- [依存ライブラリ](#依存ライブラリ)
- [処理フロー概要](#処理フロー概要)
- [例外ハンドリング方針](#例外ハンドリング方針)
- [ログ設計](#ログ設計)
- [出力ファイル](#出力ファイル)
- [外部連携](#外部連携)

---

## システム概要

| 項目             | 内容                                                                                                                                             |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| システム名       | wakasaba_orchestrator                                                                                                                            |
| 目的             | 複数のLinuxサーバに対して定型メンテナンス処理（OS更新・PaperMCサーバ更新・バックアップ等）を自動実行するオーケストレーションバッチ               |
| 実行環境         | Java 21 以上（`SequencedCollection`（`List.getFirst()` 等）および `HttpClient` の `AutoCloseable` 対応を使用）/ Gradle ビルド                    |
| 実行形式         | Fat JAR（Shadow JAR）によるコマンドライン実行                                                                                                    |
| エントリポイント | `com.wks.main.Main`                                                                                                                              |
| 対象サーバ構成   | リモートサーバ6台（SSH接続）＋自サーバ1台（ローカル実行）                                                                                        |
| ビルドツール     | Gradle / Shadow Plugin 9.3.2                                                                                                                     |
| 実行コマンド例   | `java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file> <server3_file> <server4_file> <server5_file>` |

---

## パッケージ構成

```
com.wks
├── main
│   └── Main.java                   # エントリポイント・グローバルログ管理・ログファイル出力
├── workflow
│   └── WksWorkFlow.java            # 全サーバへの処理シーケンス定義
├── cmd
│   └── SshCommand.java             # SSH経由コマンド実行の業務ロジックラッパー
├── papermc
│   └── PaperUrlGen.java            # PaperMC / Pl3xMap API レスポンスJSON解析
├── util
│   ├── ConnectionInformation.java  # サーバ接続情報の保持・ファイル読込み（record）
│   ├── SshExec.java                # JSch を用いた SSH セッション管理・コマンド実行
│   ├── BashExec.java               # ProcessBuilder を用いたローカル Bash コマンド実行
│   └── Curl.java                   # Java 標準 HttpClient を用いた HTTP GET リクエスト
└── parts
    └── WksConstants.java           # アプリケーション全体の定数を一元管理
```

---

## クラス一覧

| クラス名                | パッケージ         | 種別   | 役割                                                            |
|-------------------------|--------------------|--------|-----------------------------------------------------------------|
| `Main`                  | `com.wks.main`     | class  | エントリポイント。引数検証・ワークフロー起動・ログファイル出力  |
| `WksWorkFlow`           | `com.wks.workflow` | class  | 6台のリモートサーバ＋自サーバへの処理シーケンスを順次実行       |
| `SshCommand`            | `com.wks.cmd`      | class  | SSH経由コマンド実行の業務ロジックラッパー。サーバ応答待機を含む |
| `PaperUrlGen`           | `com.wks.papermc`  | class  | PaperMC API・Modrinth API のJSONレスポンス解析                  |
| `ConnectionInformation` | `com.wks.util`     | record | SSH接続情報（host/port/user/key）の保持とファイル読込み         |
| `SshExec`               | `com.wks.util`     | class  | JSch を使用した SSH セッション確立・コマンド実行・疎通確認      |
| `BashExec`              | `com.wks.util`     | class  | ProcessBuilder によるローカル Bash コマンド実行                 |
| `Curl`                  | `com.wks.util`     | class  | Java 標準 HttpClient による HTTP GET リクエスト                 |
| `WksConstants`          | `com.wks.parts`    | class  | アプリケーション全体で使用する定数の一元管理                    |

---

## クラス間依存関係

```
Main
├── WksWorkFlow
│   ├── SshCommand
│   │   ├── SshExec
│   │   │   └── ConnectionInformation
│   │   ├── BashExec
│   │   ├── Curl
│   │   ├── PaperUrlGen
│   │   │   └── WksConstants
│   │   └── WksConstants
│   ├── BashExec
│   │   └── WksConstants
│   ├── ConnectionInformation
│   │   └── WksConstants
│   └── WksConstants
└── WksConstants
```

- `Main.log`（`public static final ArrayList<String>`）を起点として、全クラスが同一ログリストを共有参照する。

---

## 依存ライブラリ

| ライブラリ                            | バージョン           | 用途                                     |
|---------------------------------------|----------------------|------------------------------------------|
| `com.github.mwiede:jsch`              | 最新版（`+`指定）    | SSH接続・コマンド実行（JSch フォーク版） |
| `tools.jackson.core:jackson-core`     | 最新版（`+`指定）    | JSON解析（コア）                         |
| `tools.jackson.core:jackson-databind` | 最新版（`+`指定）    | JSON解析（ObjectMapper）                 |
| `org.junit.jupiter:junit-jupiter`     | JUnit BOM 5.10.0準拠 | テスト用（Fat JAR には含まれない）       |

---

## 処理フロー概要

Mainで6引数を検証し、開始ログを記録して次のワークフローを呼び出す。

```
1. setConnectionInformation(servers): 6ファイルを読み込み cis[0]～cis[5] を設定
2. isAllAlive(cis): 引数順に確認前ログを追記しSSH疎通確認
   最初の疎通不可 → WARNING: Servers Not Active → 保守せずMainへ復帰
3. systemStop(cis): PaperMC停止 → Schubert停止 → 管理端末で60秒待機（1回）
4. allUMount(cis): サーバ0～4の /nas・/share を解除し、findmntの出力がないことを確認
5. allUpdate(cis): サーバ0～5で1台ずつ update → upgrade
6. allReboot(cis): サーバ0～5へ順に再起動指示
7. BashExec.waitOneMin(): 管理端末で60秒待機（1回）
8. allRebootComplete(cis): サーバ0～5を順に確認（各呼び出し600秒目安）
9. allMount(cis): サーバ0～4の /nas・/share を接続し、findmntの出力があることを確認
10. systemPreSetup(cis): サーバ1でバックアップ → PaperMC/Pl3xMap取得 → SHA検証・配置
    wgetPaperMcの末尾で、管理端末のpapermc_version.txtを更新
11. systemStart(cis): PaperMC起動 → Schubert起動
12. 管理端末: 区切りログ → BashExec.update() → upgrade() → shutdown()
13. Mainで終了時刻記録・ログファイル出力
例外発生: 後続工程中止 → Mainでスタックトレース・終了ログ出力 → 手動復旧
```

| インデックス | 定数              | 役割             | 保守対象                                                           |
|--------------|-------------------|------------------|--------------------------------------------------------------------|
| 0            | `SERVER_TEST`     | テスト           | OS更新・再起動・共有解除と復旧                                     |
| 1            | `SERVER_PAPER`    | PaperMC          | OS更新・再起動・共有解除と復旧・PaperMC停止/バックアップ/更新/起動 |
| 2            | `SERVER_LLM`      | ローカルLLM      | OS更新・再起動・共有解除と復旧                                     |
| 3            | `SERVER_SCHUBERT` | Schubert         | OS更新・再起動・共有解除と復旧・Schubert停止/起動                  |
| 4            | `SERVER_VPN`      | VPN              | OS更新・再起動・共有解除と復旧                                     |
| 5            | `SERVER_NAS`      | NAS              | OS更新・再起動。自身への共有操作は行わない                         |
| 管理端末     | -                 | 本バッチ実行端末 | 固定待機・時刻取得、最後にOS更新・60秒遅延再起動                   |

- 全台メンテナンスは工程単位の直列処理であり、並列実行ではない。
- 全機疎通確認はサービス停止前に行い、最初の失敗で中止する。管理端末のOS更新も開始しない。
- 初期疎通確認前に `Checking Connection: %s`（host・port・user）を記録する。リモート各台の `LOG_SPLIT`
  は記録せず、管理端末の区切りのみ維持する。
- NAS以外のリモート5台には `/nas`・`/share` の設定、`findmnt` と必要な権限があることを前提とする。管理端末は共有操作対象外。
- 両サービス停止後の固定待機は管理端末で1回60秒のみ。停止状態の動的確認は行わない。
- 全台再起動指示後も管理端末で1回60秒待機し、その後SSH復帰を待つ。boot_idは確認しない。
- 復帰待ちはサーバごとに `SERVER_REBOOT_WAIT_LIMIT = 600` 秒を指定する。失敗した試行後に期限超過を確認し、最後の試行が成功すれば期限後でも続行する。厳密な600秒の締切ではない。
- SSH疎通確認は `session.connect(60000)` を使用する。通常のコマンド実行前の1引数版待機は `Integer.MAX_VALUE`
  秒を指定するため、再起動後の明示待機とは異なる。
- バックアップ・ダウンロード・配置は全台復帰と全共有再マウント後。バージョンファイルの内容・出力端末・SHA検証前の出力仕様はNo.307・308から変更しない。
- マウント有無は `findmnt` の標準出力の有無で判定する。状態不一致は実行時例外。共有元照合、確認コマンド自体の失敗検知、解除リトライは追加しない。
- 例外時は残りの工程を打ち切り、サービスの自動復旧やロールバックは行わない。PaperMC処理の例外でもSchubertを起動せず、手動復旧する。
- 自サーバの再起動は従来どおり60秒遅延バックグラウンド実行。

---

## 例外ハンドリング方針

- 各クラスの業務メソッドは検査例外を `throws` で `Main.main` まで委譲する。個別の回復処理は行わない。
- `Main.main` はワークフロー実行中の `Exception` を捕捉し、スタックトレースをログへ追記した後、終了時刻記録とログファイル出力へ進む。
- `Main.outLog` が送出する `IOException` は捕捉せずJVMへ伝播する。
- PaperMCの正式版未検出時は `RuntimeException`、バージョンファイルのオープン失敗時は `FileNotFoundException`（
  `IOException`）が発生し、ワークフローの後続処理を中断して `Main.main` でログへ記録する。
- `SshExec.isAlive()` 内部のみ例外を吸収する。`session.connect(60000)` が投げるすべての `JSchException` を `false`
  として返す。処理開始時疎通確認では全体の保守開始可否の判定に使用し、再起動待ちではポーリング継続判定に使用する。

| 例外クラス                            | 主な発生状況                                                                                        |
|---------------------------------------|-----------------------------------------------------------------------------------------------------|
| `IOException`                         | ファイル読込・SSH出力読取・HTTP通信・ログ書込失敗・ログ出力先ディレクトリ不在またはファイル生成失敗 |
| `InterruptedException`                | SSH / HTTP 待機中・`process.waitFor()` 中の割り込み                                                 |
| `JSchException`                       | SSH セッション生成・接続・チャンネル接続失敗                                                        |
| `NumberFormatException`（非検査）     | 接続情報ファイルのポート番号が整数でない                                                            |
| `IndexOutOfBoundsException`（非検査） | 接続情報ファイルの行数が4行未満                                                                     |

---

## ログ設計

- `Main.log`（`public static final ArrayList<String>`）を全クラス共通のグローバルログバッファとして使用する。
- 各クラスは `Main.log` への参照を `private static final ArrayList<String> log = Main.log` として保持し、実行中に随時追記する。
- ワークフローの正常・異常終了後、`Main.outLog()` が `log` の全行をファイルへ書き出す。
- ログファイルのパスは `WksConstants.PATH_EXEC_LOG`（`/home/mini/wakasaba_orchestrator/log/log_%s.txt`
  ）に終了時刻タイムスタンプを埋め込んだ値となる。
- ワークフロー実行中に例外が発生した場合、スタックトレースと終了時刻を途中までの実行ログへ追記してファイル出力する。

### ログに記録される内容

| 種別                   | フォーマット例                                                   |
|------------------------|------------------------------------------------------------------|
| バッチ開始時刻         | `Batch start time: 20240101120000`                               |
| バッチ終了時刻         | `Batch end time: 20240101130000`                                 |
| 初期疎通確認対象       | `Checking Connection: {host: "x.x.x.x", port: 22, user: "mini"}` |
| 自サーバ処理開始区切り | `/* --------Job start: this server-------- */`                   |
| 実行コマンド           | `$ sudo apt update`                                              |
| コマンド標準出力       | （コマンド実行結果の各行）                                       |
| 例外情報               | ワークフロー実行中に捕捉した例外のスタックトレース               |

---

## 出力ファイル

| ファイル          | 管理端末上の出力先                                     | 内容・更新方式                                                                     |
|-------------------|--------------------------------------------------------|------------------------------------------------------------------------------------|
| 実行ログ          | `/home/mini/wakasaba_orchestrator/log/log_%s.txt`      | 終了時刻ごとの実行ログ。`PATH_WAKASABA_EXEC + "log/log_%s.txt"` から構成           |
| PaperMCバージョン | `/home/mini/wakasaba_orchestrator/papermc_version.txt` | 取得対象のバージョン文字列のみ。既定文字コード、末尾改行なしで新規作成または上書き |

PaperMCはAPI応答順に系列・候補を探索し、ハイフン（`-`
）を含むpre・rc等を除外する。系列内に正式版がなければ次の系列へ進み、全系列に正式版がなければ実行時例外とする。数値比較・並べ替えは行わない。

バージョンファイルは `wgetPaperMc` のPl3xMap条件分岐後、SHA検証・本番配置前に `PrintStream.print(version)` で出力する。
本番配置・起動完了を示すものではなく、初期疎通不可による全体中止や出力到達前の例外発生時は以前のファイルが残る。
出力先ディレクトリの存在、作成・上書き権限、十分なディスク容量を前提とし、親ディレクトリの自動作成や書き込み・クローズ時のエラーフラグ確認は行わない。

---

## 外部連携

### SSH接続（リモートサーバ操作）

- ライブラリ: `com.github.mwiede:jsch`
- 認証方式: 公開鍵認証（秘密鍵ファイルパスを接続情報ファイルで指定）
- `StrictHostKeyChecking=no` を設定し、known_hosts 検証を無効化する。
- コマンドは `ChannelExec` モードで1コマンドずつ実行する。
- サーバ再起動後の再接続は `SshCommand.waitForBecomeActive()`
  がポーリング（1秒間隔）で自動待機する。この待機はすべてのSSHコマンド実行前に行われるため、再起動中のサーバへも自動でリトライが行われる。
- 全6台の初期SSH疎通を確認し、1台でも疎通不可なら警告を記録して全体の保守を開始しない。

### HTTP REST API

- ライブラリ: Java 標準 `java.net.http.HttpClient`（HTTP/1.1、接続タイムアウト60秒）
- User-Agent: `wakasaba_orchestrator/1.0`

| API          | 用途                                    | エンドポイント                                                            |
|--------------|-----------------------------------------|---------------------------------------------------------------------------|
| PaperMC API  | バージョン一覧取得                      | `GET https://fill.papermc.io/v3/projects/paper`                           |
| PaperMC API  | ビルド情報・ダウンロードURL・SHA256取得 | `GET https://fill.papermc.io/v3/projects/paper/versions/{version}/builds` |
| Modrinth API | Pl3xMap ダウンロードURL・SHA512取得     | `GET https://api.modrinth.com/v2/project/pl3xmap/version`                 |

### ローカルBash実行

- Java `ProcessBuilder` を使用して `sh -c <コマンド>` の形式で実行する。
- `update` / `upgrade` / `waitOneMin` / `getNowTime` はプロセス終了を `waitFor()` で同期待機する。
- `shutdown` のみバックグラウンド実行（`waitFor()` 呼出しなし）とし、60秒遅延後に再起動する。

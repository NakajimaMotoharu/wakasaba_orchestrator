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
- [外部連携](#外部連携)

---

## システム概要

| 項目       | 内容                                                                                                  |
|----------|-----------------------------------------------------------------------------------------------------|
| システム名    | wakasaba_orchestrator                                                                               |
| 目的       | 複数のLinuxサーバに対して定型メンテナンス処理（OS更新・PaperMCサーバ更新・バックアップ等）を自動実行するオーケストレーションバッチ                            |
| 実行環境     | Java 17 以上（Record を使用）/ Gradle ビルド                                                                  |
| 実行形式     | Fat JAR（Shadow JAR）によるコマンドライン実行                                                                     |
| エントリポイント | `com.wks.main.Main`                                                                                 |
| 対象サーバ構成  | リモートサーバ3台（SSH接続）＋自サーバ1台（ローカル実行）                                                                     |
| ビルドツール   | Gradle / Shadow Plugin 9.3.2                                                                        |
| 実行コマンド例  | `java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file>` |

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

| クラス名                    | パッケージ              | 種別     | 役割                                     |
|-------------------------|--------------------|--------|----------------------------------------|
| `Main`                  | `com.wks.main`     | class  | エントリポイント。引数検証・ワークフロー起動・ログファイル出力        |
| `WksWorkFlow`           | `com.wks.workflow` | class  | 3台のリモートサーバ＋自サーバへの処理シーケンスを順次実行          |
| `SshCommand`            | `com.wks.cmd`      | class  | SSH経由コマンド実行の業務ロジックラッパー。サーバ応答待機を含む      |
| `PaperUrlGen`           | `com.wks.papermc`  | class  | PaperMC API・Modrinth API のJSONレスポンス解析  |
| `ConnectionInformation` | `com.wks.util`     | record | SSH接続情報（host/port/user/key）の保持とファイル読込み |
| `SshExec`               | `com.wks.util`     | class  | JSch を使用した SSH セッション確立・コマンド実行・疎通確認     |
| `BashExec`              | `com.wks.util`     | class  | ProcessBuilder によるローカル Bash コマンド実行     |
| `Curl`                  | `com.wks.util`     | class  | Java 標準 HttpClient による HTTP GET リクエスト  |
| `WksConstants`          | `com.wks.parts`    | class  | アプリケーション全体で使用する定数の一元管理                 |

---

## クラス間依存関係

```
Main
├── WksWorkFlow
│   ├── SshCommand
│   │   ├── SshExec
│   │   │   └── ConnectionInformation
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

| ライブラリ                                 | バージョン              | 用途                       |
|---------------------------------------|--------------------|--------------------------|
| `com.github.mwiede:jsch`              | 最新版（`+`指定）         | SSH接続・コマンド実行（JSch フォーク版） |
| `tools.jackson.core:jackson-core`     | 最新版（`+`指定）         | JSON解析（コア）               |
| `tools.jackson.core:jackson-databind` | 最新版（`+`指定）         | JSON解析（ObjectMapper）     |
| `org.junit.jupiter:junit-jupiter`     | JUnit BOM 5.10.0準拠 | テスト用（Fat JAR には含まれない）    |

---

## 処理フロー概要

```
main(args)
│
├─ [引数チェック] args.length == 3 ?
│     No  → USAGEメッセージ出力して終了
│     Yes ↓
│
├─ log に開始時刻を追記
│
├─ WksWorkFlow.execScheduledJob(args)
│   │
│   ├─ [サーバ0] OS メンテナンス（SSH）
│   │   ├─ apt update
│   │   ├─ apt upgrade -y
│   │   └─ shutdown -r now（即時再起動）
│   │
│   ├─ [サーバ1] PaperMC サーバメンテナンス（SSH）
│   │   ├─ PaperMC サービス停止
│   │   ├─ apt update
│   │   ├─ apt upgrade -y
│   │   ├─ バックアップシェル実行
│   │   ├─ PaperMC / Pl3xMap 最新版ダウンロード（API 取得 → wget）
│   │   ├─ SHA検証 → 本番ディレクトリへ配置
│   │   ├─ shutdown -r now（即時再起動）
│   │   └─ PaperMC サービス起動
│   │
│   ├─ [サーバ2] OS メンテナンス（SSH）
│   │   ├─ apt update
│   │   ├─ apt upgrade -y
│   │   └─ shutdown -r now（即時再起動）
│   │
│   └─ [自サーバ] OS メンテナンス（Bash）
│       ├─ apt update
│       ├─ apt upgrade -y
│       └─ (sleep 60 && shutdown -r now) &（60秒遅延バックグラウンド再起動）
│
├─ log に終了時刻を追記
└─ log をファイルへ出力（log_yyyyMMddHHmmss.txt）
```

---

## 例外ハンドリング方針

- 各クラスの業務メソッドは検査例外を `throws` で上位へ委譲する。個別の `try-catch` による回復処理は行わない。
- 全例外は最終的に `Main.main` まで伝播し、JVM がスタックトレースを標準エラーへ出力して終了する。
- `SshExec.isAlive()` 内部のみ例外を吸収する。`session.connect()` が投げるすべての `JSchException` を `false`
  として返し、再起動中のサーバへのポーリング待機を実現する。

| 例外クラス                            | 主な発生状況                                                |
|----------------------------------|-------------------------------------------------------|
| `IOException`                    | ファイル読込・SSH出力読取・HTTP通信・ログ書込失敗・ログ出力先ディレクトリ不在またはファイル生成失敗 |
| `InterruptedException`           | SSH / HTTP 待機中・`process.waitFor()` 中の割り込み             |
| `JSchException`                  | SSH セッション生成・接続・チャンネル接続失敗                              |
| `NumberFormatException`（非検査）     | 接続情報ファイルのポート番号が整数でない                                  |
| `IndexOutOfBoundsException`（非検査） | 接続情報ファイルの行数が4行未満                                      |

---

## ログ設計

- `Main.log`（`public static final ArrayList<String>`）を全クラス共通のグローバルログバッファとして使用する。
- 各クラスは `Main.log` への参照を `private static final ArrayList<String> log = Main.log` として保持し、実行中に随時追記する。
- バッチ終了後、`Main.outLog()` が `log` の全行をファイルへ書き出す。
- ログファイルのパスは `WksConstants.PATH_EXEC_LOG`（`/home/mini/wakasaba_orchestrator/log/log_%s.txt`
  ）に終了時刻タイムスタンプを埋め込んだ値となる。
- 途中で例外が発生した場合、`outLog()` に到達しないためログファイルは生成されない。

### ログに記録される内容

| 種別          | フォーマット例                                                                      |
|-------------|------------------------------------------------------------------------------|
| バッチ開始時刻     | `Batch start time: 20240101120000`                                           |
| バッチ終了時刻     | `Batch end time: 20240101130000`                                             |
| サーバ処理開始区切り  | `/* --------Job start: {host: "x.x.x.x", port: 22, user: "mini"}-------- */` |
| 自サーバ処理開始区切り | `/* --------Job start: this server-------- */`                               |
| 実行コマンド      | `$ sudo apt update`                                                          |
| コマンド標準出力    | （コマンド実行結果の各行）                                                                |

---

## 外部連携

### SSH接続（リモートサーバ操作）

- ライブラリ: `com.github.mwiede:jsch`
- 認証方式: 公開鍵認証（秘密鍵ファイルパスを接続情報ファイルで指定）
- `StrictHostKeyChecking=no` を設定し、known_hosts 検証を無効化する。
- コマンドは `ChannelExec` モードで1コマンドずつ実行する。
- サーバ再起動後の再接続は `SshCommand.waitForBecomeActive()`
  がポーリング（1秒間隔）で自動待機する。この待機はすべてのSSHコマンド実行前に行われるため、再起動中のサーバへも自動でリトライが行われる。

### HTTP REST API

- ライブラリ: Java 標準 `java.net.http.HttpClient`（HTTP/1.1、接続タイムアウト60秒）
- User-Agent: `wakasaba_orchestrator/1.0`

| API          | 用途                         | エンドポイント                                                                   |
|--------------|----------------------------|---------------------------------------------------------------------------|
| PaperMC API  | バージョン一覧取得                  | `GET https://fill.papermc.io/v3/projects/paper`                           |
| PaperMC API  | ビルド情報・ダウンロードURL・SHA256取得   | `GET https://fill.papermc.io/v3/projects/paper/versions/{version}/builds` |
| Modrinth API | Pl3xMap ダウンロードURL・SHA512取得 | `GET https://api.modrinth.com/v2/project/pl3xmap/version`                 |

### ローカルBash実行

- Java `ProcessBuilder` を使用して `sh -c <コマンド>` の形式で実行する。
- `update` / `upgrade` はプロセス終了を `waitFor()` で同期待機する。
- `shutdown` のみバックグラウンド実行（`waitFor()` 呼出しなし）とし、60秒遅延後に再起動する。

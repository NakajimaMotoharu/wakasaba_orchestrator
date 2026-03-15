# wakasaba_orchestrator システム全体構成

## 目次

- [システム概要](#システム概要)
- [パッケージ構成](#パッケージ構成)
- [クラス一覧](#クラス一覧)
- [依存ライブラリ](#依存ライブラリ)
- [処理フロー概要](#処理フロー概要)

---

## システム概要

| 項目       | 内容                                                                                 |
|----------|------------------------------------------------------------------------------------|
| システム名    | wakasaba_orchestrator                                                              |
| 目的       | 複数のLinuxサーバに対して、スケジュールに従い定型メンテナンス処理（OS更新・PaperMCサーバ更新・バックアップ等）を自動実行するオーケストレーションバッチ |
| 実行環境     | Java 17 以上（Recordを使用）/ Gradle ビルド                                                  |
| 実行形式     | Fat JAR（Shadow Jar）によるコマンドライン実行                                                    |
| エントリポイント | `com.wks.main.Main`                                                                |

### 実行コマンド例

```bash
java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file>
```

各引数はサーバ接続情報ファイルのパスを示す。ファイルフォーマットについては [補足資料：接続情報ファイル仕様](./290_SERVER_FILE_SPEC)
を参照。

---

## パッケージ構成

```
com.wks
├── main
│   └── Main.java                  # エントリポイント・ログ管理
├── workflow
│   └── WksWorkFlow.java           # サーバ操作シーケンス定義
├── cmd
│   └── SshCommand.java            # SSH経由コマンド実行ラッパー
├── papermc
│   └── PaperUrlGen.java           # PaperMC/Pl3xMap APIパーサ
├── util
│   ├── ConnectionInformation.java # サーバ接続情報Record
│   ├── SshExec.java               # JSch SSH実行エンジン
│   ├── BashExec.java              # ローカルBashコマンド実行
│   └── Curl.java                  # HTTPクライアント（API呼出し）
└── parts
    └── WksConstants.java          # 全定数定義
```

---

## クラス一覧

| クラス名                                                   | パッケージ              | 種別     | 役割                                 |
|--------------------------------------------------------|--------------------|--------|------------------------------------|
| [Main](./201_Main)                                   | `com.wks.main`     | class  | エントリポイント。引数検証・ログ管理・ログファイル出力        |
| [WksWorkFlow](./202_WksWorkFlow)                     | `com.wks.workflow` | class  | 3台のリモートサーバ＋自サーバへの処理シーケンス定義         |
| [SshCommand](./203_SshCommand)                       | `com.wks.cmd`      | class  | SSH経由コマンド実行の業務ロジックラッパー             |
| [PaperUrlGen](./204_PaperUrlGen)                     | `com.wks.papermc`  | class  | PaperMC・Pl3xMap APIレスポンスJSONパーサ    |
| [ConnectionInformation](./205_ConnectionInformation) | `com.wks.util`     | record | サーバ接続情報（host/port/user/key）の保持と読込み |
| [SshExec](./206_SshExec)                             | `com.wks.util`     | class  | JSch を用いたSSHセッション管理・コマンド実行         |
| [BashExec](./207_BashExec)                           | `com.wks.util`     | class  | ProcessBuilder を用いたローカルBashコマンド実行  |
| [Curl](./208_Curl)                                   | `com.wks.util`     | class  | Java標準HttpClient を用いたHTTP GETリクエスト |
| [WksConstants](./209_WksConstants)                   | `com.wks.parts`    | class  | アプリケーション全体で使用する定数の一元管理             |

---

## 依存ライブラリ

| ライブラリ                                 | バージョン               | 用途                       |
|---------------------------------------|---------------------|--------------------------|
| `com.github.mwiede:jsch`              | 最新版（`+`指定）          | SSH接続・コマンド実行（JSch フォーク版） |
| `tools.jackson.core:jackson-core`     | 最新版（`+`指定）          | JSONパース（コア）              |
| `tools.jackson.core:jackson-databind` | 最新版（`+`指定）          | JSONパース（ObjectMapper）    |
| `org.junit.jupiter:junit-jupiter`     | JUnit BOM 5.10.0 準拠 | テスト用（本体には含まれない）          |

ビルドツール: **Gradle** / パッケージング: **Shadow Plugin 9.3.2**（Fat JAR生成）

---

## 処理フロー概要

```
main(args)
│
├─ [引数チェック] args.length == 3 ?
│     No  → USAGEメッセージ出力して終了
│     Yes ↓
│
├─ ログ: 開始時刻記録
│
├─ WksWorkFlow.execScheduledJob(args)
│   │
│   ├─ [サーバ0] 接続情報読込み
│   │   ├─ update
│   │   ├─ upgrade
│   │   └─ shutdown (reboot)
│   │
│   ├─ [サーバ1] 接続情報読込み  ← PaperMCサーバ
│   │   ├─ stopPaperMC
│   │   ├─ update
│   │   ├─ upgrade
│   │   ├─ backupPaperMC
│   │   ├─ wgetPaperMc          ← PaperMC/Pl3xMap 最新版ダウンロード
│   │   ├─ movePaperMc          ← SHA256/SHA512検証 → ファイル配置
│   │   ├─ shutdown (reboot)
│   │   └─ startPaperMC
│   │
│   ├─ [サーバ2] 接続情報読込み
│   │   ├─ update
│   │   ├─ upgrade
│   │   └─ shutdown (reboot)
│   │
│   └─ [自サーバ] BashExec経由で実行
│       ├─ update
│       ├─ upgrade
│       └─ shutdown (60秒後 reboot, バックグラウンド)
│
├─ ログ: 終了時刻記録
└─ ログファイル出力 (log_yyyyMMddHHmmss.txt)
```

---

## 補足資料

- [接続情報ファイル仕様](./290_SERVER_FILE_SPEC)
- [PaperMC API仕様メモ](./291_PAPERMC_API_NOTE)
- [例外・エラーハンドリング方針](./292_ERROR_HANDLING)

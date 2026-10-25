# wakasaba_orchestrator システム全体構成

## 目次

- [システム概要](#システム概要)
- [パッケージ構成](#パッケージ構成)
- [クラス一覧](#クラス一覧)
- [依存ライブラリ](#依存ライブラリ)
- [処理フロー概要](#処理フロー概要)
- [出力ファイル](#出力ファイル)
- [PaperMCバージョン選択と例外](#papermcバージョン選択と例外)

---

## システム概要

| 項目             | 内容                                                                                                                                                                                         |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| システム名       | wakasaba_orchestrator                                                                                                                                                                        |
| 目的             | 複数のLinuxサーバに対して定型メンテナンス処理（OS更新・PaperMCサーバ更新・バックアップ等）を自動実行するオーケストレーションバッチ。実行契機は手動実行（スケジューラ等からの呼び出しを含む） |
| 実行環境         | Java 21 以上（`SequencedCollection`（`List.getFirst()` 等）および `HttpClient` の `AutoCloseable` 対応を使用）/ Gradle ビルド                                                                |
| 実行形式         | Fat JAR（Shadow Jar）によるコマンドライン実行                                                                                                                                                |
| エントリポイント | `com.wks.main.Main`                                                                                                                                                                          |

### 実行コマンド例

```bash
java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file> <server3_file> <server4_file>
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

| クラス名                                             | パッケージ         | 種別   | 役割                                                   |
|------------------------------------------------------|--------------------|--------|--------------------------------------------------------|
| [Main](./201_Main)                                   | `com.wks.main`     | class  | エントリポイント。引数検証・ログ管理・ログファイル出力 |
| [WksWorkFlow](./202_WksWorkFlow)                     | `com.wks.workflow` | class  | 5台のリモートサーバ＋自サーバへの処理シーケンス定義    |
| [SshCommand](./203_SshCommand)                       | `com.wks.cmd`      | class  | SSH経由コマンド実行の業務ロジックラッパー              |
| [PaperUrlGen](./204_PaperUrlGen)                     | `com.wks.papermc`  | class  | PaperMC・Pl3xMap APIレスポンスJSONパーサ               |
| [ConnectionInformation](./205_ConnectionInformation) | `com.wks.util`     | record | サーバ接続情報（host/port/user/key）の保持と読込み     |
| [SshExec](./206_SshExec)                             | `com.wks.util`     | class  | JSch を用いたSSHセッション管理・コマンド実行           |
| [BashExec](./207_BashExec)                           | `com.wks.util`     | class  | ProcessBuilder を用いたローカルBashコマンド実行        |
| [Curl](./208_Curl)                                   | `com.wks.util`     | class  | Java標準HttpClient を用いたHTTP GETリクエスト          |
| [WksConstants](./209_WksConstants)                   | `com.wks.parts`    | class  | アプリケーション全体で使用する定数の一元管理           |

---

## 依存ライブラリ

| ライブラリ                            | バージョン            | 用途                                     |
|---------------------------------------|-----------------------|------------------------------------------|
| `com.github.mwiede:jsch`              | 最新版（`+`指定）     | SSH接続・コマンド実行（JSch フォーク版） |
| `tools.jackson.core:jackson-core`     | 最新版（`+`指定）     | JSONパース（コア）                       |
| `tools.jackson.core:jackson-databind` | 最新版（`+`指定）     | JSONパース（ObjectMapper）               |
| `org.junit.jupiter:junit-jupiter`     | JUnit BOM 5.10.0 準拠 | テスト用（本体には含まれない）           |

ビルドツール: **Gradle** / パッケージング: **Shadow Plugin 9.3.2**（Fat JAR生成）

---

## 処理フロー概要

```
main(args)
│
├─ [引数チェック] args.length == 5 ?
│     No  → USAGEメッセージ出力して終了
│     Yes ↓
│
├─ ログ: 開始時刻記録
│
├─ try: WksWorkFlow.execScheduledJob(args)
│   │
│   ├─ [サーバ0] 接続情報読込み
│   │   ├─ isAlive? No → WARNING: Server Not Active をログへ追記して次サーバへ
│   │   ├─ update
│   │   ├─ upgrade
│   │   └─ shutdown (reboot)
│   │
│   ├─ [サーバ1] 接続情報読込み  ← PaperMCサーバ
│   │   ├─ isAlive? No → WARNING: Server Not Active をログへ追記して次サーバへ
│   │   ├─ stopPaperMC
│   │   ├─ waitOneMin           ← sleep 60による安全停止待機
│   │   ├─ update
│   │   ├─ upgrade
│   │   ├─ backupPaperMC
│   │   ├─ wgetPaperMc          ← PaperMC正式版選択・Pl3xMapダウンロード・管理端末へバージョンファイル出力
│   │   ├─ movePaperMc          ← SHA256/SHA512検証 → ファイル配置
│   │   ├─ shutdown (reboot)
│   │   └─ startPaperMC
│   │
│   ├─ [サーバ2] 接続情報読込み
│   │   ├─ isAlive? No → WARNING: Server Not Active をログへ追記して次サーバへ
│   │   ├─ update
│   │   ├─ upgrade
│   │   └─ shutdown (reboot)
│   │
│   ├─ [サーバ3] 接続情報読込み  ← Schubertサーバ
│   │   ├─ isAlive? No → WARNING: Server Not Active をログへ追記してサーバ4へ
│   │   ├─ stopSchubert
│   │   ├─ waitOneMin           ← sleep 60による安全停止待機
│   │   ├─ update
│   │   ├─ upgrade
│   │   ├─ shutdown (reboot)
│   │   └─ startSchubert
│   │
│   ├─ [サーバ4] 接続情報読込み  ← VPNサーバ
│   │   ├─ isAlive? No → WARNING: Server Not Active をログへ追記して自サーバへ
│   │   ├─ update
│   │   ├─ upgrade
│   │   └─ shutdown (reboot)
│   │
│   └─ [自サーバ] BashExec経由で実行
│       ├─ update
│       ├─ upgrade
│       └─ shutdown (60秒後 reboot, バックグラウンド)
│
├─ catch (Exception): スタックトレースをログへ追記
├─ ログ: 終了時刻記録
└─ ログファイル出力 (log_yyyyMMddHHmmss.txt)
```

---

## 出力ファイル

| 定数                   | 管理端末上の出力先                                     | 内容・出力タイミング                                   |
|------------------------|--------------------------------------------------------|--------------------------------------------------------|
| `PATH_EXEC_LOG`        | `/home/mini/wakasaba_orchestrator/log/log_%s.txt`      | 終了時刻ごとの実行ログ。`Main.outLog` で出力           |
| `PATH_PAPERMC_VERSION` | `/home/mini/wakasaba_orchestrator/papermc_version.txt` | 取得対象のバージョン文字列。`wgetPaperMc` の末尾で出力 |

両パスは `PATH_WAKASABA_EXEC`（`/home/mini/wakasaba_orchestrator/`）との定数連結で構成する。
バージョンファイルは `PrintStream(String)` の既定文字コードで、`print(version)` により末尾改行なしで新規作成または上書きする。
Pl3xMapをバージョン不一致でスキップした場合も出力するが、SHA検証・本番配置前のため本番配置・起動完了を示さない。
初期疎通確認によるスキップや出力到達前の例外発生時は更新せず、既存内容が残る。
出力先ディレクトリの存在、作成・上書き権限、十分なディスク容量を前提とし、親ディレクトリの自動作成や書き込み・クローズ時のエラーフラグ確認は行わない。

---

## PaperMCバージョン選択と例外

`getPaperMcVersion` はAPI応答順に系列・候補を探索し、ハイフン（`-`）を含むpre・rc等を除外する。
系列内に正式版がなければ次の系列を探索し、最初に見つかった正式版をダウンロードURLとSHA-256期待値の取得に使用する。数値比較・並べ替えは行わない。

正式版未検出時は `RuntimeException(OTHER_PAPERMC_EXCEPTION)`、バージョンファイルのオープン失敗時は
`FileNotFoundException`（`IOException`）を送出する。
いずれも `WksWorkFlow` を経由し、`Main.main` でスタックトレースをログへ記録する。ワークフローの後続処理は実行しない。

---

## 補足資料

- [接続情報ファイル仕様](./290_SERVER_FILE_SPEC)
- [PaperMC API仕様メモ](./291_PAPERMC_API_NOTE)
- [例外・エラーハンドリング方針](./292_ERROR_HANDLING)

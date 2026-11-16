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
java -jar wakasaba_orchestrator-1.0-SNAPSHOT-all.jar <server0_file> <server1_file> <server2_file> <server3_file> <server4_file> <server5_file>
```

各引数はサーバ接続情報ファイルのパスを示す。ファイルフォーマットについては [補足資料：接続情報ファイル仕様](./290_SERVER_FILE_SPEC)
を参照。

---

## パッケージ構成

```
com.wks
├── main
│   └── Main.java                  # エントリポイント・AplLog初期化・終了待機
├── workflow
│   └── WksWorkFlow.java           # サーバ操作シーケンス定義
├── cmd
│   └── SshCommand.java            # SSH経由コマンド実行ラッパー
├── papermc
│   └── PaperUrlGen.java           # PaperMC/Pl3xMap APIパーサ
├── util
│   ├── AplLog.java               # 単一インスタンス・ログ受付・非同期ファイル出力
│   ├── ConnectionInformation.java # サーバ接続情報Record
│   ├── SshExec.java               # JSch SSH実行エンジン
│   ├── BashExec.java              # ローカルBashコマンド実行
│   └── Curl.java                  # HTTPクライアント（API呼出し）
└── parts
    └── WksConstants.java          # 全定数定義
```

---

## クラス一覧

| クラス名                                             | パッケージ         | 種別                                 | 役割                                                     |
|------------------------------------------------------|--------------------|--------------------------------------|----------------------------------------------------------|
| [Main](./201_Main)                                   | `com.wks.main`     | class                                | エントリポイント。引数検証・ログ管理・ログファイル出力   |
| [WksWorkFlow](./202_WksWorkFlow)                     | `com.wks.workflow` | class                                | 6台のリモートサーバ＋自サーバへの処理シーケンス定義      |
| [AplLog](210_AplLog.md)                              | `com.wks.util`     | class（Thread継承）                  | ログ受付・例外整形・随時ファイル出力                     |
| `UpdateThread`                                       | `com.wks.workflow` | class（package-private、Thread継承） | WksWorkFlow.java内。サーバ1台のupdate・upgradeと例外保持 |
| [SshCommand](./203_SshCommand)                       | `com.wks.cmd`      | class                                | SSH経由コマンド実行の業務ロジックラッパー                |
| [PaperUrlGen](./204_PaperUrlGen)                     | `com.wks.papermc`  | class                                | PaperMC・Pl3xMap APIレスポンスJSONパーサ                 |
| [ConnectionInformation](./205_ConnectionInformation) | `com.wks.util`     | record                               | サーバ接続情報（host/port/user/key）の保持と読込み       |
| [SshExec](./206_SshExec)                             | `com.wks.util`     | class                                | JSch を用いたSSHセッション管理・コマンド実行             |
| [BashExec](./207_BashExec)                           | `com.wks.util`     | class                                | ProcessBuilder を用いたローカルBashコマンド実行          |
| [Curl](./208_Curl)                                   | `com.wks.util`     | class                                | Java標準HttpClient を用いたHTTP GETリクエスト            |
| [WksConstants](./209_WksConstants)                   | `com.wks.parts`    | class                                | アプリケーション全体で使用する定数の一元管理             |

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

- 初期疎通確認前は対象ci.host()のAPLでChecking Connectionを記録する。疎通不可の全体中止はlocalhostのERRでServers Not
  Activeを記録する。区切りログは廃止する。

Mainで6引数を検証し、開始ログを記録して次のワークフローを呼び出す。

```
1. setConnectionInformation(servers): 6ファイルを読み込み cis[0]～cis[5] を設定
2. isAllAlive(cis): 引数順に確認前ログを追記しSSH疎通確認
   最初の疎通不可 → Servers Not Active → 保守せずMainへ復帰
3. systemStop(cis): PaperMC停止 → Schubert停止 → 管理端末で60秒待機（1回）
4. allUMount(cis): サーバ0～4の /nas・/share を解除し、findmntの出力がないことを確認
5. allUpdate(cis): サーバ0～5を並列更新（各台 update → upgrade）
   全スレッドのjoin → 引数順の例外検出 → 全件正常なら次工程へ
6. allReboot(cis): サーバ0～5へ順に再起動指示
7. BashExec.waitOneMin(): 管理端末で60秒待機（1回）
8. allRebootComplete(cis): サーバ0～5を順に確認（各呼び出し600秒目安）
9. allMount(cis): サーバ0～4の /nas・/share を接続し、findmntの出力があることを確認
10. systemPreSetup(cis): サーバ1でバックアップ → PaperMC/Pl3xMap取得 → SHA検証・配置
    wgetPaperMcの末尾で、管理端末のpapermc_version.txtを更新
11. systemStart(cis): PaperMC起動 → Schubert起動
12. 管理端末: BashExec.update() → upgrade() → shutdown()
13. MainでSYS終了ログ追加 → endOfLogOutput → AplLog.join
例外発生: 後続工程中止 → MainでERR・SYS終了ログ追加 → 受付終了・join → 手動復旧
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

- 工程の順序は維持し、OS更新工程のみサーバ間を並列化する。共有操作・再起動・復帰確認・アプリ操作は従来どおり直列とする。
- 全機疎通確認はサービス停止前に行い、最初の失敗で中止する。管理端末のOS更新も開始しない。
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

## ログ出力仕様

| 項目             | 内容                                                                            |
|------------------|---------------------------------------------------------------------------------|
| 書式             | `[日時 サーバIP ログレベル]: 内容` ＋ LF                                        |
| 日時             | 受付時の管理端末時刻。`yyyyMMddHHmmss`、Asia/Tokyo                              |
| サーバIP欄       | SSH操作対象の `ci.host()`。ホスト名の場合はそのまま。ローカル処理は `localhost` |
| レベル           | `SYS`・`APL`・`CMD`・`OUT`・`ERR`                                               |
| CMD本文          | `$ ` ＋コマンド                                                                 |
| 出力先           | 管理端末の `/home/mini/wakasaba_orchestrator/log/log_%s.txt`                    |
| ファイル名の日時 | AplLog初回生成時の日時。終了時刻ではない                                        |
| 文字コード・更新 | `PrintStream(String)` の既定値。新規作成または上書き                            |
| 書き出し         | 受付済みログを専用スレッドで随時出力。リスト排出後は1秒休止                     |
| 終了             | 最終ログ追加→終了通知→join。残ログ排出後にclose                                 |

- ログの所有者はAplLogのみとし、各部品はgetInstance経由で追加する。Mainの公開ログリスト・終了時一括出力APIは使用しない。
- 並列更新中も同一インスタンスへ追加する。各行に接続先を付けるが、複数行を一括排他しないためサーバ間のログは混在する。
- 受付済みログの随時書き出しであり、すべてのコマンド出力の逐次取得や厳密な即時性を保証するものではない。
- 正常な終了順序は全ログ発行処理の完了→最終ログ追加→endOfLogOutput→join。終了通知後の追加は受け付けない。
- ログディレクトリの存在、作成・上書き権限と十分な容量を前提とする。親ディレクトリの作成とPrintStreamの書き込みエラーフラグ検査は行わない。

詳細は [AplLog](210_AplLog.md) を参照。

---

## 出力ファイル

| 定数                   | 管理端末上の出力先                                     | 内容・出力タイミング                                   |
|------------------------|--------------------------------------------------------|--------------------------------------------------------|
| 実行ログ               | `/home/mini/wakasaba_orchestrator/log/log_%s.txt`      | 初回生成時刻のファイルへAplLogが随時出力               |
| `PATH_PAPERMC_VERSION` | `/home/mini/wakasaba_orchestrator/papermc_version.txt` | 取得対象のバージョン文字列。`wgetPaperMc` の末尾で出力 |

両パスは `PATH_WAKASABA_EXEC`（`/home/mini/wakasaba_orchestrator/`）との定数連結で構成する。
バージョンファイルは `PrintStream(String)` の既定文字コードで、`print(version)` により末尾改行なしで新規作成または上書きする。
Pl3xMapをバージョン不一致でスキップした場合も出力するが、SHA検証・本番配置前のため本番配置・起動完了を示さない。
初期疎通不可による全体中止や出力到達前の例外発生時は更新せず、既存内容が残る。
出力先ディレクトリの存在、作成・上書き権限、十分なディスク容量を前提とし、親ディレクトリの自動作成や書き込み・クローズ時のエラーフラグ確認は行わない。

---

## PaperMCバージョン選択と例外

- 更新スレッド内の例外は保存し、全スレッドの終了待ち後に親スレッドで引数順に検出する。
- `JSchException`・`IOException`・`InterruptedException` は同じ例外を再送出し、その他の `Exception` は原因を保持した
  `RuntimeException` で再送出する。
- 親スレッドで例外を再送出した場合は、再起動以降を実行せず、従来どおりMainでログ出力して手動復旧する。
- 更新失敗時もほかの開始済みタスクはキャンセルせず、終了を待つ。複数の失敗は集約せず、引数順で最初に検出した例外を再送出する。

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

## 備考（将来対応）

- 将来対応1（ローカル大量出力時の停止リスク）：BashExecは終了待ちの後に標準出力を読み取るため、パイプが満杯になると子プロセスとJava側が相互待ちになる可能性がある。現在の運用では許容し、今回の変更では未解消とする。
- 将来対応2（SSH標準出力の逐次ログ化）：SshExecは標準出力を配列に蓄積し、コマンド終了後にSshCommandからAplLogへ渡す。実行中の進捗は記録しない現行仕様を許容し、将来対応1とあわせて見直す。
- 上記2件は解消済みではなく、明示的に保留した事項である。

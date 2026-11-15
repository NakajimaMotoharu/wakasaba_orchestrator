# wakasaba_orchestrator 基本設計書：WksWorkFlow

## 基本情報

| 項目             | 内容                                                                                      |
|------------------|-------------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                                            |
| ファイル名       | `WksWorkFlow.java`                                                                        |
| 種別             | class（public）                                                                           |
| 責務             | リモート6台と管理端末の工程単位メンテナンスを定義・実行する                               |
| 主な依存クラス   | `Main`、`SshCommand`、`BashExec`、`ConnectionInformation`、`WksConstants`、`UpdateThread` |

---

## フィールド一覧

| フィールド名 | 型             | 修飾子                 | 初期値     | 説明                                         |
|--------------|----------------|------------------------|------------|----------------------------------------------|
| `log`        | `List<String>` | `private static final` | `Main.log` | 初期疎通確認対象・警告・管理端末区切りの追記 |

---

## メソッド一覧

| メソッド名                                       | 戻り値型                  | 修飾子           | 処理                                        |
|--------------------------------------------------|---------------------------|------------------|---------------------------------------------|
| `execScheduledJob(String[] servers)`             | `void`                    | `public static`  | 6接続ファイルで全体の工程を実行             |
| `setConnectionInformation(String[] servers)`     | `ConnectionInformation[]` | `private static` | 全ファイルを引数順に読込                    |
| `isAllAlive(ConnectionInformation[] cis)`        | `boolean`                 | `private static` | 各対象をログへ記録し、最初の疎通不可でfalse |
| `systemStop(ConnectionInformation[] cis)`        | `void`                    | `private static` | 両サービス停止後に共通60秒待機              |
| `allUMount(ConnectionInformation[] cis)`         | `void`                    | `private static` | NASを除き共有解除・状態確認                 |
| `allUpdate(ConnectionInformation[] cis)`         | `void`                    | `private static` | 全6台の並列更新・全join・例外検出           |
| `allReboot(ConnectionInformation[] cis)`         | `void`                    | `private static` | 全6台へ再起動指示                           |
| `allRebootComplete(ConnectionInformation[] cis)` | `void`                    | `private static` | 各台600秒目安のSSH復帰待ち                  |
| `allMount(ConnectionInformation[] cis)`          | `void`                    | `private static` | NASを除き共有接続・状態確認                 |
| `systemPreSetup(ConnectionInformation[] cis)`    | `void`                    | `private static` | PaperMCバックアップ・取得・配置             |
| `systemStart(ConnectionInformation[] cis)`       | `void`                    | `private static` | PaperMC・Schubert起動                       |

---

## 処理フロー

### `execScheduledJob(String[] servers)`

```
1. setConnectionInformation(servers): 6ファイルを読み込み cis[0]～cis[5] を設定
2. isAllAlive(cis): 引数順に確認前ログを追記しSSH疎通確認
   最初の疎通不可 → WARNING: Servers Not Active → 保守せずMainへ復帰
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
12. 管理端末: 区切りログ → BashExec.update() → upgrade() → shutdown()
13. Mainで終了時刻記録・ログファイル出力
例外発生: 後続工程中止 → Mainでスタックトレース・終了ログ出力 → 手動復旧
```

### `allUpdate(ConnectionInformation[] cis)`

```
1. UpdateThread[cis.length] を生成
2. 各cis[i]でUpdateThreadを生成しstart()（このループ内ではjoinしない）
3. 全スレッドに対して順にjoin()
4. 全スレッドに対して引数順にexceptionDetection()
5. 例外がなければ呼び出し元へ復帰し、全台再起動工程へ進む
```

- 更新スレッド内の例外は保存し、全スレッドの終了待ち後に親スレッドで引数順に検出する。
- `JSchException`・`IOException`・`InterruptedException` は同じ例外を再送出し、その他の `Exception` は原因を保持した
  `RuntimeException` で再送出する。
- 親スレッドで例外を再送出した場合は、再起動以降を実行せず、従来どおりMainでログ出力して手動復旧する。
- 更新失敗時もほかの開始済みタスクはキャンセルせず、終了を待つ。複数の失敗は集約せず、引数順で最初に検出した例外を再送出する。

### 各対象の役割と処理内容

| インデックス | 定数              | 役割             | 保守対象                                                           |
|--------------|-------------------|------------------|--------------------------------------------------------------------|
| 0            | `SERVER_TEST`     | テスト           | OS更新・再起動・共有解除と復旧                                     |
| 1            | `SERVER_PAPER`    | PaperMC          | OS更新・再起動・共有解除と復旧・PaperMC停止/バックアップ/更新/起動 |
| 2            | `SERVER_LLM`      | ローカルLLM      | OS更新・再起動・共有解除と復旧                                     |
| 3            | `SERVER_SCHUBERT` | Schubert         | OS更新・再起動・共有解除と復旧・Schubert停止/起動                  |
| 4            | `SERVER_VPN`      | VPN              | OS更新・再起動・共有解除と復旧                                     |
| 5            | `SERVER_NAS`      | NAS              | OS更新・再起動。自身への共有操作は行わない                         |
| 管理端末     | -                 | 本バッチ実行端末 | 固定待機・時刻取得、最後にOS更新・60秒遅延再起動                   |

---

## 同一ソース内のクラス：UpdateThread

| 項目             | 内容                                                          |
|------------------|---------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.UpdateThread`                               |
| 定義ファイル     | `WksWorkFlow.java`                                            |
| 種別             | トップレベルclass（package-private）。`Thread` を継承         |
| 責務             | サーバ1台のOS更新を実行し、例外を親スレッドによる検出まで保持 |
| 依存クラス       | `ConnectionInformation`、`SshCommand`                         |

### メソッド一覧

| メソッド                                 | 戻り値型 | 修飾子             | 処理                                   |
|------------------------------------------|----------|--------------------|----------------------------------------|
| `UpdateThread(ConnectionInformation ci)` | -        | `public`           | 対象接続情報を保持                     |
| `run()`                                  | `void`   | `public`、Override | update→upgrade。例外を保持して終了     |
| `exceptionDetection()`                   | `void`   | `public`           | 保持した例外があれば親スレッドで再送出 |

### 処理仕様

- `run` はupdate後にupgradeを実行する。updateが例外となれば、そのタスクのupgradeは実行しない。
- `JSchException`・`IOException`・`InterruptedException` は対応フィールドへ保持し、その他の `Exception` も保持する。
- `exceptionDetection` は上記3検査例外を同じインスタンスのまま再送出し、その他は `RuntimeException(exception)`
  として原因付きで再送出する。
- 保持例外がなければ何もせず復帰する。複数タスクの例外を集約する処理はない。
- `Error` は捕捉しない。`join()` 自体が割り込まれた場合は親の `InterruptedException` を上位へ委譲し、子のキャンセルは行わない。

---

## 例外

| 例外クラス                   | 発生条件                                                                                          |
|------------------------------|---------------------------------------------------------------------------------------------------|
| `IOException`                | 接続ファイル読込、SSH標準出力取得、Bash実行・時刻取得、HTTP通信、バージョンファイルのオープン失敗 |
| `InterruptedException`       | SSH・Bash・ポーリング待機中の割り込み                                                             |
| `JSchException`              | セッション生成・SSH接続・コマンド実行失敗                                                         |
| `RuntimeException`（非検査） | マウント状態不一致、SSH復帰待ち超過、PaperMC正式版未検出                                          |

更新タスク内の例外は `UpdateThread` で一時保持し、全join後に親で再送出する。
検査例外・非検査例外とも `Main.main` へ伝播する。初期疎通不可は例外ではなく警告ログを残して復帰する。

---

## 設計上の注意点

- 共有ログの宣言型は `List<String>`、実体は `Main` が生成する `CopyOnWriteArrayList<String>` とする。各クラスは同一インスタンスを参照する。
- 並列更新中の `add`・`addAll` は安全に実行できるが、コマンド行と標準出力を一体で排他しないため、サーバ間のログは混在する。サーバ別の順序・まとまりは保証しない。

- 工程の順序は維持し、OS更新工程のみサーバ間を並列化する。共有操作・再起動・復帰確認・アプリ操作は従来どおり直列とする。
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

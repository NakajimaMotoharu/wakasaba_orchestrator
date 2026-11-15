# クラス詳細設計：WksWorkFlow

## 基本情報

| 項目             | 内容                                                                      |
|------------------|---------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`                                            |
| ファイル名       | `WksWorkFlow.java`                                                        |
| 種別             | class（public）                                                           |
| 責務             | リモート6台と管理端末の工程単位メンテナンスを定義・実行する               |
| 依存クラス       | `Main`、`SshCommand`、`BashExec`、`ConnectionInformation`、`WksConstants` |

---

## フィールド一覧

| フィールド名 | 型                  | 修飾子                 | 初期値     | 説明                                         |
|--------------|---------------------|------------------------|------------|----------------------------------------------|
| `log`        | `ArrayList<String>` | `private static final` | `Main.log` | 初期疎通確認対象・警告・管理端末区切りの追記 |

---

## メソッド一覧

| メソッド名                                       | 戻り値型                  | 修飾子           | 処理                                        |
|--------------------------------------------------|---------------------------|------------------|---------------------------------------------|
| `execScheduledJob(String[] servers)`             | `void`                    | `public static`  | 6接続ファイルで全体の工程を実行             |
| `setConnectionInformation(String[] servers)`     | `ConnectionInformation[]` | `private static` | 全ファイルを引数順に読込                    |
| `isAllAlive(ConnectionInformation[] cis)`        | `boolean`                 | `private static` | 各対象をログへ記録し、最初の疎通不可でfalse |
| `systemStop(ConnectionInformation[] cis)`        | `void`                    | `private static` | 両サービス停止後に共通60秒待機              |
| `allUMount(ConnectionInformation[] cis)`         | `void`                    | `private static` | NASを除き共有解除・状態確認                 |
| `allUpdate(ConnectionInformation[] cis)`         | `void`                    | `private static` | 全6台の更新                                 |
| `allReboot(ConnectionInformation[] cis)`         | `void`                    | `private static` | 全6台へ再起動指示                           |
| `allRebootComplete(ConnectionInformation[] cis)` | `void`                    | `private static` | 各台600秒目安のSSH復帰待ち                  |
| `allMount(ConnectionInformation[] cis)`          | `void`                    | `private static` | NASを除き共有接続・状態確認                 |
| `systemPreSetup(ConnectionInformation[] cis)`    | `void`                    | `private static` | PaperMCバックアップ・取得・配置             |
| `systemStart(ConnectionInformation[] cis)`       | `void`                    | `private static` | PaperMC・Schubert起動                       |

---

## メソッド詳細

### `execScheduledJob(String[] servers)`

```java
public static void execScheduledJob(String[] servers)
        throws IOException, InterruptedException, JSchException;
```

引数は6つの接続情報ファイルパス。読み込み後の配列添字とサーバ定数を一致させる。
各工程の対象ループは直列であり、全台が工程を完了してから次工程へ進む。

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

### ヘルパーメソッドの詳細

- `setConnectionInformation` は `new ConnectionInformation[servers.length]` を生成し、各ファイルを `getCiFromFile` で読み込む（
  `IOException`）。
- `isAllAlive` は `OTHER_CONNECTION_CHECK` に接続情報を埋めてログ追記後、`SshCommand.isAlive`
  を実行する。falseなら即false、全件成功ならtrue（`JSchException`）。
- `allMount`・`allUMount` は添字が `SERVER_NAS` でない場合のみそれぞれ `mount`・`umount` を呼ぶ。
- `allUpdate` は各台で `update` → `upgrade`、`allReboot` は各台で `shutdown` を呼ぶ。
- `allRebootComplete` は各台で `waitForBecomeActive(ci, SERVER_REBOOT_WAIT_LIMIT)` を呼ぶ。
- `systemStop` は `SERVER_PAPER`、`SERVER_SCHUBERT` の順に停止し、`BashExec.waitOneMin()` を呼ぶ。
- `systemPreSetup` は `SERVER_PAPER` に `backupPaperMC` → `wgetPaperMc` → `movePaperMc` を呼ぶ。
- `systemStart` は `startPaperMC` → `startSchubert` を呼ぶ。
- 上記のコマンド実行系ヘルパーは `JSchException`・`IOException`・`InterruptedException` を上位へ委譲する。

---

## 例外

| 例外クラス                   | 発生条件                                                                                          |
|------------------------------|---------------------------------------------------------------------------------------------------|
| `IOException`                | 接続ファイル読込、SSH標準出力取得、Bash実行・時刻取得、HTTP通信、バージョンファイルのオープン失敗 |
| `InterruptedException`       | SSH・Bash・ポーリング待機中の割り込み                                                             |
| `JSchException`              | セッション生成・SSH接続・コマンド実行失敗                                                         |
| `RuntimeException`（非検査） | マウント状態不一致、SSH復帰待ち超過、PaperMC正式版未検出                                          |

検査例外・非検査例外とも `Main.main` へ伝播する。初期疎通不可は例外ではなく警告ログを残して復帰する。

---

## 設計上の注意点

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

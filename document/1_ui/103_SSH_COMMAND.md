# wakasaba_orchestrator 基本設計書：SshCommand

## 基本情報

| 項目             | 内容                                                                                              |
|------------------|---------------------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.cmd.SshCommand`                                                                          |
| ファイル名       | `SshCommand.java`                                                                                 |
| 種別             | class（public）                                                                                   |
| 責務             | SSH経由で実行する各種コマンドの業務ロジックラッパー。コマンドの組み立て・実行・ログ記録を集約する |
| 主な依存クラス   | `Main`、`SshExec`、`ConnectionInformation`、`Curl`、`PaperUrlGen`、`WksConstants`、`BashExec`     |

---

## フィールド一覧

| フィールド名 | 型             | 修飾子                 | 説明                                                                      |
|--------------|----------------|------------------------|---------------------------------------------------------------------------|
| `log`        | `List<String>` | `private static final` | `Main.log` への参照。実行コマンドと標準出力をログに追記するために使用する |

---

## メソッド一覧

| メソッド名                       | 戻り値型   | 修飾子           | 説明                                                                         |
|----------------------------------|------------|------------------|------------------------------------------------------------------------------|
| `update`                         | `void`     | `public static`  | `sudo apt update` を実行する                                                 |
| `upgrade`                        | `void`     | `public static`  | `sudo apt upgrade -y` を実行する                                             |
| `shutdown`                       | `void`     | `public static`  | `sudo shutdown -r now` を実行する                                            |
| `stopPaperMC`                    | `void`     | `public static`  | `sudo systemctl stop papermc` を実行する                                     |
| `startSchubert`                  | `void`     | `public static`  | Schubert起動シェルを実行する                                                 |
| `stopSchubert`                   | `void`     | `public static`  | Schubert停止シェルを実行する                                                 |
| `startPaperMC`                   | `void`     | `public static`  | `sudo systemctl start papermc` を実行する                                    |
| `backupPaperMC`                  | `void`     | `public static`  | バックアップシェルスクリプトを実行する                                       |
| `wgetPaperMc`                    | `void`     | `public static`  | PaperMC・Pl3xMap JARをダウンロードし、管理端末へバージョンファイルを出力する |
| `movePaperMc`                    | `void`     | `public static`  | ダウンロード済み JAR を SHA 検証後に本番ディレクトリへ移動する               |
| `isAlive`                        | `boolean`  | `public static`  | 対象サーバへ SSH 接続可能か判定する                                          |
| `mount`                          | `void`     | `public static`  | /nas・/share接続と状態確認                                                   |
| `umount`                         | `void`     | `public static`  | /nas・/share解除と状態確認                                                   |
| `runCommand`                     | `String[]` | `private static` | コマンド実行・ログ記録の共通処理                                             |
| `waitForBecomeActive`            | `void`     | `public static`  | 対象サーバが SSH 応答可能になるまでポーリング待機する                        |
| `waitForBecomeActive(ci, limit)` | `void`     | `public static`  | 上限秒数指定の復帰待ち                                                       |

---

## 処理フロー

### `update` / `upgrade` / `shutdown` / `stopPaperMC` / `startPaperMC` / `startSchubert` /

`stopSchubert` / `backupPaperMC`

各メソッドは対応する定数コマンドを引数として `runCommand(ci, コマンド定数)` を呼び出す単純なラッパー。

| メソッド名      | 実行コマンド（定数名） |
|-----------------|------------------------|
| `update`        | `CMD_UPDATE`           |
| `upgrade`       | `CMD_UPGRADE`          |
| `shutdown`      | `CMD_SHUTDOWN`         |
| `stopPaperMC`   | `CMD_PAPERMC_END`      |
| `startSchubert` | `CMD_SCHUBERT_START`   |
| `stopSchubert`  | `CMD_SCHUBERT_END`     |
| `startPaperMC`  | `CMD_PAPERMC_START`    |
| `backupPaperMC` | `CMD_PAPERMC_BACKUP`   |

---

### `wgetPaperMc(ConnectionInformation ci)`

PaperMCの最新正式版および対応するPl3xMapのJARをリモートサーバ上にダウンロードし、管理端末へPaperMCバージョンファイルを出力する。

```
1. userAgent = OTHER_USER_AGENT
2. Curl.exec(userAgent, URL_PAPERMC_VERSION) → versionJson
3. PaperUrlGen.getPaperMcVersion(versionJson) → version（pre・rc等を除外した最新正式版）
4. Curl.exec(userAgent, URL_PAPERMC_DL_URL % version) → urlJson
5. PaperUrlGen.getPaperMcUrl(urlJson) → url
6. cmd = CMD_WGET_PAPERMC % (userAgent, url) を組み立て
7. runCommand(ci, cmd) → リモートサーバ上で wget 実行、PATH_DL_PAPERMC に保存

8. Curl.exec(userAgent, URL_PL3XMAP_DL_URL) → pl3xMapUrlJson
9. PaperUrlGen.getPl3xMapUrl(pl3xMapUrlJson, version) → pl3xMapUrl
   ├─ null の場合: Pl3xMap のダウンロードをスキップ
   └─ null でない場合:
      10. cmd = CMD_WGET_PL3XMAP % (userAgent, pl3xMapUrl) を組み立て
      11. runCommand(ci, cmd) → リモートサーバ上で wget 実行、PATH_DL_PL3XMAP に保存

12. 管理端末上で new PrintStream(PATH_PAPERMC_VERSION) → ps
13. ps.print(version) → バージョン文字列を上書き（末尾改行なし）
14. ps.close()
```

出力先は `/home/mini/wakasaba_orchestrator/papermc_version.txt`。Pl3xMapのダウンロードをスキップした場合も出力する。
文字コードは `PrintStream(String)` の既定値を使用する。出力はSHA検証・本番配置前であり、内容はダウンロード対象のバージョンを示す。

---

### `movePaperMc(ConnectionInformation ci)`

ダウンロード済みの JAR ファイルを SHA チェックサムで検証し、正常な場合のみ本番ディレクトリへ移動する。

```
[事前情報取得]
1. Curl.exec → versionJson → PaperUrlGen.getPaperMcVersion → version（最新正式版）
2. Curl.exec → sha256Json → expectedSHA256（PaperMC の SHA256 期待値）
3. Curl.exec → sha512Json → expectedSHA512（Pl3xMap の SHA512 期待値。バージョン不一致時 null）

[PaperMC 検証・配置]
4. waitForBecomeActive(ci)
5. SshExec(ci, CMD_PAPERMC_HASH).execute() → ret（sha256sum 出力）
6. log にコマンドと結果を追記
7. ret[0].substring(0, 64) == expectedSHA256 ?
   Yes:
     8.  waitForBecomeActive(ci)
     9.  SshExec(ci, CMD_PAPERMC_RM).execute()  → 本番 JAR を削除
     10. log に追記
     11. waitForBecomeActive(ci)
     12. SshExec(ci, CMD_PAPERMC_MV).execute()  → DL ファイルを本番へ移動
     13. log に追記
   No: スキップ（ハッシュ不一致のため移動しない）

[Pl3xMap 旧ファイル削除（常に実行）]
14. waitForBecomeActive(ci)
15. SshExec(ci, CMD_PL3XMAP_RM).execute()  → 本番プラグイン JAR を削除
16. log に追記

[Pl3xMap 検証・配置（expectedSHA512 != null の場合のみ）]
17. waitForBecomeActive(ci)
18. SshExec(ci, CMD_PL3XMAP_HASH).execute() → ret（sha512sum 出力）
19. log に追記
20. ret[0].substring(0, 128) == expectedSHA512 ?
    Yes:
      21. waitForBecomeActive(ci)
      22. SshExec(ci, CMD_PL3XMAP_MV).execute() → DL ファイルを本番へ移動
      23. log に追記
    No: スキップ
```

#### SHA 検証仕様

| 対象        | アルゴリズム | 期待値取得元                      | 比較文字数    |
|-------------|--------------|-----------------------------------|---------------|
| PaperMC JAR | SHA-256      | PaperMC API（`checksums.sha256`） | 先頭 64 文字  |
| Pl3xMap JAR | SHA-512      | Modrinth API（`hashes.sha512`）   | 先頭 128 文字 |

---

### `runCommand(ConnectionInformation ci, String cmd)` ※private

```
1. waitForBecomeActive(ci) → サーバが応答するまで待機
2. new SshExec(ci, cmd)
3. sshExec.execute() → ret（標準出力の行配列）
4. log に "$ {cmd}" を追記
5. log に ret の全要素を追記
6. ret（String[]）を呼び出し元へ返却
```

---

### `waitForBecomeActive(ConnectionInformation ci, int limit)`

`public static void`。検査例外は `JSchException`・`InterruptedException`・`IOException`。
`limit` は秒単位で、各呼び出しの開始時刻を `BashExec.getNowTime()` で取得する。

```
1. startUnixTime = BashExec.getNowTime()
2. SshExec(ci, CMD_DO_NOTHING) を生成
3. while (!sshExec.isAlive()):
     startUnixTime + limit < BashExec.getNowTime() なら
       RuntimeException(OTHER_REBOOT_EXCEPTION % ci.toString())
     Thread.sleep(1000)
4. 接続成功で復帰
```

再起動後の明示待機では `SERVER_REBOOT_WAIT_LIMIT = 600` 秒を渡す。
各接続確認は `session.connect(60000)` を使用するが、全体の600秒は厳密な締切ではない。
失敗した接続試行後に期限超過を確認し、最後の試行で接続成功した場合は期限後でも後続へ進む。

### `waitForBecomeActive(ConnectionInformation ci)`

`public static void`。同じ3種類の検査例外を上位へ委譲する。
`waitForBecomeActive(ci, Integer.MAX_VALUE)` へ委譲する。
通常のコマンド実行前および `movePaperMc` から利用する。600秒を適用するのはワークフローの再起動後明示待機である。

---

### `isAlive(ConnectionInformation ci)`

```
1. new SshExec(ci, CMD_DO_NOTHING)
2. sshExec.isAlive() の結果を返却
```

処理開始時点で対象サーバへSSH接続可能か判定する。コマンド実行および標準出力ログの追加は行わない。

---

### `mount(ConnectionInformation ci)` / `umount(ConnectionInformation ci)`

両方とも `public static void`。
`JSchException`・`InterruptedException`・`IOException` を上位へ委譲する。

| 工程 | mount                                 | umount                              |
|------|---------------------------------------|-------------------------------------|
| 1    | `runCommand(ci, CMD_MOUNT_NAS)`       | `runCommand(ci, CMD_UMOUNT_NAS)`    |
| 2    | `runCommand(ci, CMD_MOUNT_SHARE)`     | `runCommand(ci, CMD_UMOUNT_SHARE)`  |
| 3    | `findmnt /nas` に出力がなければ例外   | `findmnt /nas` に出力があれば例外   |
| 4    | `findmnt /share` に出力がなければ例外 | `findmnt /share` に出力があれば例外 |

状態不一致は `RuntimeException(OTHER_MOUNT_STATUS_EXCEPTION)` とする。
使用中で解除できない場合も、確認コマンドが正常なら残存マウントを検出して後続を止める。
アンマウントのリトライ、終了コード取得、共有元の期待値照合は追加しない。
NAS自身を除外する責務は `WksWorkFlow` にあり、ここでは指定された接続先のみ操作する。

---

## 例外

| 例外クラス                   | 発生条件                                                                                          |
|------------------------------|---------------------------------------------------------------------------------------------------|
| `JSchException`              | SSH 接続・チャンネル接続失敗                                                                      |
| `InterruptedException`       | SSH コマンド実行中の割り込み・ポーリングスリープ中の割り込み                                      |
| `IOException`                | SSH標準出力の読み取り失敗・HTTP通信失敗・バージョンファイルのオープン失敗・管理端末の時刻取得失敗 |
| `RuntimeException`（非検査） | PaperMC正式版が全系列に見つからない場合・マウント状態不一致・復帰待ち超過                         |

---

## 設計上の注意点

- OS更新工程では、異なる接続先の `update`・`upgrade` が複数の `UpdateThread` から同時に呼ばれる。各台のコマンド順は維持する。

- 共有ログの宣言型は `List<String>`、実体は `Main` が生成する `CopyOnWriteArrayList<String>` とする。各クラスは同一インスタンスを参照する。
- 並列更新中の `add`・`addAll` は安全に実行できるが、コマンド行と標準出力を一体で排他しないため、サーバ間のログは混在する。サーバ別の順序・まとまりは保証しない。

- バージョンファイルの親ディレクトリの存在と実行ユーザの作成・上書き権限、十分なディスク容量を前提とする。親ディレクトリの自動作成や
  `PrintStream` のエラーフラグ確認は行わない。
- 対象サーバの初期疎通不可による全体中止や、ファイル出力到達前の例外発生時はファイルを更新しない。既存ファイルがあれば以前の内容が残る。
- `runCommand` はコマンド実行前に必ず `waitForBecomeActive` を呼ぶため、再起動中のサーバへも自動で待機・リトライが行われる。
- `isAlive` は `WksWorkFlow` の処理開始時疎通確認で使用し、全体の保守開始可否を判断するための判定値を返す。
- `movePaperMc` は `runCommand` を経由せず、`SshExec` を直接操作してコマンドを実行する。SHA検証の結果によってコマンドの実行有無を制御する必要があるため、共通処理である
  `runCommand` には委譲できない設計となっている。
- `movePaperMc` における Pl3xMap の旧ファイル削除（`CMD_PL3XMAP_RM`）は、新バージョンのダウンロード有無に関わらず**常に実行
  **される。
- `wgetPaperMc` と `movePaperMc` の両方で独立して API を呼び出してバージョン情報を取得する。

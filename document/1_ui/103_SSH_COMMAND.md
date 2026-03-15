# wakasaba_orchestrator 基本設計書：SshCommand

## 基本情報

| 項目       | 内容                                                                           |
|----------|------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.cmd.SshCommand`                                                     |
| ファイル名    | `SshCommand.java`                                                            |
| 種別       | class（public）                                                                |
| 責務       | SSH経由で実行する各種コマンドの業務ロジックラッパー。コマンドの組み立て・実行・ログ記録を集約する                           |
| 主な依存クラス  | `Main`、`SshExec`、`ConnectionInformation`、`Curl`、`PaperUrlGen`、`WksConstants` |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                    | 説明                                         |
|--------|---------------------|------------------------|--------------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` への参照。実行コマンドと標準出力をログに追記するために使用する |

---

## メソッド一覧

| メソッド名                 | 戻り値型   | 修飾子              | 説明                                          |
|-----------------------|--------|------------------|---------------------------------------------|
| `update`              | `void` | `public static`  | `sudo apt update` を実行する                     |
| `upgrade`             | `void` | `public static`  | `sudo apt upgrade -y` を実行する                 |
| `shutdown`            | `void` | `public static`  | `sudo shutdown -r now` を実行する                |
| `stopPaperMC`         | `void` | `public static`  | `sudo systemctl stop papermc` を実行する         |
| `startPaperMC`        | `void` | `public static`  | `sudo systemctl start papermc` を実行する        |
| `backupPaperMC`       | `void` | `public static`  | バックアップシェルスクリプトを実行する                         |
| `wgetPaperMc`         | `void` | `public static`  | PaperMC・Pl3xMap の最新版 JAR をリモートサーバ上にダウンロードする |
| `movePaperMc`         | `void` | `public static`  | ダウンロード済み JAR を SHA 検証後に本番ディレクトリへ移動する        |
| `runCommand`          | `void` | `private static` | コマンド実行・ログ記録の共通処理                            |
| `waitForBecomeActive` | `void` | `private static` | 対象サーバが SSH 応答可能になるまでポーリング待機する               |

---

## 処理フロー

### `update` / `upgrade` / `shutdown` / `stopPaperMC` / `startPaperMC` / `backupPaperMC`

各メソッドは対応する定数コマンドを引数として `runCommand(ci, コマンド定数)` を呼び出す単純なラッパー。

| メソッド名           | 実行コマンド（定数名）          |
|-----------------|----------------------|
| `update`        | `CMD_UPDATE`         |
| `upgrade`       | `CMD_UPGRADE`        |
| `shutdown`      | `CMD_SHUTDOWN`       |
| `stopPaperMC`   | `CMD_PAPERMC_END`    |
| `startPaperMC`  | `CMD_PAPERMC_START`  |
| `backupPaperMC` | `CMD_PAPERMC_BACKUP` |

---

### `wgetPaperMc(ConnectionInformation ci)`

PaperMC および Pl3xMap の最新版 JAR ファイルをリモートサーバ上にダウンロードする。

```
1. userAgent = OTHER_USER_AGENT
2. Curl.exec(userAgent, URL_PAPERMC_VERSION) → versionJson
3. PaperUrlGen.getPaperMcVersion(versionJson) → version（例: "1.21.4"）
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
```

---

### `movePaperMc(ConnectionInformation ci)`

ダウンロード済みの JAR ファイルを SHA チェックサムで検証し、正常な場合のみ本番ディレクトリへ移動する。

```
[事前情報取得]
1. Curl.exec → versionJson → version
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

| 対象          | アルゴリズム  | 期待値取得元                          | 比較文字数     |
|-------------|---------|---------------------------------|-----------|
| PaperMC JAR | SHA-256 | PaperMC API（`checksums.sha256`） | 先頭 64 文字  |
| Pl3xMap JAR | SHA-512 | Modrinth API（`hashes.sha512`）   | 先頭 128 文字 |

---

### `runCommand(ConnectionInformation ci, String cmd)` ※private

```
1. waitForBecomeActive(ci) → サーバが応答するまで待機
2. new SshExec(ci, cmd)
3. sshExec.execute() → ret（標準出力の行配列）
4. log に "$ {cmd}" を追記
5. log に ret の全要素を追記
```

---

### `waitForBecomeActive(ConnectionInformation ci)` ※private

```
1. new SshExec(ci, CMD_DO_NOTHING)
2. while (!sshExec.isAlive()):
     Thread.sleep(1000)
3. isAlive() が true になったらリターン
```

---

## 例外

| 例外クラス                  | 発生条件                             |
|------------------------|----------------------------------|
| `JSchException`        | SSH 接続・チャンネル接続失敗                 |
| `InterruptedException` | SSH コマンド実行中の割り込み・ポーリングスリープ中の割り込み |
| `IOException`          | SSH 標準出力の読み取り失敗・HTTP 通信失敗        |

---

## 設計上の注意点

- `runCommand` はコマンド実行前に必ず `waitForBecomeActive` を呼ぶため、再起動中のサーバへも自動で待機・リトライが行われる。
- `movePaperMc` における Pl3xMap の旧ファイル削除（`CMD_PL3XMAP_RM`）は、新バージョンのダウンロード有無に関わらず**常に実行
  **される。
- `wgetPaperMc` と `movePaperMc` の両方で独立して API を呼び出してバージョン情報を取得する。

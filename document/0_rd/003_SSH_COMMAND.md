# wakasaba_orchestrator 要件定義書：SshCommand

## 基本情報

| 項目       | 内容                                                                           |
|----------|------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.cmd.SshCommand`                                                     |
| ファイル名    | `SshCommand.java`                                                            |
| 種別       | class（public）                                                                |
| 責務       | SSH経由で実行する各種コマンドの業務ロジックラッパー。コマンドの組み立て・実行・ログ記録を集約する                           |
| 主な依存クラス  | `Main`、`SshExec`、`ConnectionInformation`、`Curl`、`PaperUrlGen`、`WksConstants` |

---

## 要求機能

### SC-01：OSパッケージ更新コマンドの実行

- `sudo apt update` を対象サーバ上で実行できること。
- 実行コマンドおよび標準出力をグローバルログへ記録すること。

### SC-02：OSパッケージアップグレードコマンドの実行

- `sudo apt upgrade -y` を対象サーバ上で実行できること。
- 実行コマンドおよび標準出力をグローバルログへ記録すること。

### SC-03：サーバ再起動コマンドの実行

- `sudo shutdown -r now` を対象サーバ上で実行できること。
- 実行コマンドおよび標準出力をグローバルログへ記録すること。

### SC-04：PaperMCサービス停止コマンドの実行

- `sudo systemctl stop papermc` を対象サーバ上で実行できること。
- 実行コマンドおよび標準出力をグローバルログへ記録すること。

### SC-05：PaperMCサービス起動コマンドの実行

- `sudo systemctl start papermc` を対象サーバ上で実行できること。
- 実行コマンドおよび標準出力をグローバルログへ記録すること。

### SC-06：バックアップシェルスクリプトの実行

- バックアップシェルスクリプト（`sh /home/mini/mcs/shell/backup.sh`）を対象サーバ上で実行できること。
- 実行コマンドおよび標準出力をグローバルログへ記録すること。

### SC-07：PaperMC/Pl3xMap最新版のダウンロード

- PaperMC公式APIを呼び出し、最新バージョン文字列を取得できること。
- 取得したバージョン文字列でビルド情報APIを呼び出し、最新ビルドのダウンロードURLを取得できること。
- 取得したダウンロードURLを埋め込んだwgetコマンドをリモートサーバ上で実行し、PaperMC JARを所定のダウンロードディレクトリへ保存できること。
- Modrinth APIを呼び出し、Pl3xMapの最新ダウンロードURLを取得できること。
- 取得したPl3xMapダウンロードURLが `null` でない場合に限り、wgetコマンドをリモートサーバ上で実行し、Pl3xMap
  JARを所定のダウンロードディレクトリへ保存できること。
- Pl3xMapのバージョンがPaperMCのバージョンと一致しない場合（ダウンロードURLが `null` の場合）、Pl3xMapのダウンロードをスキップすること。

### SC-08：JARファイルのSHA検証・本番配置

- ダウンロード済みPaperMC JARのSHA-256チェックサムをリモートサーバ上で計算し、PaperMC APIから取得した期待値と比較できること。
- SHA-256が一致する場合、本番ディレクトリの旧PaperMC JARを削除し、ダウンロードファイルを本番ディレクトリへ移動できること。
- SHA-256が一致しない場合、PaperMC JARのファイル移動をスキップすること。
- Pl3xMapの旧JARファイルは、バージョン一致・SHA検証の結果にかかわらず本番プラグインディレクトリから削除すること。
- Pl3xMapダウンロードURLが `null` でない場合（バージョン一致の場合）、ダウンロード済みPl3xMap
  JARのSHA-512チェックサムをリモートサーバ上で計算し、Modrinth APIから取得した期待値（`expectedSHA512`）と比較できること。
- `expectedSHA512` が `null` でない場合（バージョン一致の場合）にのみSHA-512検証を実施し、一致する場合、ダウンロードファイルを本番プラグインディレクトリへ移動できること。
- `expectedSHA512` が `null` の場合（バージョン不一致の場合）、SHA-512検証・ファイル移動の両方をスキップすること。

#### SHA検証仕様

| 対象          | アルゴリズム  | 期待値取得元                          | 比較文字数   |
|-------------|---------|---------------------------------|---------|
| PaperMC JAR | SHA-256 | PaperMC API（`checksums.sha256`） | 先頭64文字  |
| Pl3xMap JAR | SHA-512 | Modrinth API（`hashes.sha512`）   | 先頭128文字 |

### SC-09：コマンド実行前のサーバ疎通確認（共通処理）

- すべてのSSHコマンド実行前に、対象サーバへのSSH接続が可能な状態になるまで待機すること。
- 待機はポーリング（1秒間隔での接続試行）によって実現すること。
- これにより、サーバ再起動後の後続コマンドも自動的に正常実行できること。

---

## 要求インタフェース

### フィールド

| フィールド名 | 型                   | 修飾子                    | 初期値        | 要件                                  |
|--------|---------------------|------------------------|------------|-------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` | `Main.log` への参照。コマンドと標準出力のログ記録に使用する |

### メソッド

| メソッド名                 | 戻り値型   | 修飾子              | 要件概要                                             |
|-----------------------|--------|------------------|--------------------------------------------------|
| `update`              | `void` | `public static`  | `CMD_UPDATE` を引数に `runCommand` を呼び出すラッパー         |
| `upgrade`             | `void` | `public static`  | `CMD_UPGRADE` を引数に `runCommand` を呼び出すラッパー        |
| `shutdown`            | `void` | `public static`  | `CMD_SHUTDOWN` を引数に `runCommand` を呼び出すラッパー       |
| `stopPaperMC`         | `void` | `public static`  | `CMD_PAPERMC_END` を引数に `runCommand` を呼び出すラッパー    |
| `startPaperMC`        | `void` | `public static`  | `CMD_PAPERMC_START` を引数に `runCommand` を呼び出すラッパー  |
| `backupPaperMC`       | `void` | `public static`  | `CMD_PAPERMC_BACKUP` を引数に `runCommand` を呼び出すラッパー |
| `wgetPaperMc`         | `void` | `public static`  | PaperMC/Pl3xMap JARをリモートサーバへダウンロードする             |
| `movePaperMc`         | `void` | `public static`  | SHA検証後に本番ディレクトリへJARを移動する                         |
| `runCommand`          | `void` | `private static` | 疎通確認後にコマンドを実行しログを記録する共通処理                        |
| `waitForBecomeActive` | `void` | `private static` | SSH接続可能になるまでポーリング待機する                            |

---

## 例外要件

| 例外クラス                  | 想定発生状況                          |
|------------------------|---------------------------------|
| `JSchException`        | SSH接続・チャンネル接続失敗                 |
| `InterruptedException` | SSHコマンド実行中の割り込み・ポーリングスリープ中の割り込み |
| `IOException`          | SSH標準出力の読み取り失敗・HTTP通信失敗         |

---

## 制約・注意事項

- `runCommand` はコマンド実行前に必ず `waitForBecomeActive` を呼ぶため、再起動中のサーバへも自動で待機・リトライが行われること。
- `movePaperMc` は `runCommand` を経由せず、`SshExec` を直接操作してコマンドを実行すること。SHA検証の結果によってコマンドの実行有無を制御する必要があるため、共通処理である
  `runCommand` には委譲できない設計となっている。
- `movePaperMc` におけるPl3xMapの旧ファイル削除（`CMD_PL3XMAP_RM`）は、新バージョンのダウンロード有無にかかわらず**常に実行
  **すること。これにより古いバージョンのPl3xMapが残留しないことを保証する。
- `wgetPaperMc` と `movePaperMc` の両方で独立してAPIを呼び出しバージョン情報を取得すること。

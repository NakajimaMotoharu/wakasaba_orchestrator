# wakasaba_orchestrator 要件定義書：WksConstants

## 基本情報

| 項目       | 内容                                             |
|----------|------------------------------------------------|
| 完全修飾クラス名 | `com.wks.parts.WksConstants`                   |
| ファイル名    | `WksConstants.java`                            |
| 種別       | class（public）                                  |
| 責務       | アプリケーション全体で使用する定数を一元管理する。インスタンス化は想定しない純粋な定数クラス |
| 主な依存クラス  | なし                                             |

---

## 要求機能

### WC-01：定数の一元管理

- アプリケーション全体で使用するファイルパス・コマンド文字列・ログメッセージ・URL・JSON操作文字列・その他の設定値をすべてこのクラスに集約すること。
- 環境変更（ファイルパス・サーバ設定等）が必要な場合、このクラスのみを修正すれば対応できること。

### WC-02：ファイルパス系定数

| 定数名                 | 値                                                 | 説明                           |
|---------------------|---------------------------------------------------|------------------------------|
| `PATH_EXEC_LOG`     | `/home/mini/wakasaba_orchestrator/log/log_%s.txt` | ログファイル出力先パス。`%s` に日時文字列を埋め込む |
| `PATH_DL_PAPERMC`   | `/home/mini/download/paper.jar`                   | PaperMC JARのダウンロード先パス        |
| `PATH_DL_PL3XMAP`   | `/home/mini/download/pl3xmap.jar`                 | Pl3xMap JARのダウンロード先パス        |
| `PATH_PROD_PAPERMC` | `/home/mini/mcs/prod/paper.jar`                   | PaperMC JAR本番配置先パス           |
| `PATH_PROD_PL3XMAP` | `/home/mini/mcs/prod/plugins/pl3xmap.jar`         | Pl3xMap JAR本番配置先パス           |
| `PATH_BACKUP_SHELL` | `/home/mini/mcs/shell/backup.sh`                  | バックアップシェルスクリプトのパス            |

### WC-03：Linuxコマンド系定数

| 定数名                  | 値                                                                            | 説明                                  |
|----------------------|------------------------------------------------------------------------------|-------------------------------------|
| `CMD_UPDATE`         | `sudo apt update`                                                            | パッケージリスト更新コマンド                      |
| `CMD_UPGRADE`        | `sudo apt upgrade -y`                                                        | パッケージアップグレードコマンド（自動承認）              |
| `CMD_SHUTDOWN`       | `sudo shutdown -r now`                                                       | 即時再起動コマンド                           |
| `CMD_WGET_PAPERMC`   | `wget -O /home/mini/download/paper.jar --user-agent="%s" %s`                 | PaperMCダウンロードコマンド。`%s` にUA・URLを埋め込む |
| `CMD_WGET_PL3XMAP`   | `wget -O /home/mini/download/pl3xmap.jar --user-agent="%s" %s`               | Pl3xMapダウンロードコマンド。`%s` にUA・URLを埋め込む |
| `CMD_PAPERMC_HASH`   | `sha256sum /home/mini/download/paper.jar`                                    | PaperMC JARのSHA256ハッシュ取得コマンド        |
| `CMD_PAPERMC_RM`     | `rm /home/mini/mcs/prod/paper.jar`                                           | 本番PaperMC JAR削除コマンド                 |
| `CMD_PAPERMC_MV`     | `mv /home/mini/download/paper.jar /home/mini/mcs/prod/paper.jar`             | PaperMC JARを本番へ移動するコマンド             |
| `CMD_PL3XMAP_HASH`   | `sha512sum /home/mini/download/pl3xmap.jar`                                  | Pl3xMap JARのSHA512ハッシュ取得コマンド        |
| `CMD_PL3XMAP_RM`     | `rm /home/mini/mcs/prod/plugins/pl3xmap.jar`                                 | 本番Pl3xMap JAR削除コマンド                 |
| `CMD_PL3XMAP_MV`     | `mv /home/mini/download/pl3xmap.jar /home/mini/mcs/prod/plugins/pl3xmap.jar` | Pl3xMap JARを本番へ移動するコマンド             |
| `CMD_PAPERMC_START`  | `sudo systemctl start papermc`                                               | PaperMCサービス起動コマンド                   |
| `CMD_PAPERMC_END`    | `sudo systemctl stop papermc`                                                | PaperMCサービス停止コマンド                   |
| `CMD_PAPERMC_BACKUP` | `sh /home/mini/mcs/shell/backup.sh`                                          | バックアップシェル実行コマンド                     |
| `CMD_DO_NOTHING`     | `:`                                                                          | Bashのno-opコマンド。SSH疎通確認用             |
| `CMD_SLEEP_SHUTDOWN` | `(sleep 60 && sudo shutdown -r now) &`                                       | 60秒後にバックグラウンドで再起動するコマンド             |
| `CMD_SHELL_HEAD`     | `sh`                                                                         | シェル実行コマンド（ProcessBuilder用）          |
| `CMD_SHELL_OPTION`   | `-c`                                                                         | シェル実行オプション（ProcessBuilder用）         |

### WC-04：ログメッセージ系定数

| 定数名               | 値                                     | 説明                  |
|-------------------|---------------------------------------|---------------------|
| `LOG_START_TIME`  | `Batch start time: %s`                | バッチ開始時刻ログフォーマット     |
| `LOG_END_TIME`    | `Batch end time: %s`                  | バッチ終了時刻ログフォーマット     |
| `LOG_COMMAND`     | `$ %s`                                | 実行コマンドのログフォーマット     |
| `LOG_SPLIT`       | `/* --------Job start: %s-------- */` | サーバ処理開始の区切りログフォーマット |
| `LOG_THIS_SERVER` | `this server`                         | 自サーバ処理開始時の識別用文字列    |

### WC-05：URLフォーマット系定数

| 定数名                   | 値                                                              | 説明                                    |
|-----------------------|----------------------------------------------------------------|---------------------------------------|
| `URL_PAPERMC_VERSION` | `https://fill.papermc.io/v3/projects/paper`                    | PaperMCバージョン一覧取得API                   |
| `URL_PAPERMC_DL_URL`  | `https://fill.papermc.io/v3/projects/paper/versions/%s/builds` | PaperMCビルド情報取得API。`%s` にバージョン文字列を埋め込む |
| `URL_PL3XMAP_DL_URL`  | `https://api.modrinth.com/v2/project/pl3xmap/version`          | Modrinth APIによるPl3xMapバージョン情報取得       |

### WC-06：JSON操作文字列系定数

| 定数名                  | 値                | 説明                           |
|----------------------|------------------|------------------------------|
| `JSON_PAPERMC_GV`    | `versions`       | PaperMC APIのバージョン一覧キー        |
| `JSON_PAPERMC_DL`    | `downloads`      | PaperMC APIのダウンロード情報キー       |
| `JSON_PAPERMC_SD`    | `server:default` | PaperMC APIのサーバ種別キー          |
| `JSON_PAPERMC_URL`   | `url`            | PaperMC APIのダウンロードURLキー      |
| `JSON_PAPERMC_CS`    | `checksums`      | PaperMC APIのチェックサム情報キー       |
| `JSON_PAPERMC_SHA`   | `sha256`         | PaperMC APIのSHA256値キー        |
| `JSON_PL3XMAP_GV`    | `game_versions`  | Modrinth APIの対応ゲームバージョンキー    |
| `JSON_PL3XMAP_FILES` | `files`          | Modrinth APIのファイル情報キー        |
| `JSON_PL3XMAP_URL`   | `url`            | Modrinth APIのダウンロードURLキー     |
| `JSON_PL3XMAP_HASH`  | `hashes`         | Modrinth APIのハッシュ情報キー        |
| `JSON_PL3XMAP_SHA`   | `sha512`         | Modrinth APIのSHA512値キー       |
| `JSON_REPLACE_DQ`    | `"`              | JSONノード文字列のダブルクオーテーション（除去対象） |
| `JSON_REPLACE_ES`    | （空文字）            | ダブルクオーテーション除去後の置換文字列         |

### WC-07：その他の定数

| 定数名                         | 値                                        | 説明                             |
|-----------------------------|------------------------------------------|--------------------------------|
| `OTHER_ARGS_MSG`            | `The length of the arguments must be 3.` | 引数不足時のUSAGEメッセージ               |
| `OTHER_TIME_ZONE`           | `Asia/Tokyo`                             | 日時文字列生成に使用するタイムゾーン             |
| `OTHER_DATE_TIME_FMT`       | `yyyyMMddHHmmss`                         | 日時フォーマット文字列                    |
| `OTHER_USER_AGENT`          | `wakasaba_orchestrator/1.0`              | HTTPリクエストのUser-Agent文字列        |
| `OTHER_SERVER_INFO`         | `{host: "%s", port: %d, user: "%s"}`     | サーバ情報のログ出力フォーマット（秘密鍵パスを含まない）   |
| `OTHER_CHANNEL_EXEC_OPTION` | `exec`                                   | JSchの `openChannel` に渡すチャンネル種別 |
| `OTHER_SSH_CONFIG`          | `StrictHostKeyChecking`                  | SSH接続設定キー（known_hostsチェック）     |
| `OTHER_SSH_CONFIG_VAL`      | `no`                                     | known_hostsチェックを無効化する設定値       |

---

## 要求インタフェース

- 全定数は `public static final` として宣言すること。
- インスタンス化は不要な設計とすること（必要に応じて privateコンストラクタを定義してよい）。

---

## 制約・注意事項

- ファイルパスはすべて `/home/mini/` 以下に固定されており、実行環境に依存する。環境変更時はこのクラスの定数のみを修正すれば対応できること。
- ファイルパスに依存するすべてのコマンド定数（`CMD_WGET_PAPERMC` / `CMD_WGET_PL3XMAP` /
  `CMD_PAPERMC_HASH` / `CMD_PAPERMC_RM` / `CMD_PAPERMC_MV` /
  `CMD_PL3XMAP_HASH` / `CMD_PL3XMAP_RM` / `CMD_PL3XMAP_MV` /
  `CMD_PAPERMC_BACKUP`）はパス定数のコンパイル時定数連結で組み立てられているため、パス定数を変更するとこれらすべてのコマンド定数に自動反映されること。
- 本クラスはインスタンス化を意図しないが、現在の実装では private コンストラクタが定義されていない。設計意図をコードで明示するために
  private コンストラクタの追加を検討すること。

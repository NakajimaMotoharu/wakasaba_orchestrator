# wakasaba_orchestrator 基本設計書：WksConstants

## 基本情報

| 項目       | 内容                                             |
|----------|------------------------------------------------|
| 完全修飾クラス名 | `com.wks.parts.WksConstants`                   |
| ファイル名    | `WksConstants.java`                            |
| 種別       | class（public）                                  |
| 責務       | アプリケーション全体で使用する定数を一元管理する。インスタンス化は想定しない純粋な定数クラス |
| 主な依存クラス  | なし                                             |

---

## 定数一覧

### ファイルパス系

| 定数名                 | 値                                                 | 説明                           |
|---------------------|---------------------------------------------------|------------------------------|
| `PATH_EXEC_LOG`     | `/home/mini/wakasaba_orchestrator/log/log_%s.txt` | ログファイル出力先パス。`%s` に日時文字列を埋め込む |
| `PATH_DL_PAPERMC`   | `/home/mini/download/paper.jar`                   | PaperMC JAR のダウンロード先パス       |
| `PATH_DL_PL3XMAP`   | `/home/mini/download/pl3xmap.jar`                 | Pl3xMap JAR のダウンロード先パス       |
| `PATH_PROD_PAPERMC` | `/home/mini/mcs/prod/paper.jar`                   | PaperMC JAR 本番配置先パス          |
| `PATH_PROD_PL3XMAP` | `/home/mini/mcs/prod/plugins/pl3xmap.jar`         | Pl3xMap JAR 本番配置先パス          |
| `PATH_BACKUP_SHELL` | `/home/mini/mcs/shell/backup.sh`                  | バックアップシェルスクリプトのパス            |

---

### Linux コマンド系

| 定数名                  | 値                                                                            | 説明                                     |
|----------------------|------------------------------------------------------------------------------|----------------------------------------|
| `CMD_UPDATE`         | `sudo apt update`                                                            | パッケージリスト更新コマンド                         |
| `CMD_UPGRADE`        | `sudo apt upgrade -y`                                                        | パッケージアップグレードコマンド（自動承認）                 |
| `CMD_SHUTDOWN`       | `sudo shutdown -r now`                                                       | 即時再起動コマンド                              |
| `CMD_WGET_PAPERMC`   | `wget -O /home/mini/download/paper.jar --user-agent="%s" %s`                 | PaperMC ダウンロードコマンド。`%s` に UA・URL を埋め込む |
| `CMD_WGET_PL3XMAP`   | `wget -O /home/mini/download/pl3xmap.jar --user-agent="%s" %s`               | Pl3xMap ダウンロードコマンド。`%s` に UA・URL を埋め込む |
| `CMD_PAPERMC_HASH`   | `sha256sum /home/mini/download/paper.jar`                                    | PaperMC JAR の SHA256 ハッシュ取得コマンド        |
| `CMD_PAPERMC_RM`     | `rm /home/mini/mcs/prod/paper.jar`                                           | 本番 PaperMC JAR 削除コマンド                  |
| `CMD_PAPERMC_MV`     | `mv /home/mini/download/paper.jar /home/mini/mcs/prod/paper.jar`             | PaperMC JAR を本番へ移動するコマンド               |
| `CMD_PL3XMAP_HASH`   | `sha512sum /home/mini/download/pl3xmap.jar`                                  | Pl3xMap JAR の SHA512 ハッシュ取得コマンド        |
| `CMD_PL3XMAP_RM`     | `rm /home/mini/mcs/prod/plugins/pl3xmap.jar`                                 | 本番 Pl3xMap JAR 削除コマンド                  |
| `CMD_PL3XMAP_MV`     | `mv /home/mini/download/pl3xmap.jar /home/mini/mcs/prod/plugins/pl3xmap.jar` | Pl3xMap JAR を本番へ移動するコマンド               |
| `CMD_PAPERMC_START`  | `sudo systemctl start papermc`                                               | PaperMC サービス起動コマンド                     |
| `CMD_PAPERMC_END`    | `sudo systemctl stop papermc`                                                | PaperMC サービス停止コマンド                     |
| `CMD_PAPERMC_BACKUP` | `sh /home/mini/mcs/shell/backup.sh`                                          | バックアップシェル実行コマンド                        |
| `CMD_DO_NOTHING`     | `:`                                                                          | Bash の no-op コマンド。SSH 疎通確認用            |
| `CMD_SLEEP_SHUTDOWN` | `(sleep 60 && sudo shutdown -r now) &`                                       | 60 秒後にバックグラウンドで再起動するコマンド               |
| `CMD_SHELL_HEAD`     | `sh`                                                                         | シェル実行コマンド（ProcessBuilder 用）            |
| `CMD_SHELL_OPTION`   | `-c`                                                                         | シェル実行オプション（ProcessBuilder 用）           |

---

### ログメッセージ系

| 定数名               | 値                                     | 説明                  |
|-------------------|---------------------------------------|---------------------|
| `LOG_START_TIME`  | `Batch start time: %s`                | バッチ開始時刻ログフォーマット     |
| `LOG_END_TIME`    | `Batch end time: %s`                  | バッチ終了時刻ログフォーマット     |
| `LOG_COMMAND`     | `$ %s`                                | 実行コマンドのログフォーマット     |
| `LOG_SPLIT`       | `/* --------Job start: %s-------- */` | サーバ処理開始の区切りログフォーマット |
| `LOG_THIS_SERVER` | `this server`                         | 自サーバ識別用文字列          |

---

### URL フォーマット系

| 定数名                   | 値                                                              | 説明                                      |
|-----------------------|----------------------------------------------------------------|-----------------------------------------|
| `URL_PAPERMC_VERSION` | `https://fill.papermc.io/v3/projects/paper`                    | PaperMC バージョン一覧取得 API                   |
| `URL_PAPERMC_DL_URL`  | `https://fill.papermc.io/v3/projects/paper/versions/%s/builds` | PaperMC ビルド情報取得 API。`%s` にバージョン文字列を埋め込む |
| `URL_PL3XMAP_DL_URL`  | `https://api.modrinth.com/v2/project/pl3xmap/version`          | Modrinth API による Pl3xMap バージョン情報取得      |

---

### JSON 操作文字列系

| 定数名                  | 値                | 説明                          |
|----------------------|------------------|-----------------------------|
| `JSON_PAPERMC_GV`    | `versions`       | PaperMC API のバージョン一覧キー      |
| `JSON_PAPERMC_DL`    | `downloads`      | PaperMC API のダウンロード情報キー     |
| `JSON_PAPERMC_SD`    | `server:default` | PaperMC API のサーバ種別キー        |
| `JSON_PAPERMC_URL`   | `url`            | PaperMC API のダウンロード URL キー  |
| `JSON_PAPERMC_CS`    | `checksums`      | PaperMC API のチェックサム情報キー     |
| `JSON_PAPERMC_SHA`   | `sha256`         | PaperMC API の SHA256 値キー    |
| `JSON_PL3XMAP_GV`    | `game_versions`  | Modrinth API の対応ゲームバージョンキー  |
| `JSON_PL3XMAP_FILES` | `files`          | Modrinth API のファイル情報キー      |
| `JSON_PL3XMAP_URL`   | `url`            | Modrinth API のダウンロード URL キー |
| `JSON_PL3XMAP_HASH`  | `hashes`         | Modrinth API のハッシュ情報キー      |
| `JSON_PL3XMAP_SHA`   | `sha512`         | Modrinth API の SHA512 値キー   |
| `JSON_REPLACE_DQ`    | `"`              | JSON 文字列のダブルクオーテーション（除去対象）  |
| `JSON_REPLACE_ES`    | （空文字）            | ダブルクオーテーション除去後の置換文字列        |

---

### その他

| 定数名                         | 値                                        | 説明                              |
|-----------------------------|------------------------------------------|---------------------------------|
| `OTHER_ARGS_MSG`            | `The length of the arguments must be 3.` | 引数不足時の USAGE メッセージ              |
| `OTHER_TIME_ZONE`           | `Asia/Tokyo`                             | 日時文字列生成に使用するタイムゾーン              |
| `OTHER_DATE_TIME_FMT`       | `yyyyMMddHHmmss`                         | 日時フォーマット文字列                     |
| `OTHER_USER_AGENT`          | `wakasaba_orchestrator/1.0`              | HTTP リクエストの User-Agent 文字列      |
| `OTHER_SERVER_INFO`         | `{host: "%s", port: %d, user: "%s"}`     | サーバ情報のログ出力フォーマット                |
| `OTHER_CHANNEL_EXEC_OPTION` | `exec`                                   | JSch の `openChannel` に渡すチャンネル種別 |
| `OTHER_SSH_CONFIG`          | `StrictHostKeyChecking`                  | SSH 接続設定キー（known_hosts チェック）    |
| `OTHER_SSH_CONFIG_VAL`      | `no`                                     | known_hosts チェックを無効化する設定値       |

---

## 設計上の注意点

- 本クラスはインスタンス化を意図しないが、private コンストラクタが定義されていない。
- `CMD_WGET_PAPERMC` / `CMD_WGET_PL3XMAP` は `PATH_DL_PAPERMC` / `PATH_DL_PL3XMAP`
  の値をコンパイル時定数連結で組み立てているため、パス定数を変更すると自動的にコマンド定数にも反映される。
- ファイルパスはすべて `/home/mini/` 以下に固定されており、実行環境に依存する。環境変更時はこのクラスの定数のみを修正すればよい。

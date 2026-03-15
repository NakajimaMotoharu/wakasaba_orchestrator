# クラス詳細設計：WksConstants

## 基本情報

| 項目       | 内容                                             |
|----------|------------------------------------------------|
| 完全修飾クラス名 | `com.wks.parts.WksConstants`                   |
| ファイル名    | `WksConstants.java`                            |
| 種別       | class（public）                                  |
| 責務       | アプリケーション全体で使用する定数を一元管理する。インスタンス化は想定しない純粋な定数クラス |
| 依存クラス    | なし                                             |

---

## 定数一覧

### ファイルパス系

| 定数名                 | 型        | 値                                                   | 説明                           |
|---------------------|----------|-----------------------------------------------------|------------------------------|
| `PATH_EXEC_LOG`     | `String` | `"/home/mini/wakasaba_orchestrator/log/log_%s.txt"` | ログファイル出力先パス。`%s` に日時文字列を埋め込む |
| `PATH_DL_PAPERMC`   | `String` | `"/home/mini/download/paper.jar"`                   | PaperMC JARのダウンロード先パス        |
| `PATH_DL_PL3XMAP`   | `String` | `"/home/mini/download/pl3xmap.jar"`                 | Pl3xMap JARのダウンロード先パス        |
| `PATH_PROD_PAPERMC` | `String` | `"/home/mini/mcs/prod/paper.jar"`                   | PaperMC JAR本番配置先パス           |
| `PATH_PROD_PL3XMAP` | `String` | `"/home/mini/mcs/prod/plugins/pl3xmap.jar"`         | Pl3xMap JAR本番配置先パス           |
| `PATH_BACKUP_SHELL` | `String` | `"/home/mini/mcs/shell/backup.sh"`                  | バックアップシェルスクリプトのパス            |

### Linuxコマンド系

| 定数名                  | 型        | 値                                                                              | 説明                                     |
|----------------------|----------|--------------------------------------------------------------------------------|----------------------------------------|
| `CMD_UPDATE`         | `String` | `"sudo apt update"`                                                            | パッケージリスト更新コマンド                         |
| `CMD_UPGRADE`        | `String` | `"sudo apt upgrade -y"`                                                        | パッケージアップグレードコマンド（自動承認）                 |
| `CMD_SHUTDOWN`       | `String` | `"sudo shutdown -r now"`                                                       | 即時再起動コマンド                              |
| `CMD_WGET_PAPERMC`   | `String` | `"wget -O /home/mini/download/paper.jar --user-agent=\"%s\" %s"`               | PaperMCダウンロードコマンド。`%s` にUA文字列・URLを埋め込む |
| `CMD_WGET_PL3XMAP`   | `String` | `"wget -O /home/mini/download/pl3xmap.jar --user-agent=\"%s\" %s"`             | Pl3xMapダウンロードコマンド。`%s` にUA文字列・URLを埋め込む |
| `CMD_PAPERMC_HASH`   | `String` | `"sha256sum /home/mini/download/paper.jar"`                                    | PaperMC JARのSHA256ハッシュ取得コマンド           |
| `CMD_PAPERMC_RM`     | `String` | `"rm /home/mini/mcs/prod/paper.jar"`                                           | 本番PaperMC JAR削除コマンド                    |
| `CMD_PAPERMC_MV`     | `String` | `"mv /home/mini/download/paper.jar /home/mini/mcs/prod/paper.jar"`             | PaperMC JARを本番へ移動するコマンド                |
| `CMD_PL3XMAP_HASH`   | `String` | `"sha512sum /home/mini/download/pl3xmap.jar"`                                  | Pl3xMap JARのSHA512ハッシュ取得コマンド           |
| `CMD_PL3XMAP_RM`     | `String` | `"rm /home/mini/mcs/prod/plugins/pl3xmap.jar"`                                 | 本番Pl3xMap JAR削除コマンド                    |
| `CMD_PL3XMAP_MV`     | `String` | `"mv /home/mini/download/pl3xmap.jar /home/mini/mcs/prod/plugins/pl3xmap.jar"` | Pl3xMap JARを本番へ移動するコマンド                |
| `CMD_PAPERMC_START`  | `String` | `"sudo systemctl start papermc"`                                               | PaperMCサービス起動コマンド                      |
| `CMD_PAPERMC_END`    | `String` | `"sudo systemctl stop papermc"`                                                | PaperMCサービス停止コマンド                      |
| `CMD_PAPERMC_BACKUP` | `String` | `"sh /home/mini/mcs/shell/backup.sh"`                                          | バックアップシェル実行コマンド                        |
| `CMD_DO_NOTHING`     | `String` | `":"`                                                                          | Bashのno-opコマンド。SSH疎通確認用                |
| `CMD_SLEEP_SHUTDOWN` | `String` | `"(sleep 60 && sudo shutdown -r now) &"`                                       | 60秒後にバックグラウンドで再起動するコマンド                |
| `CMD_SHELL_HEAD`     | `String` | `"sh"`                                                                         | シェル実行コマンド（ProcessBuilder用）             |
| `CMD_SHELL_OPTION`   | `String` | `"-c"`                                                                         | シェル実行オプション（ProcessBuilder用）            |

### ログメッセージ系

| 定数名               | 型        | 値                                       | 説明                  |
|-------------------|----------|-----------------------------------------|---------------------|
| `LOG_START_TIME`  | `String` | `"Batch start time: %s"`                | バッチ開始時刻ログフォーマット     |
| `LOG_END_TIME`    | `String` | `"Batch end time: %s"`                  | バッチ終了時刻ログフォーマット     |
| `LOG_COMMAND`     | `String` | `"$ %s"`                                | 実行コマンドのログフォーマット     |
| `LOG_SPLIT`       | `String` | `"/* --------Job start: %s-------- */"` | サーバ処理開始の区切りログフォーマット |
| `LOG_THIS_SERVER` | `String` | `"this server"`                         | 自サーバ識別用文字列          |

### URLフォーマット系

| 定数名                   | 型        | 値                                                                | 説明                                    |
|-----------------------|----------|------------------------------------------------------------------|---------------------------------------|
| `URL_PAPERMC_VERSION` | `String` | `"https://fill.papermc.io/v3/projects/paper"`                    | PaperMCバージョン一覧取得API                   |
| `URL_PAPERMC_DL_URL`  | `String` | `"https://fill.papermc.io/v3/projects/paper/versions/%s/builds"` | PaperMCビルド情報取得API。`%s` にバージョン文字列を埋め込む |
| `URL_PL3XMAP_DL_URL`  | `String` | `"https://api.modrinth.com/v2/project/pl3xmap/version"`          | Modrinth APIによるPl3xMapバージョン情報取得       |

### JSON操作文字列系

| 定数名                  | 型        | 値                  | 説明                        |
|----------------------|----------|--------------------|---------------------------|
| `JSON_PAPERMC_GV`    | `String` | `"versions"`       | PaperMC APIのバージョン一覧キー     |
| `JSON_PAPERMC_DL`    | `String` | `"downloads"`      | PaperMC APIのダウンロード情報キー    |
| `JSON_PAPERMC_SD`    | `String` | `"server:default"` | PaperMC APIのサーバ種別キー       |
| `JSON_PAPERMC_URL`   | `String` | `"url"`            | PaperMC APIのダウンロードURLキー   |
| `JSON_PAPERMC_CS`    | `String` | `"checksums"`      | PaperMC APIのチェックサム情報キー    |
| `JSON_PAPERMC_SHA`   | `String` | `"sha256"`         | PaperMC APIのSHA256値キー     |
| `JSON_PL3XMAP_GV`    | `String` | `"game_versions"`  | Modrinth APIの対応ゲームバージョンキー |
| `JSON_PL3XMAP_FILES` | `String` | `"files"`          | Modrinth APIのファイル情報キー     |
| `JSON_PL3XMAP_URL`   | `String` | `"url"`            | Modrinth APIのダウンロードURLキー  |
| `JSON_PL3XMAP_HASH`  | `String` | `"hashes"`         | Modrinth APIのハッシュ情報キー     |
| `JSON_PL3XMAP_SHA`   | `String` | `"sha512"`         | Modrinth APIのSHA512値キー    |
| `JSON_REPLACE_DQ`    | `String` | `"\""`             | JSON文字列のダブルクオーテーション（除去対象） |
| `JSON_REPLACE_ES`    | `String` | `""`               | ダブルクオーテーション除去後の置換文字列（空文字） |

### その他

| 定数名                         | 型        | 値                                          | 説明                              |
|-----------------------------|----------|--------------------------------------------|---------------------------------|
| `OTHER_ARGS_MSG`            | `String` | `"The length of the arguments must be 3."` | 引数不足時のUSAGEメッセージ                |
| `OTHER_TIME_ZONE`           | `String` | `"Asia/Tokyo"`                             | 日時文字列生成に使用するタイムゾーン              |
| `OTHER_DATE_TIME_FMT`       | `String` | `"yyyyMMddHHmmss"`                         | 日時フォーマット文字列                     |
| `OTHER_USER_AGENT`          | `String` | `"wakasaba_orchestrator/1.0"`              | HTTPリクエストのUser-Agent文字列         |
| `OTHER_SERVER_INFO`         | `String` | `"{host: \"%s\", port: %d, user: \"%s\"}"` | サーバ情報のログ出力フォーマット                |
| `OTHER_CHANNEL_EXEC_OPTION` | `String` | `"exec"`                                   | JSch の `openChannel` に渡すチャンネル種別 |
| `OTHER_SSH_CONFIG`          | `String` | `"StrictHostKeyChecking"`                  | SSH接続設定キー（known_hostsチェック）      |
| `OTHER_SSH_CONFIG_VAL`      | `String` | `"no"`                                     | known_hostsチェックを無効化する設定値        |

---

## 設計上の注意点

- 本クラスはインスタンス化を意図しないが、private コンストラクタが定義されていない。必要に応じて追加を検討する。
- `CMD_WGET_PAPERMC` / `CMD_WGET_PL3XMAP` はパスを文字列連結で組み立てているため、`PATH_DL_PAPERMC` / `PATH_DL_PL3XMAP`
  の値を変更しても `CMD_*` 側には自動反映される。
- ファイルパスはすべて `/home/mini/` 以下に固定されており、実行環境に依存する。環境を変更する場合はこのクラスの定数を変更すればよい。

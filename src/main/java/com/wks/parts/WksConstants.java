package com.wks.parts;

public class WksConstants {
    // サーバ定数
    /**
     * テストサーバのサーバ定数
     */
    public static final int SERVER_TEST = 0;
    /**
     * PaperMCサーバのサーバ定数
     */
    public static final int SERVER_PAPER = 1;
    /**
     * ローカルLLMサーバのサーバ定数
     */
    public static final int SERVER_LLM = 2;
    /**
     * Schubertサーバのサーバ定数
     */
    public static final int SERVER_SCHUBERT = 3;
    /**
     * VPNサーバのサーバ定数
     */
    public static final int SERVER_VPN = 4;
    /**
     * NASサーバのサーバ定数
     */
    public static final int SERVER_NAS = 5;

    // ファイルパス
    /**
     * wakasaba_orchestratorディレクトリ
     */
    public static final String PATH_WAKASABA_EXEC = "/home/mini/wakasaba_orchestrator/";
    /**
     * ログ出力先パス
     */
    public static final String PATH_EXEC_LOG = PATH_WAKASABA_EXEC + "log/log_%s.txt";
    /**
     * PaperMCバージョン情報格納パス
     */
    public static final String PATH_PAPERMC_VERSION = PATH_WAKASABA_EXEC + "papermc_version.txt";
    /**
     * PaperMCダウンロード先パス
     */
    public static final String PATH_DL_PAPERMC = "/home/mini/download/paper.jar";
    /**
     * Pl3xMapダウンロード先パス
     */
    public static final String PATH_DL_PL3XMAP = "/home/mini/download/pl3xmap.jar";
    /**
     * PaperMC実行ディレクトリパス
     */
    public static final String PATH_PROD_PAPERMC = "/home/mini/mcs/prod/paper.jar";
    /**
     * Pl3xMap実行ディレクトリパス
     */
    public static final String PATH_PROD_PL3XMAP = "/home/mini/mcs/prod/plugins/pl3xmap.jar";
    /**
     * バックアップ処理シェル格納パス
     */
    public static final String PATH_BACKUP_SHELL = "/home/mini/mcs/shell/backup.sh";
    /**
     * Schubertアプリ格納パス
     */
    public static final String PATH_PROD_SCHUBERT_APP = "/home/mini/schubert/";
    /**
     * Schubert起動シェルパス
     */
    public static final String PATH_PROD_SCHUBERT_START_SHELL = PATH_PROD_SCHUBERT_APP + "start_schubert.sh";
    /**
     * Schubert停止シェルパス
     */
    public static final String PATH_PROD_SCHUBERT_STOP_SHELL = PATH_PROD_SCHUBERT_APP + "stop_schubert.sh";

    // Linuxコマンド
    /**
     * アップデートコマンド
     */
    public static final String CMD_UPDATE = "sudo apt update";
    /**
     * アップグレードコマンド
     */
    public static final String CMD_UPGRADE = "sudo apt upgrade -y";
    /**
     * 再起動コマンド
     */
    public static final String CMD_SHUTDOWN = "sudo shutdown -r now";
    /**
     * PaperMCダウンロードコマンド
     */
    public static final String CMD_WGET_PAPERMC = "wget -O " + PATH_DL_PAPERMC + " --user-agent=\"%s\" %s";
    /**
     * Pl3xMapダウンロードコマンド
     */
    public static final String CMD_WGET_PL3XMAP = "wget -O " + PATH_DL_PL3XMAP + " --user-agent=\"%s\" %s";
    /**
     * PaperMCハッシュ取得コマンド
     */
    public static final String CMD_PAPERMC_HASH = "sha256sum " + PATH_DL_PAPERMC;
    /**
     * PaperMC削除コマンド
     */
    public static final String CMD_PAPERMC_RM = "rm " + PATH_PROD_PAPERMC;
    /**
     * PaperMC移動コマンド
     */
    public static final String CMD_PAPERMC_MV = "mv " + PATH_DL_PAPERMC + " " + PATH_PROD_PAPERMC;
    /**
     * Pl3xMapハッシュ取得コマンド
     */
    public static final String CMD_PL3XMAP_HASH = "sha512sum " + PATH_DL_PL3XMAP;
    /**
     * Pl3xMap削除コマンド
     */
    public static final String CMD_PL3XMAP_RM = "rm " + PATH_PROD_PL3XMAP;
    /**
     * Pl3xMap移動コマンド
     */
    public static final String CMD_PL3XMAP_MV = "mv " + PATH_DL_PL3XMAP + " " + PATH_PROD_PL3XMAP;
    /**
     * PaperMC起動コマンド
     */
    public static final String CMD_PAPERMC_START = "sudo systemctl start papermc";
    /**
     * PaperMC停止コマンド
     */
    public static final String CMD_PAPERMC_END = "sudo systemctl stop papermc";
    /**
     * バックアップシェル実行コマンド
     */
    public static final String CMD_PAPERMC_BACKUP = "sh " + PATH_BACKUP_SHELL;
    /**
     * Schubert起動コマンド
     */
    public static final String CMD_SCHUBERT_START = "sh " + PATH_PROD_SCHUBERT_START_SHELL;
    /**
     * Schubert停止コマンド
     */
    public static final String CMD_SCHUBERT_END = "sh " + PATH_PROD_SCHUBERT_STOP_SHELL;
    /**
     * 何もしないコマンド
     */
    public static final String CMD_DO_NOTHING = ":";
    /**
     * 1分間待機するコマンド
     */
    public static final String CMD_WAIT_ONE_MIN = "sleep 60";
    /**
     * スリープ後再起動コマンド
     */
    public static final String CMD_SLEEP_SHUTDOWN = "(" + CMD_WAIT_ONE_MIN + " && sudo shutdown -r now) &";
    /**
     * シェル実行コマンド
     */
    public static final String CMD_SHELL_HEAD = "sh";
    /**
     * シェル実行オプション
     */
    public static final String CMD_SHELL_OPTION = "-c";
    /**
     * NASマウントコマンド
     */
    public static final String CMD_MOUNT_NAS = "sudo mount /nas";
    /**
     * NASアンマウントコマンド
     */
    public static final String CMD_UMOUNT_NAS = "sudo umount /nas";
    /**
     * NASマウント状態取得コマンド
     */
    public static final String CMD_MOUNT_STATUS_NAS = "findmnt /nas";
    /**
     * SHAREマウントコマンド
     */
    public static final String CMD_MOUNT_SHARE = "sudo mount /share";
    /**
     * SHAREアンマウントコマンド
     */
    public static final String CMD_UMOUNT_SHARE = "sudo umount /share";
    /**
     * SHAREマウント状態取得コマンド
     */
    public static final String CMD_MOUNT_STATUS_SHARE = "findmnt /share";
    /**
     * 時刻取得コマンド
     */
    public static final String CMD_GET_TIME = "date '+%s'";

    // ログメッセージ
    /**
     * システムログタイプ
     */
    public static final String LOG_LEVEL_SYSTEM = "SYS";
    /**
     * アプリログタイプ
     */
    public static final String LOG_LEVEL_APPLICATIONS = "APL";
    /**
     * コマンドログタイプ
     */
    public static final String LOG_LEVEL_COMMAND = "CMD";
    /**
     * 例外ログタイプ
     */
    public static final String LOG_LEVEL_EXCEPTION = "ERR";
    /**
     * 出力タイプ
     */
    public static final String LOG_LEVEL_OUTPUT = "OUT";
    /**
     * コマンドログ用接頭辞
     */
    public static final String LOG_PARTS_DOL = "$ ";
    /**
     * ログ出力用改行文字
     */
    public static final String LOG_PARTS_LF = "\n";
    /**
     * ログ出力フォーマット
     */
    public static final String LOG_FORMAT = "[%s %s %s]: %s" + LOG_PARTS_LF;

    public static final String LOG_START_MESSAGE = "BATCH MAINTENANCE START";

    public static final String LOG_FINISH_MESSAGE = "BATCH MAINTENANCE END";

    // URLフォーマット
    /**
     * PaperMCバージョン取得API
     */
    public static final String URL_PAPERMC_VERSION = "https://fill.papermc.io/v3/projects/paper";
    /**
     * PaperMCダウンロードURL取得API
     */
    public static final String URL_PAPERMC_DL_URL = "https://fill.papermc.io/v3/projects/paper/versions/%s/builds";
    /**
     * Pl3xMapダウンロードURL取得API
     */
    public static final String URL_PL3XMAP_DL_URL = "https://api.modrinth.com/v2/project/pl3xmap/version";

    // JSON操作文字列
    /**
     * PaperMC-ダウンロードAPIのバージョン情報
     */
    public static final String JSON_PAPERMC_GV = "versions";
    /**
     * PaperMC-ダウンロードAPIのダウンロード情報
     */
    public static final String JSON_PAPERMC_DL = "downloads";
    /**
     * PaperMC-ダウンロードAPIのサーバ情報
     */
    public static final String JSON_PAPERMC_SD = "server:default";
    /**
     * PaperMC-ダウンロードAPIのURL情報
     */
    public static final String JSON_PAPERMC_URL = "url";
    /**
     * PaperMC-ダウンロードAPIのチェックサム情報
     */
    public static final String JSON_PAPERMC_CS = "checksums";
    /**
     * PaperMC-ダウンロードAPIのハッシュ情報
     */
    public static final String JSON_PAPERMC_SHA = "sha256";
    /**
     * Pl3xMap-ダウンロードAPIのゲームバージョン情報
     */
    public static final String JSON_PL3XMAP_GV = "game_versions";
    /**
     * Pl3xMap-ダウンロードAPIのファイル情報
     */
    public static final String JSON_PL3XMAP_FILES = "files";
    /**
     * Pl3xMap-ダウンロードAPIのURL情報
     */
    public static final String JSON_PL3XMAP_URL = "url";
    /**
     * Pl3xMap-ダウンロードAPIのハッシュ情報
     */
    public static final String JSON_PL3XMAP_HASH = "hashes";
    /**
     * Pl3xMap-ダウンロードAPIのSHA512情報
     */
    public static final String JSON_PL3XMAP_SHA = "sha512";
    /**
     * メジャーバージョン初期値用空文字
     */
    public static final String JSON_ES = "";
    /**
     * メジャーバージョン判別用ハイフン
     */
    public static final String JSON_HYPHEN = "-";
    /**
     * ダブルクオーテーション除去用ダブルクオーテーション
     */
    public static final String JSON_REPLACE_DQ = "\"";
    /**
     * ダブルクオーテーション除去用空文字
     */
    public static final String JSON_REPLACE_ES = "";

    // その他
    /**
     * 引数異常時に表示するUSAGE
     */
    public static final String OTHER_ARGS_MSG = "The length of the arguments must be 6.";
    /**
     * タイムゾーン
     */
    public static final String OTHER_TIME_ZONE = "Asia/Tokyo";
    /**
     * 日時文字列変換時フォーマット
     */
    public static final String OTHER_DATE_TIME_FMT = "yyyyMMddHHmmss";
    /**
     * ユーザエージェント定数
     */
    public static final String OTHER_USER_AGENT = "wakasaba_orchestrator/1.0";
    /**
     * 接続情報文字列変換フォーマット
     */
    public static final String OTHER_SERVER_INFO = "{host: \"%s\", port: %d, user: \"%s\"}";
    /**
     * ChannelExecのオプション指定
     */
    public static final String OTHER_CHANNEL_EXEC_OPTION = "exec";
    /**
     * SSH接続時KnownHostオプション
     */
    public static final String OTHER_SSH_CONFIG = "StrictHostKeyChecking";
    /**
     * SSH接続時KnownHostオプションの設定値
     */
    public static final String OTHER_SSH_CONFIG_VAL = "no";
    /**
     * 全サーバが現在アクティブでないときの警告メッセージ
     */
    public static final String OTHER_NOT_ALIVE_MSG = "Servers Not Active";
    /**
     * PaperMCのバージョン探索で正式版が見つからなかった時の例外内容
     */
    public static final String OTHER_PAPERMC_EXCEPTION = "No official version was found.";
    /**
     * NASマウント状態が正常でない場合の例外内容
     */
    public static final String OTHER_MOUNT_STATUS_EXCEPTION = "The mount status is not normal.";
    /**
     * 疎通確認時のメッセージ
     */
    public static final String OTHER_CONNECTION_CHECK = "Checking Connection";
    /**
     * 再起動待機時間上限
     */
    public static final int SERVER_REBOOT_WAIT_LIMIT = 600;
    /**
     * サーバの起動状態に復帰しない場合の例外内容
     */
    public static final String OTHER_REBOOT_EXCEPTION = "The server did not restart successfully: %s";
    /**
     * ローカルホストIPの出力
     */
    public static final String OTHER_IP_LOCALHOST = "localhost";
}

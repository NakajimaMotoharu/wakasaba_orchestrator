package com.wks.parts;

public class WksConstants {
	// ファイルパス
	/** ログ出力先パス */
	public static final String PATH_EXEC_LOG = "/home/mini/wakasaba_orchestrator/log/log_%s.txt";
	/** PaperMCダウンロード先パス */
	public static final String PATH_DL_PAPERMC = "/home/mini/download/paper.jar";
	/** Pl3xMapダウンロード先パス */
	public static final String PATH_DL_PL3XMAP = "/home/mini/download/pl3xmap.jar";
	/** PaperMC実行ディレクトリパス */
	public static final String PATH_PROD_PAPERMC = "/home/mini/mcs/prod/paper.jar";
	/** Pl3xMap実行ディレクトリパス */
	public static final String PATH_PROD_PL3XMAP = "/home/mini/mcs/prod/plugins/pl3xmap.jar";
	/** バックアップ処理シェル格納パス */
	public static final String PATH_BACKUP_SHELL = "/home/mini/mcs/shell/backup.sh";

	// Linuxコマンド
	/** アップデートコマンド */
	public static final String CMD_UPDATE = "sudo apt update";
	/** アップグレードコマンド */
	public static final String CMD_UPGRADE = "sudo apt upgrade -y";
	/** 再起動コマンド */
	public static final String CMD_SHUTDOWN = "sudo shutdown -r now";
	/** PaperMCダウンロードコマンド */
	public static final String CMD_WGET_PAPERMC = "wget -O " + PATH_DL_PAPERMC + " --user-agent=\"%s\" %s";
	/** Pl3xMapダウンロードコマンド */
	public static final String CMD_WGET_PL3XMAP = "wget -O " + PATH_DL_PL3XMAP + " --user-agent=\"%s\" %s";
	/** PaperMCハッシュ取得コマンド */
	public static final String CMD_PAPERMC_HASH = "sha256sum " + PATH_DL_PAPERMC;
	/** PaperMC削除コマンド */
	public static final String CMD_PAPERMC_RM = "rm " + PATH_PROD_PAPERMC;
	/** PaperMC移動コマンド */
	public static final String CMD_PAPERMC_MV = "mv " + PATH_DL_PAPERMC + " " + PATH_PROD_PAPERMC;
	/** Pl3xMapハッシュ取得コマンド */
	public static final String CMD_PL3XMAP_HASH = "sha512sum " + PATH_DL_PL3XMAP;
	/** Pl3xMap削除コマンド */
	public static final String CMD_PL3XMAP_RM = "rm " + PATH_PROD_PL3XMAP;
	/** Pl3xMap移動コマンド */
	public static final String CMD_PL3XMAP_MV = "mv " + PATH_DL_PL3XMAP + " " + PATH_PROD_PL3XMAP;
	/** PaperMC起動コマンド */
	public static final String CMD_PAPERMC_START = "sudo systemctl start papermc";
	/** PaperMC停止コマンド */
	public static final String CMD_PAPERMC_END = "sudo systemctl stop papermc";
	/** バックアップシェル実行コマンド */
	public static final String CMD_PAPERMC_BACKUP = "sh " + PATH_BACKUP_SHELL;
	/** 何もしないコマンド */
	public static final String CMD_DO_NOTHING = ":";
	/** 1分間待機するコマンド */
	public static final String CMD_WAIT_ONE_MIN = "sleep 60";
	/** スリープ後再起動コマンド */
	public static final String CMD_SLEEP_SHUTDOWN = "(" + CMD_WAIT_ONE_MIN + " && sudo shutdown -r now) &";
	/** シェル実行コマンド */
	public static final String CMD_SHELL_HEAD = "sh";
	/** シェル実行オプション */
	public static final String CMD_SHELL_OPTION = "-c";

	// ログメッセージ
	/** プログラム開始ログ */
	public static final String LOG_START_TIME = "Batch start time: %s";
	/** プログラム終了ログ */
	public static final String LOG_END_TIME = "Batch end time: %s";
	/** 実行コマンド記述ログ */
	public static final String LOG_COMMAND = "$ %s";
	/** サーバ区切りログ */
	public static final String LOG_SPLIT = "/* --------Job start: %s-------- */";
	/** 実行サーバログ */
	public static final String LOG_THIS_SERVER = "this server";

	// URLフォーマット
	/** PaperMCバージョン取得API */
	public static final String URL_PAPERMC_VERSION = "https://fill.papermc.io/v3/projects/paper";
	/** PaperMCダウンロードURL取得API */
	public static final String URL_PAPERMC_DL_URL = "https://fill.papermc.io/v3/projects/paper/versions/%s/builds";
	/** Pl3xMapダウンロードURL取得API */
	public static final String URL_PL3XMAP_DL_URL = "https://api.modrinth.com/v2/project/pl3xmap/version";

	// JSON操作文字列
	/** PaperMC-ダウンロードAPIのバージョン情報 */
	public static final String JSON_PAPERMC_GV = "versions";
	/** PaperMC-ダウンロードAPIのダウンロード情報 */
	public static final String JSON_PAPERMC_DL = "downloads";
	/** PaperMC-ダウンロードAPIのサーバ情報 */
	public static final String JSON_PAPERMC_SD = "server:default";
	/** PaperMC-ダウンロードAPIのURL情報 */
	public static final String JSON_PAPERMC_URL = "url";
	/** PaperMC-ダウンロードAPIのチェックサム情報 */
	public static final String JSON_PAPERMC_CS = "checksums";
	/** PaperMC-ダウンロードAPIのハッシュ情報 */
	public static final String JSON_PAPERMC_SHA = "sha256";
	/** Pl3xMap-ダウンロードAPIのゲームバージョン情報 */
	public static final String JSON_PL3XMAP_GV = "game_versions";
	/** Pl3xMap-ダウンロードAPIのファイル情報 */
	public static final String JSON_PL3XMAP_FILES = "files";
	/** Pl3xMap-ダウンロードAPIのURL情報 */
	public static final String JSON_PL3XMAP_URL = "url";
	/** Pl3xMap-ダウンロードAPIのハッシュ情報 */
	public static final String JSON_PL3XMAP_HASH = "hashes";
	/** Pl3xMap-ダウンロードAPIのSHA512情報 */
	public static final String JSON_PL3XMAP_SHA = "sha512";
	/** ダブルクオーテーション除去用ダブルクオーテーション */
	public static final String JSON_REPLACE_DQ = "\"";
	/** ダブルクオーテーション除去用空文字 */
	public static final String JSON_REPLACE_ES = "";

	// その他
	/** 引数異常時に表示するUSAGE */
	public static final String OTHER_ARGS_MSG = "The length of the arguments must be 3.";
	/** タイムゾーン */
	public static final String OTHER_TIME_ZONE = "Asia/Tokyo";
	/** 日時文字列変換時フォーマット */
	public static final String OTHER_DATE_TIME_FMT = "yyyyMMddHHmmss";
	/** ユーザエージェント定数 */
	public static final String OTHER_USER_AGENT = "wakasaba_orchestrator/1.0";
	/** 接続情報文字列変換フォーマット */
	public static final String OTHER_SERVER_INFO = "{host: \"%s\", port: %d, user: \"%s\"}";
	/** ChannelExecのオプション指定 */
	public static final String OTHER_CHANNEL_EXEC_OPTION = "exec";
	/** SSH接続時KnownHostオプション */
	public static final String OTHER_SSH_CONFIG = "StrictHostKeyChecking";
	/** SSH接続時KnownHostオプションの設定値 */
	public static final String OTHER_SSH_CONFIG_VAL = "no";

}

package com.wks.parts;

public class WksConstants {
	// ファイルパス
	public static final String PATH_EXEC_LOG = "/home/mini/wakasaba_orchestrator/log/log_%s.txt";
	public static final String PATH_DL_PAPERMC = "/home/mini/download/paper.jar";
	public static final String PATH_DL_PL3XMAP = "/home/mini/download/pl3xmap.jar";
	public static final String PATH_PROD_PAPERMC = "/home/mini/mcs/prod/paper.jar";
	public static final String PATH_PROD_PL3XMAP = "/home/mini/mcs/prod/plugins/pl3xmap.jar";
	public static final String PATH_BACKUP_SHELL = "/home/mini/mcs/shell/backup.sh";

	// Linuxコマンド
	public static final String CMD_UPDATE = "sudo apt update";
	public static final String CMD_UPGRADE = "sudo apt upgrade -y";
	public static final String CMD_SHUTDOWN = "sudo shutdown -r now";
	public static final String CMD_WGET_PAPERMC = "wget -O " + PATH_DL_PAPERMC + " --user-agent=\"%s\" %s";
	public static final String CMD_WGET_PL3XMAP = "wget -O " + PATH_DL_PL3XMAP + " --user-agent=\"%s\" %s";
	public static final String CMD_PAPERMC_HASH = "sha256sum " + PATH_DL_PAPERMC;
	public static final String CMD_PAPERMC_RM = "rm " + PATH_PROD_PAPERMC;
	public static final String CMD_PAPERMC_MV = "mv " + PATH_DL_PAPERMC + " " + PATH_PROD_PAPERMC;
	public static final String CMD_PL3XMAP_HASH = "sha512sum " + PATH_DL_PL3XMAP;
	public static final String CMD_PL3XMAP_RM = "rm " + PATH_PROD_PL3XMAP;
	public static final String CMD_PL3XMAP_MV = "mv " + PATH_DL_PL3XMAP + " " + PATH_PROD_PL3XMAP;
	public static final String CMD_PAPERMC_START = "sudo systemctl start papermc";
	public static final String CMD_PAPERMC_END = "sudo systemctl stop papermc";
	public static final String CMD_PAPERMC_BACKUP = "sh " + PATH_BACKUP_SHELL;
	public static final String CMD_DO_NOTHING = ":";
	public static final String CMD_SLEEP_SHUTDOWN = "(sleep 60 && sudo shutdown -r now) &";
	public static final String CMD_SHELL_HEAD = "sh";
	public static final String CMD_SHELL_OPTION = "-c";


	// ログメッセージ
	public static final String LOG_START_TIME = "Batch start time: %s";
	public static final String LOG_END_TIME = "Batch end time: %s";
	public static final String LOG_COMMAND = "$ %s";
	public static final String LOG_SPLIT = "/* --------Job start: %s-------- */";
	public static final String LOG_THIS_SERVER = "this server";

	// URLフォーマット
	public static final String URL_PAPERMC_VERSION = "https://fill.papermc.io/v3/projects/paper";
	public static final String URL_PAPERMC_DL_URL = "https://fill.papermc.io/v3/projects/paper/versions/%s/builds";
	public static final String URL_PL3XMAP_DL_URL = "https://api.modrinth.com/v2/project/pl3xmap/version";

	// JSON操作文字列
	public static final String JSON_PAPERMC_GV = "versions";
	public static final String JSON_PAPERMC_DL = "downloads";
	public static final String JSON_PAPERMC_SD = "server:default";
	public static final String JSON_PAPERMC_URL = "url";
	public static final String JSON_PAPERMC_CS = "checksums";
	public static final String JSON_PAPERMC_SHA = "sha256";
	public static final String JSON_PL3XMAP_GV = "game_versions";
	public static final String JSON_PL3XMAP_FILES = "files";
	public static final String JSON_PL3XMAP_URL = "url";
	public static final String JSON_PL3XMAP_HASH = "hashes";
	public static final String JSON_PL3XMAP_SHA = "sha512";
	public static final String JSON_REPLACE_DQ = "\"";
	public static final String JSON_REPLACE_ES = "";

	// その他
	public static final String ARGS_MSG = "The length of the arguments must be 3.";
	public static final String TIME_ZONE = "Asia/Tokyo";
	public static final String DATE_TIME_FMT = "yyyyMMddHHmmss";
	public static final String USER_AGENT = "wakasaba_orchestrator/1.0";
	public static final String SERVER_INFO = "{host: \"%s\", port: %d, user: \"%s\"}";
	public static final String CHANNEL_EXEC_OPTION = "exec";
	public static final String SSH_CONFIG = "StrictHostKeyChecking";
	public static final String SSH_CONFIG_VAL = "no";

}

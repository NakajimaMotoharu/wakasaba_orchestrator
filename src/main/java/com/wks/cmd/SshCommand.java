package com.wks.cmd;

import com.jcraft.jsch.JSchException;
import com.wks.main.Main;
import com.wks.papermc.PaperUrlGen;
import com.wks.parts.WksConstants;
import com.wks.util.ConnectionInformation;
import com.wks.util.Curl;
import com.wks.util.SshExec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SshCommand {
	/** logインスタンス取得 */
	private static final ArrayList<String> log = Main.log;

	/** updateコマンドの実行 */
	public static void update(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, WksConstants.CMD_UPDATE);
	}

	/** upgradeコマンドの実行 */
	public static void upgrade(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, WksConstants.CMD_UPGRADE);
	}

	/** shutdownコマンドの実行 */
	public static void shutdown(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, WksConstants.CMD_SHUTDOWN);
	}

	/** PaperMCクライアントのダウンロード実行 */
	public static void wgetPaperMc(ConnectionInformation ci) throws IOException, InterruptedException, JSchException {
		// ユーザエージェントの設定
		String userAgent = WksConstants.USER_AGENT;

		// 最新バージョンの取得
		String versionJson = Curl.exec(userAgent, WksConstants.URL_PAPERMC_VERSION);
		String version = PaperUrlGen.getPaperMcVersion(versionJson);

		// 最新バージョンのサーバクライアントのダウンロードURL取得
		String urlJson = Curl.exec(userAgent, String.format(WksConstants.URL_PAPERMC_DL_URL, version));
		String url = PaperUrlGen.getPaperMcUrl(urlJson);

		// wgetコマンド構成
		String cmd = String.format(WksConstants.CMD_WGET_PAPERMC, userAgent, url);

		// コマンド実行
		runCommand(ci, cmd);

		// Pl3xMapのダウンロードURL取得
		String pl3xMapUrlJson = Curl.exec(userAgent, WksConstants.URL_PL3XMAP_DL_URL);
		String pl3xMapUrl = PaperUrlGen.getPl3xMapUrl(pl3xMapUrlJson, version);

		if (pl3xMapUrl != null){
			// wgetコマンド構成
			String pl3xCmd = String.format(WksConstants.CMD_WGET_PL3XMAP, userAgent, pl3xMapUrl);

			// コマンド実行
			runCommand(ci, pl3xCmd);
		}
	}

	/** PaperMCクライアントの検証と移動 */
	public static void movePaperMc(ConnectionInformation ci) throws IOException, InterruptedException, JSchException {
		// ユーザエージェントの設定
		String userAgent = WksConstants.USER_AGENT;

		// 最新バージョンの取得
		String versionJson = Curl.exec(userAgent, WksConstants.URL_PAPERMC_VERSION);
		String version = PaperUrlGen.getPaperMcVersion(versionJson);

		// 最新バージョンのサーバクライアントのSHA256取得
		String sha256Json = Curl.exec(userAgent, String.format(WksConstants.URL_PAPERMC_DL_URL, version));
		String expectedSHA256 = PaperUrlGen.getPaperMcSha256(sha256Json);

		// 最新バージョンのPl3xMapのSHA256取得
		String sha512Json = Curl.exec(userAgent, WksConstants.URL_PL3XMAP_DL_URL);
		String expectedSHA512 = PaperUrlGen.getPl3xMapSha512(sha512Json, version);

		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		String cmd = WksConstants.CMD_PAPERMC_HASH;
		SshExec sshExec = new SshExec(ci, cmd);

		// コマンド実行
		String[] ret = sshExec.execute();

		// 実行コマンドをlogに出力
		log.add(String.format(WksConstants.LOG_COMMAND, cmd));

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));

		// SHA256検証(正常ファイルならファイルコピー実行)
		if (ret[0].substring(0, 64).equals(expectedSHA256)){
			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = WksConstants.CMD_PAPERMC_RM;
			sshExec = new SshExec(ci, cmd);

			// コマンド実行
			ret = sshExec.execute();

			// 実行コマンドをlogに出力
			log.add(String.format(WksConstants.LOG_COMMAND, cmd));

			// 返り値をログに追加
			log.addAll(Arrays.asList(ret));

			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = WksConstants.CMD_PAPERMC_MV;
			sshExec = new SshExec(ci, cmd);

			// コマンド実行
			ret = sshExec.execute();

			// 実行コマンドをlogに出力
			log.add(String.format(WksConstants.LOG_COMMAND, cmd));

			// 返り値をログに追加
			log.addAll(Arrays.asList(ret));
		}

		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		cmd = WksConstants.CMD_PL3XMAP_RM;
		sshExec = new SshExec(ci, cmd);

		// コマンド実行
		ret = sshExec.execute();

		// 実行コマンドをlogに出力
		log.add(String.format(WksConstants.LOG_COMMAND, cmd));

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));

		if (expectedSHA512 != null){
			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = WksConstants.CMD_PL3XMAP_HASH;
			sshExec = new SshExec(ci, cmd);

			// コマンド実行
			ret = sshExec.execute();

			// 実行コマンドをlogに出力
			log.add(String.format(WksConstants.LOG_COMMAND, cmd));

			// 返り値をログに追加
			log.addAll(Arrays.asList(ret));

			// SHA512検証(正常ファイルならファイルコピー実行)
			if (ret[0].substring(0, 128).equals(expectedSHA512)){
				// Active待機
				waitForBecomeActive(ci);

				// コマンド記述
				cmd = WksConstants.CMD_PL3XMAP_MV;
				sshExec = new SshExec(ci, cmd);

				// コマンド実行
				ret = sshExec.execute();

				// 実行コマンドをlogに出力
				log.add(String.format(WksConstants.LOG_COMMAND, cmd));

				// 返り値をログに追加
				log.addAll(Arrays.asList(ret));
			}
		}

	}

	/** PaperMC起動コマンドの実行 */
	public static void startPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, WksConstants.CMD_PAPERMC_START);
	}

	/** PaperMC停止コマンドの実行 */
	public static void stopPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, WksConstants.CMD_PAPERMC_END);
	}

	/** PaperMCバックアップシェルの実行 */
	public static void backupPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, WksConstants.CMD_PAPERMC_BACKUP);
	}

	/** 任意のコマンド実行 */
	private static void runCommand(ConnectionInformation ci, String cmd) throws JSchException, InterruptedException, IOException {
		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		SshExec sshExec = new SshExec(ci, cmd);

		// コマンド実行
		String[] ret = sshExec.execute();

		// 実行コマンドをlogに出力
		log.add(String.format(WksConstants.LOG_COMMAND, cmd));

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));
	}

	/** ci情報に接続可能になるまで待機 */
	private static void waitForBecomeActive(ConnectionInformation ci) throws JSchException, InterruptedException {
		// 何もしないコマンドを実行するsshExecを作成
		SshExec sshExec = new SshExec(ci, WksConstants.CMD_DO_NOTHING);

		// Activeになるまでループ
		while (!sshExec.isAlive()){
			Thread.sleep(1000);
		}
	}

}

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

/**
 * SSHコマンドの実行を行うラッパークラス
 */
public class SshCommand {
	/** logインスタンス取得 */
	private static final ArrayList<String> log = Main.log;

	/**
	 * updateコマンドの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void update(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		// updateコマンドの実行
		runCommand(ci, WksConstants.CMD_UPDATE);
	}

	/**
	 * upgradeコマンドの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void upgrade(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		// upgradeコマンドの実行
		runCommand(ci, WksConstants.CMD_UPGRADE);
	}

	/**
	 * shutdownコマンドの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void shutdown(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		// shutdownコマンドの実行
		runCommand(ci, WksConstants.CMD_SHUTDOWN);
	}

	/**
	 * PaperMCクライアントのダウンロード実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void wgetPaperMc(ConnectionInformation ci) throws IOException, InterruptedException, JSchException {
		// ユーザエージェントの設定
		String userAgent = WksConstants.OTHER_USER_AGENT;

		// 最新バージョンのJsonの取得
		String versionJson = Curl.exec(userAgent, WksConstants.URL_PAPERMC_VERSION);
		// 最新バージョンの値取得
		String version = PaperUrlGen.getPaperMcVersion(versionJson);

		// 最新バージョンのサーバクライアントのダウンロードJson取得
		String urlJson = Curl.exec(userAgent, String.format(WksConstants.URL_PAPERMC_DL_URL, version));
		// 最新バージョンのサーバクライアントのダウンロードURL取得
		String url = PaperUrlGen.getPaperMcUrl(urlJson);

		// wgetコマンド構成
		String cmd = String.format(WksConstants.CMD_WGET_PAPERMC, userAgent, url);

		// コマンド実行
		runCommand(ci, cmd);

		// Pl3xMapのダウンロードJson取得
		String pl3xMapUrlJson = Curl.exec(userAgent, WksConstants.URL_PL3XMAP_DL_URL);
		// Pl3xMapのダウンロードURL取得
		String pl3xMapUrl = PaperUrlGen.getPl3xMapUrl(pl3xMapUrlJson, version);

		// Pl3xMapの最新バージョンがPaperMCの最新バージョンが一致している場合以下を実行
		if (pl3xMapUrl != null){
			// wgetコマンド構成
			String pl3xCmd = String.format(WksConstants.CMD_WGET_PL3XMAP, userAgent, pl3xMapUrl);

			// コマンド実行
			runCommand(ci, pl3xCmd);
		}
	}

	/**
	 * PaperMCクライアントの検証と移動
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void movePaperMc(ConnectionInformation ci) throws IOException, InterruptedException, JSchException {
		// ユーザエージェントの設定
		String userAgent = WksConstants.OTHER_USER_AGENT;

		// 最新バージョンのJson取得
		String versionJson = Curl.exec(userAgent, WksConstants.URL_PAPERMC_VERSION);
		// 最新バージョンの値取得
		String version = PaperUrlGen.getPaperMcVersion(versionJson);

		// 最新バージョンのサーバクライアントのSHA256のJson取得
		String sha256Json = Curl.exec(userAgent, String.format(WksConstants.URL_PAPERMC_DL_URL, version));
		// 最新バージョンのサーバクライアントのSHA256の値取得
		String expectedSHA256 = PaperUrlGen.getPaperMcSha256(sha256Json);

		// 最新バージョンのPl3xMapのSHA512のJson取得
		String sha512Json = Curl.exec(userAgent, WksConstants.URL_PL3XMAP_DL_URL);
		// 最新バージョンのPl3xMapのSHA512の値取得
		String expectedSHA512 = PaperUrlGen.getPl3xMapSha512(sha512Json, version);

		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		String cmd = WksConstants.CMD_PAPERMC_HASH;
		// SSH実行インスタンスにコマンド設定
		SshExec sshExec = new SshExec(ci, cmd);

		// コマンド実行
		String[] ret = sshExec.execute();

		// 実行コマンドをlogに出力
		log.add(String.format(WksConstants.LOG_COMMAND, cmd));

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));

		// SHA256検証(正常ファイルならファイル移動実行)
		if (ret[0].substring(0, 64).equals(expectedSHA256)){
			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = WksConstants.CMD_PAPERMC_RM;
			// SSH実行インスタンスにコマンド設定
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
			// SSH実行インスタンスにコマンド設定
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
		// SSH実行インスタンスにコマンド設定
		sshExec = new SshExec(ci, cmd);

		// コマンド実行
		ret = sshExec.execute();

		// 実行コマンドをlogに出力
		log.add(String.format(WksConstants.LOG_COMMAND, cmd));

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));

		// Pl3xMapの最新バージョンが一致時実行
		if (expectedSHA512 != null){
			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = WksConstants.CMD_PL3XMAP_HASH;
			// SSH実行インスタンスにコマンド設定
			sshExec = new SshExec(ci, cmd);

			// コマンド実行
			ret = sshExec.execute();

			// 実行コマンドをlogに出力
			log.add(String.format(WksConstants.LOG_COMMAND, cmd));

			// 返り値をログに追加
			log.addAll(Arrays.asList(ret));

			// SHA512検証(正常ファイルならファイル移動実行)
			if (ret[0].substring(0, 128).equals(expectedSHA512)){
				// Active待機
				waitForBecomeActive(ci);

				// コマンド記述
				cmd = WksConstants.CMD_PL3XMAP_MV;
				// SSH実行インスタンスにコマンド設定
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

	/**
	 * PaperMC起動コマンドの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void startPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		// PaperMCを起動するコマンドを実行
		runCommand(ci, WksConstants.CMD_PAPERMC_START);
	}

	/**
	 * PaperMC停止コマンドの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void stopPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		// PaperMC停止コマンドの実行
		runCommand(ci, WksConstants.CMD_PAPERMC_END);
	}

	/**
	 * 1分間待機するコマンドの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void waitOneMin(ConnectionInformation ci) throws JSchException, IOException, InterruptedException {
		// 1分間待機するコマンドの実行
		runCommand(ci, WksConstants.CMD_WAIT_ONE_MIN);
	}

	/**
	 * PaperMCバックアップシェルの実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
	public static void backupPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		// PaperMCバックアップシェルの実行
		runCommand(ci, WksConstants.CMD_PAPERMC_BACKUP);
	}

	/**
	 * 任意のコマンド実行
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 * @throws IOException SSHコマンド標準出力取得に失敗
	 */
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

	/**
	 * ci情報に接続可能になるまで待機
	 *
	 * @param ci サーバ情報
	 * @throws JSchException SSHコマンド実行に失敗
	 * @throws InterruptedException SSHコマンド実行に失敗
	 */
	private static void waitForBecomeActive(ConnectionInformation ci) throws JSchException, InterruptedException {
		// 何もしないコマンドを実行するsshExecを作成
		SshExec sshExec = new SshExec(ci, WksConstants.CMD_DO_NOTHING);

		// Activeになるまでループ
		while (!sshExec.isAlive()){
			// Active出なければ1秒待機
			Thread.sleep(1000);
		}
	}

}

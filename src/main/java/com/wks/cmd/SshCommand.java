package com.wks.cmd;

import com.jcraft.jsch.JSchException;
import com.wks.main.Main;
import com.wks.papermc.PaperUrlGen;
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
		runCommand(ci, "sudo apt update");
	}

	/** upgradeコマンドの実行 */
	public static void upgrade(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, "sudo apt upgrade -y");
	}

	/** shutdownコマンドの実行 */
	public static void shutdown(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, "sudo shutdown -r now");
	}

	/** PaperMCクライアントのダウンロード実行 */
	public static void wgetPaperMc(ConnectionInformation ci) throws IOException, InterruptedException, JSchException {
		// ユーザエージェントの設定
		String userAgent = "wakasaba_orchestrator/1.0";

		// 最新バージョンの取得
		String versionJson = Curl.exec(userAgent, "https://fill.papermc.io/v3/projects/paper");
		String version = PaperUrlGen.getPaperMcVersion(versionJson);

		// 最新バージョンのサーバクライアントのダウンロードURL取得
		String urlJson = Curl.exec(userAgent, "https://fill.papermc.io/v3/projects/paper/versions/" + version + "/builds");
		String url = PaperUrlGen.getPaperMcUrl(urlJson);

		// wgetコマンド構成
		String cmd = "wget " +
				"-O /home/mini/download/paper.jar " +
				"--user-agent=\"" + userAgent + "\" " +
				url;

		// コマンド実行
		runCommand(ci, cmd);
	}

	/** PaperMCクライアントの検証と移動 */
	public static void movePaperMc(ConnectionInformation ci) throws IOException, InterruptedException, JSchException {
		// ユーザエージェントの設定
		String userAgent = "wakasaba_orchestrator/1.0";

		// 最新バージョンの取得
		String versionJson = Curl.exec(userAgent, "https://fill.papermc.io/v3/projects/paper");
		String version = PaperUrlGen.getPaperMcVersion(versionJson);

		// 最新バージョンのサーバクライアントのSHA256取得
		String sha256Json = Curl.exec(userAgent, "https://fill.papermc.io/v3/projects/paper/versions/" + version + "/builds");
		String expectedSHA256 = PaperUrlGen.getPaperMcSha256(sha256Json);

		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		String cmd = "sha256sum download/paper.jar";
		SshExec sshExec = new SshExec(ci, cmd);

		// コマンド実行
		String[] ret = sshExec.execute();

		// 実行コマンドをlogに出力
		log.add("& " + cmd);

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));

		// SHA256検証(正常ファイルならファイルコピー実行)
		if (ret[0].substring(0, 64).equals(expectedSHA256)){
			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = "rm /home/mini/mcs/prod/paper.jar";
			sshExec = new SshExec(ci, cmd);

			// コマンド実行
			ret = sshExec.execute();

			// 実行コマンドをlogに出力
			log.add("& " + cmd);

			// 返り値をログに追加
			log.addAll(Arrays.asList(ret));

			// Active待機
			waitForBecomeActive(ci);

			// コマンド記述
			cmd = "mv /home/mini/download/paper.jar /home/mini/mcs/prod/paper.jar";
			sshExec = new SshExec(ci, cmd);

			// コマンド実行
			ret = sshExec.execute();

			// 実行コマンドをlogに出力
			log.add("& " + cmd);

			// 返り値をログに追加
			log.addAll(Arrays.asList(ret));
		}

	}

	/** PaperMC起動コマンドの実行 */
	public static void startPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, "sudo systemctl start papermc");
	}

	/** PaperMC停止コマンドの実行 */
	public static void stopPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, "sudo systemctl stop papermc");
	}

	/** PaperMCバックアップシェルの実行 */
	public static void backupPaperMC(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		runCommand(ci, "sh /home/mini/mcs/shell/backup.sh");
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
		log.add("& " + cmd);

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));
	}

	/** ci情報に接続可能になるまで待機 */
	private static void waitForBecomeActive(ConnectionInformation ci) throws JSchException, InterruptedException {
		// 何もしないコマンドを実行するsshExecを作成
		SshExec sshExec = new SshExec(ci, ":");

		// Activeになるまでループ
		while (!sshExec.isAlive()){
			Thread.sleep(1000);
		}
	}

}

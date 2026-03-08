package com.wks.cmd;

import com.jcraft.jsch.JSchException;
import com.wks.util.ConnectionInformation;
import com.wks.util.SshExec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class SshCommand {

	private static final ArrayList<String> log = new ArrayList<>();

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

	/** 任意のコマンド実行 */
	public static void runCommand(ConnectionInformation ci, String cmd) throws JSchException, InterruptedException, IOException {
		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		SshExec sshExec = new SshExec(ci, cmd);

		// コマンド実行
		String[] ret = sshExec.execute();

		// 返り値をログに追加
		log.addAll(Arrays.asList(ret));
	}

	/** ログ追加 */
	public static void addLog(String msg){
		log.add(msg);
	}

	/** ログ出力 */
	public static void outLog(String path) throws FileNotFoundException {
		// 出力先指定
		PrintStream ps = new PrintStream(path);

		// logの中身を1行ずつ書き出す
		for (String s : log) {
			ps.println(s);
		}

		// 出力先クローズ
		ps.close();
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

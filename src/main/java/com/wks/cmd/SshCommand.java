package com.wks.cmd;

import com.jcraft.jsch.JSchException;
import com.wks.util.ConnectionInformation;
import com.wks.util.SshExec;

import java.io.IOException;

public class SshCommand {

	/** updateコマンドの実行 */
	public static String[] update(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		return runCommand(ci, "sudo apt update");
	}

	/** upgradeコマンドの実行 */
	public static String[] upgrade(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		return runCommand(ci, "sudo apt upgrade -y");
	}

	/** shutdownコマンドの実行 */
	public static String[] shutdown(ConnectionInformation ci) throws JSchException, InterruptedException, IOException {
		return runCommand(ci, "sudo shutdown -r now");
	}

	/** 任意のコマンド実行 */
	public static String[] runCommand(ConnectionInformation ci, String cmd) throws JSchException, InterruptedException, IOException {
		// Active待機
		waitForBecomeActive(ci);

		// コマンド記述
		SshExec sshExec = new SshExec(ci, cmd);

		// コマンド実行+返却
		return sshExec.execute();
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

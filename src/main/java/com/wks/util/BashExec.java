package com.wks.util;

import com.wks.main.Main;
import com.wks.parts.WksConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class BashExec {
	/** logインスタンス取得 */
	private static final ArrayList<String> log = Main.log;

	/** updateコマンドの実行 */
	public static void update() throws IOException, InterruptedException {
		runCommand(WksConstants.CMD_UPDATE);
	}

	/** upgradeコマンドの実行 */
	public static void upgrade() throws IOException, InterruptedException {
		runCommand(WksConstants.CMD_UPGRADE);
	}

	/** shutdownコマンドの実行 */
	public static void shutdown() throws IOException {
		// 60秒待機+シャットダウンのコマンドを作成
		String shutdownCmd = WksConstants.CMD_SLEEP_SHUTDOWN;

		// コマンドを実行
		String[] cmd = new String[]{WksConstants.CMD_SHELL_HEAD, WksConstants.CMD_SHELL_OPTION, shutdownCmd};

		// ProcessBuilderを指定のコマンドで作成
		ProcessBuilder processBuilder = new ProcessBuilder(cmd);

		// プロセス実行
		processBuilder.start();

		// logファイルにコマンド追記
		log.add(String.format(WksConstants.LOG_COMMAND, shutdownCmd));

		//出力を無視して終了
	}

	/** 任意のコマンド実行 */
	private static void runCommand(String cmd) throws IOException, InterruptedException {
		// ProcessBuilderを指定のコマンドで作成
		ProcessBuilder processBuilder =
				new ProcessBuilder(WksConstants.CMD_SHELL_HEAD, WksConstants.CMD_SHELL_OPTION, cmd);

		// プロセス実行
		Process process = processBuilder.start();
		// プロセス終了まで待機
		process.waitFor();

		// プロセス出力を受け取るBufferedReaderを作成
		InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		// 実行コマンドをログに追加
		log.add(String.format(WksConstants.LOG_COMMAND, cmd));

		// 出力を1行ずつlogに追加
		while (true){
			// 1行読み取り
			String line = bufferedReader.readLine();
			// これ以上読み取れなければ終了
			if (line == null){
				break;
			}
			// logに追加
			log.add(line);
		}

		// 各Readerをクローズ
		bufferedReader.close();
		inputStreamReader.close();
	}
}

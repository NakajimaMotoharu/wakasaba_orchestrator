package com.wks.util;

import com.wks.main.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class BashExec {
	private static final ArrayList<String> log = Main.log;

	public static void update() throws IOException, InterruptedException {
		runCommand("sudo apt update");
	}

	public static void upgrade() throws IOException, InterruptedException {
		runCommand("sudo apt upgrade -y");
	}

	public static void shutdown() throws IOException {
		// 10秒待機+シャットダウンのコマンドを実行
		String[] cmd = new String[]{"sh", "-c", "(sleep 60 && sudo shutdown -r now) &"};

		// ProcessBuilderを指定のコマンドで作成
		ProcessBuilder processBuilder = new ProcessBuilder(cmd);

		// プロセス実行
		processBuilder.start();

		//出力を無視して終了
	}

	public static void runCommand(String cmd) throws IOException, InterruptedException {
		// ProcessBuilderを指定のコマンドで作成
		ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", cmd);

		// プロセス実行
		Process process = processBuilder.start();
		// プロセス終了まで待機
		process.waitFor();

		// プロセス出力を受け取るBufferedReaderを作成
		InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

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

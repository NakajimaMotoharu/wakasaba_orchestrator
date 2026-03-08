package com.wks.main;

import com.jcraft.jsch.JSchException;
import com.wks.cmd.SshCommand;
import com.wks.util.ConnectionInformation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {
	public static final ArrayList<String> log = new ArrayList<>();

	public static void main(String[] args) throws IOException, InterruptedException, JSchException {
		if (args.length != 4){
			System.out.println("The length of the arguments must be four.");
		}

		ConnectionInformation ci = new ConnectionInformation(args[0], Integer.parseInt(args[1]), args[2], args[3]);

		addLog("# server1");
		SshCommand.wgetPaperMc(ci);
		outLog("./log.txt");
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
}

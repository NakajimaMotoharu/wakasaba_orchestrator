package com.wks.main;

import com.jcraft.jsch.JSchException;
import com.wks.workflow.WksWorkFlow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {
	public static final ArrayList<String> log = new ArrayList<>();

	public static void main(String[] args) throws IOException, InterruptedException, JSchException {
		if (args.length != 3){
			System.out.println("The length of the arguments must be 3.");
		}

		WksWorkFlow.execScheduledJob(args);
		outLog("./log.txt");

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

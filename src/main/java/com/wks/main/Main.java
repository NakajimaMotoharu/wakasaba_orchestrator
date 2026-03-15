package com.wks.main;

import com.jcraft.jsch.JSchException;
import com.wks.workflow.WksWorkFlow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main {
	public static final ArrayList<String> log = new ArrayList<>();

	public static void main(String[] args) throws IOException, InterruptedException, JSchException {
		if (args.length == 3){
			log.add("Batch start time: " + getDateTime());
			WksWorkFlow.execScheduledJob(args);
			log.add("Batch end time: " + getDateTime());
			outLog("./log/log_" + getDateTime() + ".txt");
		} else {
			System.out.println("The length of the arguments must be 3.");
		}

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

	/** 日時文字列取得 */
	private static String getDateTime(){
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		return ZonedDateTime.now(ZoneId.of("Asia/Tokyo")).format(dateTimeFormatter);
	}
}

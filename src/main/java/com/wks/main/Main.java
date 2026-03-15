package com.wks.main;

import com.jcraft.jsch.JSchException;
import com.wks.parts.WksConstants;
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
			log.add(String.format(WksConstants.LOG_START_TIME, getDateTime()));
			WksWorkFlow.execScheduledJob(args);
			log.add(String.format(WksConstants.LOG_END_TIME, getDateTime()));
			outLog(String.format(WksConstants.PATH_EXEC_LOG, getDateTime()));
		} else {
			System.out.println(WksConstants.OTHER_ARGS_MSG);
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
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(WksConstants.OTHER_DATE_TIME_FMT);
		return ZonedDateTime.now(ZoneId.of(WksConstants.OTHER_TIME_ZONE)).format(dateTimeFormatter);
	}
}

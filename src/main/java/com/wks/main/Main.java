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

/**
 * 実行クラス
 */
public class Main {
	/** ログ */
	public static final ArrayList<String> log = new ArrayList<>();

	/**
	 * エントリポイント
	 *
	 * @param args 3つ要求(すべてサーバの接続用データ)
	 * @throws IOException サーバファイル読み込み失敗、あるいはログファイル書き込み失敗
	 * @throws InterruptedException SSHコマンド実行中に失敗
	 * @throws JSchException SSHコマンド実行中に失敗
	 */
	public static void main(String[] args) throws IOException, InterruptedException, JSchException {
		if (args.length == 3){
			// 引数が3つあるとき正常実行開始
			// 開始ログ書き込み
			log.add(String.format(WksConstants.LOG_START_TIME, getDateTime()));
			// ワークフローに従い各サーバにアクセス・処理実行
			WksWorkFlow.execScheduledJob(args);
			// 終了ログ書き込み
			log.add(String.format(WksConstants.LOG_END_TIME, getDateTime()));
			// ログファイル出力
			outLog(String.format(WksConstants.PATH_EXEC_LOG, getDateTime()));
		} else {
			// 引数が3つないときは引数不足時のメッセージを出力し終了
			System.out.println(WksConstants.OTHER_ARGS_MSG);
		}

	}

	/**
	 * ログ出力
	 *
	 * @param path 出力先ファイルパス
	 * @throws FileNotFoundException ファイル出力失敗
	 */
	public static void outLog(String path) throws FileNotFoundException {
		// 出力先指定
		PrintStream ps = new PrintStream(path);

		// ログ各行に対してループ
		for (String s : log) {
			// logの中身を1行ずつ書き出す
			ps.println(s);
		}

		// 出力先クローズ
		ps.close();
	}

	/**
	 * 日時文字列取得
	 *
	 * @return メソッド実行時の日時をyyyyMMddhhmmss形式で返却
	 */
	private static String getDateTime(){
		// 出力フォーマット設定
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(WksConstants.OTHER_DATE_TIME_FMT);
		// タイムゾーンを東京でフォーマットにしたがい日時を返却
		return ZonedDateTime.now(ZoneId.of(WksConstants.OTHER_TIME_ZONE)).format(dateTimeFormatter);
	}
}

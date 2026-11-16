package com.wks.util;

import com.wks.parts.WksConstants;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AplLog extends Thread {

    /**
     * ログ格納用リスト
     */
    private final List<String> log = new CopyOnWriteArrayList<>();
    /**
     * ログ出力継続フラグ
     */
    private volatile boolean continuationFlag = true;
    /**
     * 出力先PrintStream
     */
    private final PrintStream ps;

    /**
     * コンストラクタ
     *
     * @throws FileNotFoundException 出力先ファイル書き込み失敗時例外
     */
    private AplLog() throws FileNotFoundException {
        // ログ出力用PrintStreamを初期化
        ps = new PrintStream(String.format(WksConstants.PATH_EXEC_LOG, getDateTime()));
    }

    /**
     * ログ出力用監視スレッド
     */
    @Override
    public void run() {
        // ログ継続フラグがtrueか、未出力ログが残っていれば継続
        while (continuationFlag || !log.isEmpty()) {
            // 未出力ログがなくなるまでログ出力
            while (!log.isEmpty()) {
                // ログリストの先頭を取り出して書き込み
                ps.print(log.removeFirst());
            }

            try {
                // 未出力ログがなくなったら1秒休止
                sleep(1000);
            } catch (InterruptedException e) {
                // 例外ログを出力
                outExceptionLog(WksConstants.OTHER_IP_LOCALHOST, e);
            }
        }

        // PrintStreamをクローズ
        ps.close();
    }

    /**
     * システムログを出力
     *
     * @param ip  発行元IP
     * @param log ログ内容
     */
    public void outSystemLog(String ip, String log) {
        // システムログを出力
        addLog(ip, WksConstants.LOG_LEVEL_SYSTEM, log);
    }

    /**
     * アプリログを出力
     *
     * @param ip  発行元IP
     * @param log ログ内容
     */
    public void outApplicationLog(String ip, String log) {
        // アプリログを出力
        addLog(ip, WksConstants.LOG_LEVEL_APPLICATIONS, log);
    }

    /**
     * コマンドログを出力
     *
     * @param ip  発行元IP
     * @param log ログ内容
     */
    public void outCommandLog(String ip, String log) {
        // コマンドログを出力
        addLog(ip, WksConstants.LOG_LEVEL_COMMAND, WksConstants.LOG_PARTS_DOL + log);
    }

    /**
     * コマンド出力ログを出力
     *
     * @param ip  発行元IP
     * @param log ログ内容
     */
    public void outOutputLog(String ip, String log) {
        // コマンド出力ログを出力
        addLog(ip, WksConstants.LOG_LEVEL_OUTPUT, log);
    }

    /**
     * コマンド出力ログを出力
     *
     * @param ip   発行元IP
     * @param logs ログ内容配列
     */
    public void outOutputLog(String ip, String[] logs) {
        // ログ配列各行に対して実行
        for (String s : logs) {
            // コマンド出力ログを出力
            addLog(ip, WksConstants.LOG_LEVEL_OUTPUT, s);
        }
    }

    /**
     * エラーログを出力
     *
     * @param ip  発行元IP
     * @param log ログ内容
     */
    public void outExceptionLog(String ip, String log) {
        // エラーログを出力
        addLog(ip, WksConstants.LOG_LEVEL_EXCEPTION, log);
    }

    /**
     * 例外ログを出力
     *
     * @param ip        発行元IP
     * @param exception 例外インスタンス
     */
    public void outExceptionLog(String ip, Exception exception) {
        // メモリ上に文字列を書き込むためのバッファ作成
        StringWriter sw = new StringWriter();
        // StringWriterに書き込むためのPrintWriterを作成
        PrintWriter pw = new PrintWriter(sw);
        // スタックトレースをPrintWriterへ出力
        exception.printStackTrace(pw);
        // 出力をリストに変換
        List<String> lines = sw.toString().lines().toList();

        // stackTrace各行に対して実行
        for (String line : lines) {
            // 各行を例外ログとして出力
            outExceptionLog(ip, line);
        }
    }

    /**
     * ログ出力を停止
     */
    public void endOfLogOutput() {
        continuationFlag = false;
    }

    /**
     * ログ追加部品
     *
     * @param ip          発行元IP
     * @param logLevel    ログレベル
     * @param logContents ログ内容
     */
    private void addLog(String ip, String logLevel, String logContents) {
        // ログ出力が停止されている場合は追加不可
        if (!continuationFlag) return;
        // 現在時刻を取得
        String nowTime = getDateTime();
        // フォーマットに従いログリストに追記
        log.add(String.format(WksConstants.LOG_FORMAT, nowTime, ip, logLevel, logContents));
    }

    /**
     * 日時文字列取得
     *
     * @return メソッド実行時の日時をyyyyMMddHHmmss形式で返却
     */
    private static String getDateTime() {
        // 出力フォーマット設定
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(WksConstants.OTHER_DATE_TIME_FMT);
        // タイムゾーンを東京でフォーマットにしたがい日時を返却
        return ZonedDateTime.now(ZoneId.of(WksConstants.OTHER_TIME_ZONE)).format(dateTimeFormatter);
    }

    /**
     * AplLogインスタンス格納用変数
     */
    private static AplLog instance = null;

    /**
     * AplLogインスタンスを取得
     *
     * @return AplLogインスタンス
     * @throws FileNotFoundException 出力先ファイル書き込み失敗時例外
     */
    public static synchronized AplLog getInstance() throws FileNotFoundException {
        // すでにインスタンス作成済みの場合、作成済みインスタンスを返却
        if (instance != null) return instance;
        // インスタンス新規作成
        instance = new AplLog();
        // インスタンスのログ出力を開始
        instance.start();
        // 作成したインスタンスを返却
        return instance;
    }
}

package com.wks.main;

import com.wks.parts.WksConstants;
import com.wks.util.AplLog;
import com.wks.workflow.WksWorkFlow;

import java.io.IOException;

/**
 * 実行クラス
 */
public class Main {
    /**
     * エントリポイント
     *
     * @param args 6つ要求(すべてサーバの接続用データ)
     * @throws IOException          ログファイル書き込み失敗
     * @throws InterruptedException ログファイル書き込み失敗
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 6) {
            // 引数が6つあるとき正常実行開始
            // 開始ログ書き込み
            AplLog.getInstance().outSystemLog(WksConstants.OTHER_IP_LOCALHOST, WksConstants.LOG_START_MESSAGE);

            try {
                // ワークフローに従い各サーバにアクセス・処理実行
                WksWorkFlow.execScheduledJob(args);
            } catch (Exception e) {
                // 例外をログに書き込み
                AplLog.getInstance().outExceptionLog(WksConstants.OTHER_IP_LOCALHOST, e);
            }
            // 終了ログ書き込み
            AplLog.getInstance().outSystemLog(WksConstants.OTHER_IP_LOCALHOST, WksConstants.LOG_FINISH_MESSAGE);
            // ログファイルクロージング
            AplLog.getInstance().endOfLogOutput();
            // ログ書込完了待合
            AplLog.getInstance().join();
        } else {
            // 引数が6つないときは引数不足時のメッセージを出力し終了
            System.out.println(WksConstants.OTHER_ARGS_MSG);
        }

    }
}

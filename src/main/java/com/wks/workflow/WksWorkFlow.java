package com.wks.workflow;

import com.jcraft.jsch.JSchException;
import com.wks.cmd.SshCommand;
import com.wks.main.Main;
import com.wks.parts.WksConstants;
import com.wks.util.BashExec;
import com.wks.util.ConnectionInformation;

import java.io.IOException;
import java.util.ArrayList;

/**
 * サーバ操作ワークフロークラス
 */
public class WksWorkFlow {
    /**
     * logインスタンス取得
     */
    private static final ArrayList<String> log = Main.log;

    /**
     * スケジュールに従い実行
     *
     * @param servers サーバ情報ファイルパス
     * @throws IOException          SSH・BASH実行失敗時
     * @throws InterruptedException SSH・BASH実行失敗時
     * @throws JSchException        SSH・BASH実行失敗時
     */
    public static void execScheduledJob(String[] servers) throws IOException, InterruptedException, JSchException {
        // サーバ0
        // サーバ0の接続情報を設定
        ConnectionInformation ci0 = ConnectionInformation.getCiFromFile(servers[0]);
        // サーバ0の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci0));
        // ci0がアクティブであれば実行
        if (SshCommand.isAlive(ci0)){
            // updateコマンド実行
            SshCommand.update(ci0);
            // upgradeコマンド実行
            SshCommand.upgrade(ci0);
            // shutdownコマンド実行
            SshCommand.shutdown(ci0);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }

        // サーバ1
        // サーバ1の接続情報を設定
        ConnectionInformation ci1 = ConnectionInformation.getCiFromFile(servers[1]);
        // サーバ1の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci1));
        // ci1がアクティブであれば実行
        if (SshCommand.isAlive(ci1)){
            // PaperMC停止コマンドの実行
            SshCommand.stopPaperMC(ci1);
            // 安全停止のため1分間待機
            SshCommand.waitOneMin(ci1);
            // updateコマンドの実行
            SshCommand.update(ci1);
            // upgradeコマンドの実行
            SshCommand.upgrade(ci1);
            // バックアップシェルの実行
            SshCommand.backupPaperMC(ci1);
            // PaperMCサーバクライアントの取得
            SshCommand.wgetPaperMc(ci1);
            // PaperMCサーバクライアントの配置
            SshCommand.movePaperMc(ci1);
            // shutdownコマンドの実行
            SshCommand.shutdown(ci1);
            // PaperMC起動コマンドの実行
            SshCommand.startPaperMC(ci1);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }

        // サーバ2
        // サーバ2の接続情報を設定
        ConnectionInformation ci2 = ConnectionInformation.getCiFromFile(servers[2]);
        // サーバ2の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci2));
        // ci2がアクティブであれば実行
        if (SshCommand.isAlive(ci2)){
            // updateコマンドの実行
            SshCommand.update(ci2);
            // upgradeコマンドの実行
            SshCommand.upgrade(ci2);
            // shutdownコマンドの実行
            SshCommand.shutdown(ci2);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }

        // サーバ3
        // サーバ3の接続情報を設定
        ConnectionInformation ci3 = ConnectionInformation.getCiFromFile(servers[3]);
        // サーバ3の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci3));
        // ci3がアクティブであれば実行
        if (SshCommand.isAlive(ci3)){
            // Schubert停止コマンドの実行
            SshCommand.stopSchubert(ci3);
            // 安全停止のため1分間待機
            SshCommand.waitOneMin(ci3);
            // updateコマンドの実行
            SshCommand.update(ci3);
            // upgradeコマンドの実行
            SshCommand.upgrade(ci3);
            // shutdownコマンドの実行
            SshCommand.shutdown(ci3);
            // Schubert起動コマンドの実行
            SshCommand.startSchubert(ci3);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }

        // サーバ4
        // サーバ4の接続情報を設定
        ConnectionInformation ci4 = ConnectionInformation.getCiFromFile(servers[4]);
        // サーバ4の接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, ci4));
        // ci4がアクティブであれば実行
        if (SshCommand.isAlive(ci4)){
            // updateコマンド実行
            SshCommand.update(ci4);
            // upgradeコマンド実行
            SshCommand.upgrade(ci4);
            // shutdownコマンド実行
            SshCommand.shutdown(ci4);
        } else {
            log.add(WksConstants.OTHER_NOT_ALIVE_MSG);
        }

        // 自サーバ
        // 自サーバの接続情報をログに設定
        log.add(String.format(WksConstants.LOG_SPLIT, WksConstants.LOG_THIS_SERVER));
        // updateコマンドの実行
        BashExec.update();
        // upgradeコマンドの実行
        BashExec.upgrade();
        // shutdownコマンドの実行
        BashExec.shutdown();
    }
}

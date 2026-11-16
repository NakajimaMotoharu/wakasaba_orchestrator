package com.wks.workflow;

import com.jcraft.jsch.JSchException;
import com.wks.cmd.SshCommand;
import com.wks.parts.WksConstants;
import com.wks.util.AplLog;
import com.wks.util.BashExec;
import com.wks.util.ConnectionInformation;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * サーバ操作ワークフロークラス
 */
public class WksWorkFlow {
    /**
     * スケジュールに従い実行
     *
     * @param servers サーバ情報ファイルパス
     * @throws IOException          SSH・BASH実行失敗時
     * @throws InterruptedException SSH・BASH実行失敗時
     * @throws JSchException        SSH・BASH実行失敗時
     */
    public static void execScheduledJob(String[] servers) throws IOException, InterruptedException, JSchException {

        // サーバの接続情報を取得
        ConnectionInformation[] cis = setConnectionInformation(servers);

        // サーバが全て接続可能か確認
        if (!isAllAlive(cis)) {
            // 接続不可能である警告メッセージをログに出力
            AplLog.getInstance().outExceptionLog(WksConstants.OTHER_IP_LOCALHOST, WksConstants.OTHER_NOT_ALIVE_MSG);
            // 呼び出し元に復帰する
            return;
        }

        // システム停止
        systemStop(cis);

        // NASマウント解除
        allUMount(cis);

        // サーバアップデート
        allUpdate(cis);

        // サーバ再起動
        allReboot(cis);

        // 再起動後固定待機
        BashExec.waitOneMin();

        // サーバ再起動完了待機
        allRebootComplete(cis);

        // NASマウント
        allMount(cis);

        // システム起動前処理
        systemPreSetup(cis);

        // システム起動
        systemStart(cis);

        // 管理サーバメンテ
        // updateコマンドの実行
        BashExec.update();
        // upgradeコマンドの実行
        BashExec.upgrade();
        // shutdownコマンドの実行
        BashExec.shutdown();
    }

    /**
     * サーバの接続情報を一括設定
     *
     * @param servers サーバ情報ファイルパス
     * @return サーバ接続情報配列
     * @throws IOException ファイル読み込み失敗
     */
    private static ConnectionInformation[] setConnectionInformation(String[] servers) throws IOException {
        // 返却用サーバ接続情報配列を初期化
        ConnectionInformation[] cis = new ConnectionInformation[servers.length];

        // サーバ情報ファイルの数だけ設定
        for (int i = 0; i < servers.length; i++) {
            // 各サーバに対して接続情報を取得
            cis[i] = ConnectionInformation.getCiFromFile(servers[i]);
        }

        // 設定済みサーバ接続情報を返却
        return cis;
    }

    /**
     * サーバが全機疎通可能状態か確認
     *
     * @param cis サーバ接続情報配列
     * @return 接続状態
     * @throws JSchException         セッションインスタンス生成失敗
     * @throws FileNotFoundException ログインスタンス読み込み失敗
     */
    private static boolean isAllAlive(ConnectionInformation[] cis) throws JSchException, FileNotFoundException {
        // 各サーバ情報に対して実行
        for (ConnectionInformation ci : cis) {
            // 検証先をログ出力
            AplLog.getInstance().outApplicationLog(ci.host(), WksConstants.OTHER_CONNECTION_CHECK);
            // サーバが接続可能状態か判定
            if (!SshCommand.isAlive(ci)) {
                // 1台でも接続不可能なマシンがあればfalseを返却
                return false;
            }
        }
        // 全機接続可能であればtrueを返却
        return true;
    }

    /**
     * サーバ全機再起動
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void allReboot(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // 全機に対してコマンド実行
        for (ConnectionInformation ci : cis) {
            // 再起動コマンドの実行
            SshCommand.shutdown(ci);
        }
    }

    /**
     * サーバ全機再起動完了を待機
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     * @throws IOException          Bashコマンド実行に失敗
     */
    private static void allRebootComplete(ConnectionInformation[] cis) throws JSchException, InterruptedException, IOException {
        // 全機に対してコマンド実行
        for (ConnectionInformation ci : cis) {
            // 疎通可能になるまで待機
            SshCommand.waitForBecomeActive(ci, WksConstants.SERVER_REBOOT_WAIT_LIMIT);
        }
    }

    /**
     * サーバ全機更新
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void allUpdate(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // 並列化用UpdateThreadの配列を作成
        UpdateThread[] updateThread = new UpdateThread[cis.length];

        // 各サーバについて実行
        for (int i = 0; i < cis.length; i++) {
            // Threadをサーバ情報を用いて初期化
            updateThread[i] = new UpdateThread(cis[i]);
            // Updateを並列実行
            updateThread[i].start();
        }

        // 各UpdateThreadに対して実行
        for (UpdateThread thread : updateThread) {
            // 各サーバのUpdateが完了するまで待機
            thread.join();
        }

        // 各UpdateThreadに対して実行
        for (UpdateThread thread : updateThread) {
            // Updateで例外が発生していた場合吸収
            thread.exceptionDetection();
        }
    }

    /**
     * サーバnas以外全機マウント実行
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void allMount(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // 全機に対してコマンド実行
        for (int i = 0; i < cis.length; i++) {
            // 実行対象がnasでない場合実行
            if (i != WksConstants.SERVER_NAS) {
                SshCommand.mount(cis[i]);
            }
        }
    }

    /**
     * サーバnas以外全機アンマウント実行
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void allUMount(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // 全機に対してコマンド実行
        for (int i = 0; i < cis.length; i++) {
            // 実行対象がnasでない場合実行
            if (i != WksConstants.SERVER_NAS) {
                SshCommand.umount(cis[i]);
            }
        }
    }

    /**
     * システム停止
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void systemStop(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // PaperMCサーバ システム停止
        SshCommand.stopPaperMC(cis[WksConstants.SERVER_PAPER]);

        // Schubertサーバ システム停止
        SshCommand.stopSchubert(cis[WksConstants.SERVER_SCHUBERT]);

        // 安全停止のため1分間待機
        BashExec.waitOneMin();
    }

    /**
     * システム起動前処理
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void systemPreSetup(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // PaperMCサーバ 起動前処理
        // バックアップシェルの実行
        SshCommand.backupPaperMC(cis[WksConstants.SERVER_PAPER]);
        // PaperMCサーバクライアントの取得
        SshCommand.wgetPaperMc(cis[WksConstants.SERVER_PAPER]);
        // PaperMCサーバクライアントの配置
        SshCommand.movePaperMc(cis[WksConstants.SERVER_PAPER]);
    }

    /**
     * システム起動
     *
     * @param cis サーバ接続情報配列
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws IOException          SSHコマンド標準出力取得に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     */
    private static void systemStart(ConnectionInformation[] cis) throws JSchException, IOException, InterruptedException {
        // PaperMCサーバ システム起動
        SshCommand.startPaperMC(cis[WksConstants.SERVER_PAPER]);
        // Schubert起動コマンドの実行
        SshCommand.startSchubert(cis[WksConstants.SERVER_SCHUBERT]);
    }
}

/**
 * Update並列化用スレッド
 */
class UpdateThread extends Thread {
    /**
     * サーバ情報
     */
    private final ConnectionInformation ci;
    /**
     * JSchExceptionが発生していた場合の一時格納先
     */
    private JSchException jSchException = null;
    /**
     * IOExceptionが発生していた場合の一時格納先
     */
    private IOException ioException = null;
    /**
     * InterruptedExceptionが発生していた場合の一時格納先
     */
    private InterruptedException interruptedException = null;
    /**
     * その他Exceptionが発生していた場合の一時格納先
     */
    private Exception exception = null;

    /**
     * コンストラクタ サーバ情報を用いて初期化
     *
     * @param ci サーバ情報
     */
    public UpdateThread(ConnectionInformation ci) {
        // サーバ情報フィールドを引数で初期化
        this.ci = ci;
    }

    /**
     * Updateの実行
     */
    @Override
    public void run() {
        try {
            // UPDATEコマンドの実行
            SshCommand.update(ci);
            // UPGRADEコマンドの実行
            SshCommand.upgrade(ci);
        } catch (JSchException e) {
            // JSchExceptionが発生した場合例外格納
            jSchException = e;
        } catch (IOException e) {
            // IOExceptionが発生した場合例外格納
            ioException = e;
        } catch (InterruptedException e) {
            // InterruptedExceptionが発生した場合例外格納
            interruptedException = e;
        } catch (Exception e) {
            // その他Exceptionが発生した場合例外格納
            exception = e;
        }
    }

    /**
     * 例外が発生していた場合、throwsする
     *
     * @throws JSchException        SSHコマンド実行に失敗
     * @throws InterruptedException SSHコマンド実行に失敗
     * @throws IOException          Bashコマンド実行に失敗
     */
    public void exceptionDetection() throws JSchException, IOException, InterruptedException {
        // JSchExceptionが発生していた場合throws
        if (jSchException != null) throw jSchException;
        // IOExceptionが発生していた場合throws
        if (ioException != null) throw ioException;
        // InterruptedExceptionが発生していた場合throws
        if (interruptedException != null) throw interruptedException;
        // その他Exceptionが発生していた場合throws
        if (exception != null) throw new RuntimeException(exception);
    }
}

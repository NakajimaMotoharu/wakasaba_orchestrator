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
	/** logインスタンス取得 */
	private static final ArrayList<String> log = Main.log;

	/**
	 * スケジュールに従い実行
	 *
	 * @param servers サーバ情報ファイルパス
	 * @throws IOException SSH・BASH実行失敗時
	 * @throws InterruptedException SSH・BASH実行失敗時
	 * @throws JSchException SSH・BASH実行失敗時
	 */
	public static void execScheduledJob(String[] servers) throws IOException, InterruptedException, JSchException {
		// サーバ0
		// サーバ0の接続情報を設定
		ConnectionInformation ci1 = ConnectionInformation.getCiFromFile(servers[0]);
		// サーバ0の接続情報をログに設定
		log.add(String.format(WksConstants.LOG_SPLIT, ci1));
		// updateコマンド実行
		SshCommand.update(ci1);
		// upgradeコマンド実行
		SshCommand.upgrade(ci1);
		// shutdownコマンド実行
		SshCommand.shutdown(ci1);

		// サーバ1
		// サーバ1の接続情報を設定
		ConnectionInformation ci2 = ConnectionInformation.getCiFromFile(servers[1]);
		// サーバ1の接続情報をログに設定
		log.add(String.format(WksConstants.LOG_SPLIT, ci2));
		// PaperMC停止コマンドの実行
		SshCommand.stopPaperMC(ci2);
		// 安全停止のため1分間待機
		SshCommand.waitOneMin(ci2);
		// updateコマンドの実行
		SshCommand.update(ci2);
		// upgradeコマンドの実行
		SshCommand.upgrade(ci2);
		// バックアップシェルの実行
		SshCommand.backupPaperMC(ci2);
		// PaperMCサーバクライアントの取得
		SshCommand.wgetPaperMc(ci2);
		// PaperMCサーバクライアントの配置
		SshCommand.movePaperMc(ci2);
		// shutdownコマンドの実行
		SshCommand.shutdown(ci2);
		// PaperMC起動コマンドの実行
		SshCommand.startPaperMC(ci2);

		// サーバ2
		// サーバ2の接続情報を設定
		ConnectionInformation ci3 = ConnectionInformation.getCiFromFile(servers[2]);
		// サーバ2の接続情報をログに設定
		log.add(String.format(WksConstants.LOG_SPLIT, ci3));
		// updateコマンドの実行
		SshCommand.update(ci3);
		// upgradeコマンドの実行
		SshCommand.upgrade(ci3);
		// shutdownコマンドの実行
		SshCommand.shutdown(ci3);

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

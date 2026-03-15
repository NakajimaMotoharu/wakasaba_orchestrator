package com.wks.workflow;

import com.jcraft.jsch.JSchException;
import com.wks.cmd.SshCommand;
import com.wks.main.Main;
import com.wks.parts.WksConstants;
import com.wks.util.BashExec;
import com.wks.util.ConnectionInformation;

import java.io.IOException;
import java.util.ArrayList;

public class WksWorkFlow {
	/** logインスタンス取得 */
	private static final ArrayList<String> log = Main.log;

	public static void execScheduledJob(String[] servers) throws IOException, InterruptedException, JSchException {
		// サーバ0
		ConnectionInformation ci1 = ConnectionInformation.getCiFromFile(servers[0]);
		log.add(String.format(WksConstants.LOG_SPLIT, ci1));
		SshCommand.update(ci1);
		SshCommand.upgrade(ci1);
		SshCommand.shutdown(ci1);

		// サーバ1
		ConnectionInformation ci2 = ConnectionInformation.getCiFromFile(servers[1]);
		log.add(String.format(WksConstants.LOG_SPLIT, ci2));
		SshCommand.stopPaperMC(ci2);
		SshCommand.update(ci2);
		SshCommand.upgrade(ci2);
		SshCommand.backupPaperMC(ci2);
		SshCommand.wgetPaperMc(ci2);
		SshCommand.movePaperMc(ci2);
		SshCommand.shutdown(ci2);
		SshCommand.startPaperMC(ci2);

		// サーバ2
		ConnectionInformation ci3 = ConnectionInformation.getCiFromFile(servers[2]);
		log.add(String.format(WksConstants.LOG_SPLIT, ci3));
		SshCommand.update(ci3);
		SshCommand.upgrade(ci3);
		SshCommand.shutdown(ci3);

		// 自サーバ
		log.add(String.format(WksConstants.LOG_SPLIT, WksConstants.LOG_THIS_SERVER));
		BashExec.update();
		BashExec.upgrade();
		BashExec.shutdown();
	}
}

package com.wks.util;

import com.jcraft.jsch.JSch;

public class SshExec {
	public static void test(){
		JSch jSch = new JSch();

		System.out.println("TEST" + jSch);
	}

	private final ConnectionInformation ci;
	private final String cmd;
	private boolean executed;

	public SshExec(ConnectionInformation ci, String cmd){
		this.ci = ci;
		this.cmd = cmd;
		executed = false;
	}
}

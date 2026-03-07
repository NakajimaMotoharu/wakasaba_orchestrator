package com.wks.util;

import com.jcraft.jsch.JSch;

public class SshExec {
	public static void test(){
		JSch jSch = new JSch();

		System.out.println("TEST" + jSch);
	}

	public SshExec(ConnectionInformation ci, String smd){

	}
}

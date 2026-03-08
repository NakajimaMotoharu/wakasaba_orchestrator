package com.wks.main;

import com.jcraft.jsch.JSchException;
import com.wks.cmd.SshCommand;
import com.wks.util.ConnectionInformation;

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws JSchException, IOException, InterruptedException {
		if (args.length != 4){
			System.out.println("The length of the arguments must be four.");
		}

		ConnectionInformation ci = new ConnectionInformation(args[0], Integer.parseInt(args[1]), args[2], args[3]);
		String[] ret = SshCommand.runCommand(ci, "ls");

		for (String s : ret) {
			System.out.println(s);
		}
	}
}

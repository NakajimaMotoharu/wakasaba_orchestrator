package com.wks.main;

import com.wks.util.Curl;
import com.wks.util.SshExec;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		try {
			System.out.println(Curl.exec("wakasaba_orchestrator/1.0", "https://fill.papermc.io/v3/projects/paper"));
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}

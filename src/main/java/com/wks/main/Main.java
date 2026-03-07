package com.wks.main;

import com.wks.papermc.PaperUrlGen;
import com.wks.util.Curl;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		try {
			String json1 = Curl.exec("wakasaba_orchestrator/1.0", "https://fill.papermc.io/v3/projects/paper");
			String version = PaperUrlGen.getPaperMcVersion(json1);

			String json2 = Curl.exec("wakasaba_orchestrator/1.0", "https://fill.papermc.io/v3/projects/paper/versions/" + version + "/builds");
			String url = PaperUrlGen.getPaperMcUrl(json2);

			String sha256 = PaperUrlGen.getPaperMcSha256(json2);

			System.out.println(url);
			System.out.println(sha256);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}

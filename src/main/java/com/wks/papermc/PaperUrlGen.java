package com.wks.papermc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class PaperUrlGen {
	/** PaperMCの最新バージョンを取得するルーチン */
	public static String getPaperMcVersion(String json){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode majorVersion = src.get("versions");
		// メジャーバージョンの中身がコレクションのため、一度リストに変換してから先頭を取得
		JsonNode fullVersion = majorVersion.get(List.copyOf(majorVersion.propertyNames()).getFirst());

		return fullVersion.get(0).toString().replace("\"", "");

	}

	/** PaperMCの最新バージョンのURLを取得するルーチン */
	public static String getPaperMcUrl(String json){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode downloadInfo = src.get(0).get("downloads");
		JsonNode serverInfo = downloadInfo.get("server:default");
		JsonNode url = serverInfo.get("url");

		return url.toString().replace("\"", "");
	}

	/** PaperMCの最新バージョンのURLのSHA-256を取得するルーチン */
	public static String getPaperMcSha256(String json){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode downloadInfo = src.get(0).get("downloads");
		JsonNode serverInfo = downloadInfo.get("server:default");
		JsonNode checkSums = serverInfo.get("checksums");
		JsonNode sha256 = checkSums.get("sha256");

		return sha256.toString().replace("\"", "");

	}

	public static  String getPl3xMapVersion(String[] json){
		return null;
	}
}

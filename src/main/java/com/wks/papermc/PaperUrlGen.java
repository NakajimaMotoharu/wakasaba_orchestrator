package com.wks.papermc;

import com.wks.parts.WksConstants;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class PaperUrlGen {
	/** PaperMCの最新バージョンを取得するルーチン */
	public static String getPaperMcVersion(String json){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode majorVersion = src.get(WksConstants.JSON_PAPERMC_GV);
		// メジャーバージョンの中身がコレクションのため、一度リストに変換してから先頭を取得
		JsonNode fullVersion = majorVersion.get(List.copyOf(majorVersion.propertyNames()).getFirst());

		return fullVersion.get(0).toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);

	}

	/** PaperMCの最新バージョンのURLを取得するルーチン */
	public static String getPaperMcUrl(String json){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode downloadInfo = src.get(0).get(WksConstants.JSON_PAPERMC_DL);
		JsonNode serverInfo = downloadInfo.get(WksConstants.JSON_PAPERMC_SD);
		JsonNode url = serverInfo.get(WksConstants.JSON_PAPERMC_URL);

		return url.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
	}

	/** PaperMCの最新バージョンのURLのSHA-256を取得するルーチン */
	public static String getPaperMcSha256(String json){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode downloadInfo = src.get(0).get(WksConstants.JSON_PAPERMC_DL);
		JsonNode serverInfo = downloadInfo.get(WksConstants.JSON_PAPERMC_SD);
		JsonNode checkSums = serverInfo.get(WksConstants.JSON_PAPERMC_CS);
		JsonNode sha256 = checkSums.get(WksConstants.JSON_PAPERMC_SHA);

		return sha256.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);

	}

	public static String getPl3xMapUrl(String json, String version){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode overView = src.get(0);

		if (!version.equals(overView.get(WksConstants.JSON_PL3XMAP_GV).get(0).
				toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES))){
			return null;
		}

		JsonNode fileInfo = overView.get(WksConstants.JSON_PL3XMAP_FILES);
		JsonNode urlInfo = fileInfo.get(0).get(WksConstants.JSON_PL3XMAP_URL);

		return urlInfo.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
	}

	public static String getPl3xMapSha512(String json, String version){

		ObjectMapper om = new ObjectMapper();
		JsonNode src = om.readTree(json);
		JsonNode overView = src.get(0);

		if (!version.equals(overView.get(WksConstants.JSON_PL3XMAP_GV).get(0).
				toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES))){
			return null;
		}

		JsonNode fileInfo = overView.get(WksConstants.JSON_PL3XMAP_FILES);
		JsonNode sha512Info = fileInfo.get(0).get(WksConstants.JSON_PL3XMAP_HASH).get(WksConstants.JSON_PL3XMAP_SHA);

		return sha512Info.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
	}
}

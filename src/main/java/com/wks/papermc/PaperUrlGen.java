package com.wks.papermc;

import com.wks.parts.WksConstants;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public class PaperUrlGen {
	/**
	 * PaperMCの最新バージョンを取得するルーチン
	 *
	 * @param json APIから取得したJSONデータ
	 * @return 最新バージョンの値
	 */
	public static String getPaperMcVersion(String json){
		// jsonの解析用インスタンス作成
		ObjectMapper om = new ObjectMapper();
		// APIから取得したJSONデータのノード作成
		JsonNode src = om.readTree(json);
		// メジャーバージョンのjsonノード抽出
		JsonNode majorVersion = src.get(WksConstants.JSON_PAPERMC_GV);
		// メジャーバージョンの中身がコレクションのため、一度リストに変換してから先頭を取得
		JsonNode fullVersion = majorVersion.get(List.copyOf(majorVersion.propertyNames()).getFirst());
		// バージョン情報からダブルクオーテーションを除去して返却
		return fullVersion.get(0).toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);

	}

	/**
	 * PaperMCの最新バージョンのURLを取得するルーチン
	 *
	 * @param json APIから取得したJSONデータ
	 * @return PaperMCの最新バージョンのダウンロードURL
	 */
	public static String getPaperMcUrl(String json){
		// jsonの解析用インスタンス作成
		ObjectMapper om = new ObjectMapper();
		// APIから取得したJSONデータのノード作成
		JsonNode src = om.readTree(json);
		// ダウンロード情報のjsonを抽出
		JsonNode downloadInfo = src.get(0).get(WksConstants.JSON_PAPERMC_DL);
		// サーバ情報のjsonを抽出
		JsonNode serverInfo = downloadInfo.get(WksConstants.JSON_PAPERMC_SD);
		// url情報のjsonを抽出
		JsonNode url = serverInfo.get(WksConstants.JSON_PAPERMC_URL);
		// url情報からダブルクオーテーションを除去して返却
		return url.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
	}

	/**
	 * PaperMCの最新バージョンのURLのSHA-256を取得するルーチン
	 *
	 * @param json APIから取得したJSONデータ
	 * @return PaperMCの最新バージョンのサーバクライアントのSHA256期待値データ
	 */
	public static String getPaperMcSha256(String json){
		// jsonの解析用インスタンス作成
		ObjectMapper om = new ObjectMapper();
		// APIから取得したJSONデータのノード作成
		JsonNode src = om.readTree(json);
		// jsonからダウンロード情報を抽出
		JsonNode downloadInfo = src.get(0).get(WksConstants.JSON_PAPERMC_DL);
		// サーバ情報のjsonを抽出
		JsonNode serverInfo = downloadInfo.get(WksConstants.JSON_PAPERMC_SD);
		// チェックサム情報のjsonを抽出
		JsonNode checkSums = serverInfo.get(WksConstants.JSON_PAPERMC_CS);
		// SHA256情報のjsonを抽出
		JsonNode sha256 = checkSums.get(WksConstants.JSON_PAPERMC_SHA);
		// SHA256情報からダブルクオーテーションを除去して返却
		return sha256.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);

	}

	/**
	 * Pl3xMapの最新バージョンのURLを取得するルーチン
	 *
	 * @param json APIから取得したJSONデータ
	 * @param version PaperMCの最新バージョン
	 * @return Pl3xMapの最新バージョンのダウンロードURL
	 */
	public static String getPl3xMapUrl(String json, String version){
		// jsonの解析用インスタンス作成
		ObjectMapper om = new ObjectMapper();
		// APIから取得したJSONデータのノード作成
		JsonNode src = om.readTree(json);
		// プラグイン概要jsonの抽出
		JsonNode overView = src.get(0);
		// 最新のPaperMCのバージョンとPl3xMapの最新バージョンが一致していない場合はnullを返却
		if (!version.equals(overView.get(WksConstants.JSON_PL3XMAP_GV).get(0).
				toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES))){
			// nullを返却
			return null;
		}
		// ファイル情報をjsonから抽出
		JsonNode fileInfo = overView.get(WksConstants.JSON_PL3XMAP_FILES);
		// URL情報をjsonから抽出
		JsonNode urlInfo = fileInfo.get(0).get(WksConstants.JSON_PL3XMAP_URL);
		// URL情報からダブルクオーテーションを除去して返却
		return urlInfo.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
	}

	/**
	 * Pl3xMapの最新バージョンのURLのSHA-512を取得するルーチン
	 *
	 * @param json APIから取得したJSONデータ
	 * @param version PaperMCの最新バージョン
	 * @return Pl3xMapの最新バージョンのサーバクライアントのSHA512期待値データ
	 */
	public static String getPl3xMapSha512(String json, String version){
		// jsonの解析用インスタンス作成
		ObjectMapper om = new ObjectMapper();
		// APIから取得したJSONデータのノード作成
		JsonNode src = om.readTree(json);
		// プラグイン概要jsonの抽出
		JsonNode overView = src.get(0);
		// 最新のPaperMCのバージョンとPl3xMapの最新バージョンが一致していない場合はnullを返却
		if (!version.equals(overView.get(WksConstants.JSON_PL3XMAP_GV).get(0).
				toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES))){
			// nullを返却
			return null;
		}
		// ファイル情報をjsonから抽出
		JsonNode fileInfo = overView.get(WksConstants.JSON_PL3XMAP_FILES);
		// SHA512情報をjsonから抽出
		JsonNode sha512Info = fileInfo.get(0).get(WksConstants.JSON_PL3XMAP_HASH).get(WksConstants.JSON_PL3XMAP_SHA);
		// SHA512情報からダブルクオーテーションを除去して返却
		return sha512Info.toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
	}
}

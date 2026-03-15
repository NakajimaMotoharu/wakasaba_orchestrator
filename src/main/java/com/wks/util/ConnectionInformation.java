package com.wks.util;

import com.wks.parts.WksConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * サーバ接続情報
 *
 * @param host 接続先IP
 * @param port 接続先ポート
 * @param user 接続ユーザ
 * @param key 秘密鍵パス
 */
public record ConnectionInformation(String host, int port, String user, String key) {

	/**
	 * ファイルから生成
	 *
	 * @param filePath ファイルパス
	 * @return 読み取ったサーバ接続情報
	 * @throws IOException ファイル読み込み失敗
	 */
	public static ConnectionInformation getCiFromFile(String filePath) throws IOException {
		// ファイルパス取得
		Path path = Paths.get(filePath);
		// 全行取得
		List<String> lines = Files.readAllLines(path);
		// 各行をサーバ情報に変換
		return new ConnectionInformation(lines.get(0), Integer.parseInt(lines.get(1)), lines.get(2), lines.get(3));
	}

	/** サーバ情報を文字列に変換 */
	@Override
	public String toString() {
		// 文字列に変換
		return String.format(WksConstants.OTHER_SERVER_INFO, host, port, user);
	}
}

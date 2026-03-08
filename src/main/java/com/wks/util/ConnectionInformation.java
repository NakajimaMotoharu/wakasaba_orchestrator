package com.wks.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public record ConnectionInformation(String host, int port, String user, String key) {

	/** ファイルから生成 */
	public static ConnectionInformation getCiFromFile(String filePath) throws IOException {
		Path path = Paths.get(filePath);
		List<String> lines = Files.readAllLines(path);

		return new ConnectionInformation(lines.get(0), Integer.parseInt(lines.get(1)), lines.get(2), lines.get(3));
	}

	/** サーバ情報を文字列に変換 */
	@Override
	public String toString() {
		return "{" +
				"host: \"" + host + "\", " +
				"port: " + port + ", " +
				"user: \"" + user + "\"" +
				"}";
	}
}

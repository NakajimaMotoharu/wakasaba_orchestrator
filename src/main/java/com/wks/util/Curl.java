package com.wks.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Curl実行クラス
 */
public class Curl {
	/** UserAgentのプロパティ定数 */
	private static final String USER_AGENT_CONST = "User-Agent";

	/**
	 * CURLコマンドの簡易実装
	 *
	 * @param userAgent ユーザエージェント情報
	 * @param url 実行URL
	 * @return API戻り値文字列
	 * @throws IOException API戻り値取得失敗時
	 * @throws InterruptedException API戻り値取得失敗時
	 */
	public static String exec(String userAgent, String url) throws IOException, InterruptedException {
		// HTTPクライアント取得(タイムアウト60秒)
		try (HttpClient client = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(60))
				.build()) {

			// レスポンス文字列取得
			HttpResponse<String> response = client.send(
					HttpRequest.newBuilder(URI.create(url))
							.header(USER_AGENT_CONST, userAgent)
							.build(),
					HttpResponse.BodyHandlers.ofString());

			// 戻り値返却
			return response.body();
		}
	}
}

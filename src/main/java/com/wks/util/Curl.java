package com.wks.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Curl {
	private static final String USER_AGENT_CONST = "User-Agent";

	public static String exec(String userAgent, String url) throws IOException, InterruptedException {
		try (HttpClient client = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(60))
				.build()) {

			HttpResponse<String> response = client.send(
					HttpRequest.newBuilder(URI.create(url))
							.header(USER_AGENT_CONST, userAgent)
							.build(),
					HttpResponse.BodyHandlers.ofString());

			return response.body();
		}
	}
}

# wakasaba_orchestrator 基本設計書：Curl

## 基本情報

| 項目       | 内容                                                                                  |
|----------|-------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.Curl`                                                                 |
| ファイル名    | `Curl.java`                                                                         |
| 種別       | class（public）                                                                       |
| 責務       | Java 標準の `HttpClient` を使用して HTTP GET リクエストを実行し、レスポンスボディを文字列で返す。外部 REST API の呼び出しを担う |
| 主な依存クラス  | なし                                                                                  |
| 依存ライブラリ  | `java.net.http.HttpClient`、`java.net.http.HttpRequest`、`java.net.http.HttpResponse` |

---

## フィールド一覧

| フィールド名             | 型        | 修飾子                    | 説明                                         |
|--------------------|----------|------------------------|--------------------------------------------|
| `USER_AGENT_CONST` | `String` | `private static final` | HTTP ヘッダの User-Agent キー名。値は `"User-Agent"` |

---

## メソッド一覧

| メソッド名  | 戻り値型     | 修飾子             | 説明                                      |
|--------|----------|-----------------|-----------------------------------------|
| `exec` | `String` | `public static` | 指定 URL に HTTP GET リクエストを送信し、レスポンスボディを返す |

---

## 処理フロー

### `exec(String userAgent, String url)`

```
1. HttpClient.newBuilder()
     .version(HTTP_1_1)
     .followRedirects(NORMAL)
     .connectTimeout(Duration.ofSeconds(60))
     .build()
   → client（try-with-resources で管理）

2. HttpRequest.newBuilder(URI.create(url))
     .header("User-Agent", userAgent)
     .build()
   → request

3. client.send(request, HttpResponse.BodyHandlers.ofString()) → response

4. response.body() を返す
```

---

## HttpClient の設定

| 設定項目       | 値          | 説明                            |
|------------|------------|-------------------------------|
| HTTP バージョン | `HTTP_1_1` | HTTP/1.1 固定                   |
| リダイレクト     | `NORMAL`   | 通常のリダイレクトに追従（HTTPS アップグレード以外） |
| 接続タイムアウト   | 60 秒       | 接続確立のタイムアウト                   |
| 読み取りタイムアウト | 未設定（無制限）   | サーバ無応答時に無期限ブロックする可能性あり        |

---

## 呼び出し元と対象 URL

| 呼び出し元メソッド                | URL（定数名）              | レスポンス用途                       |
|--------------------------|-----------------------|-------------------------------|
| `SshCommand.wgetPaperMc` | `URL_PAPERMC_VERSION` | PaperMC バージョン一覧の取得            |
| `SshCommand.wgetPaperMc` | `URL_PAPERMC_DL_URL`  | PaperMC ダウンロード URL・SHA256 の取得 |
| `SshCommand.wgetPaperMc` | `URL_PL3XMAP_DL_URL`  | Pl3xMap ダウンロード URL・SHA512 の取得 |
| `SshCommand.movePaperMc` | （上記と同じ 3 URL）         | SHA 検証用の期待値取得                 |

---

## 例外

| 例外クラス                  | 発生条件              |
|------------------------|-------------------|
| `IOException`          | ネットワーク障害・接続タイムアウト |
| `InterruptedException` | リクエスト待機中の割り込み     |

---

## 設計上の注意点

- HTTP レスポンスのステータスコード検証を行っていない。API が 4xx/5xx を返してもレスポンスボディをそのまま返すため、後続の
  `PaperUrlGen` で JSON パースエラーが発生する可能性がある。
- try-with-resources で `HttpClient` を管理しているため、メソッド終了時（正常・例外問わず）に接続プールがクローズされる。

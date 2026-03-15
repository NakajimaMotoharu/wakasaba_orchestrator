# クラス詳細設計：Curl

## 基本情報

| 項目       | 内容                                                                                    |
|----------|---------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.Curl`                                                                   |
| ファイル名    | `Curl.java`                                                                           |
| 種別       | class（public）                                                                         |
| 責務       | Java 標準の `HttpClient` を使用して HTTP GET リクエストを実行し、レスポンスボディを文字列で返す。外部REST APIの呼び出しを担う     |
| 依存クラス    | なし                                                                                    |
| 依存ライブラリ  | `java.net.http.HttpClient`, `java.net.http.HttpRequest`, `java.net.http.HttpResponse` |

---

## フィールド一覧

| フィールド名             | 型        | 修飾子                    | 説明                                      |
|--------------------|----------|------------------------|-----------------------------------------|
| `USER_AGENT_CONST` | `String` | `private static final` | HTTPヘッダのUser-Agentキー名。値は `"User-Agent"` |

---

## メソッド一覧

| メソッド名  | 戻り値型     | 修飾子             | 説明                                  |
|--------|----------|-----------------|-------------------------------------|
| `exec` | `String` | `public static` | 指定URLにHTTP GETリクエストを送信し、レスポンスボディを返す |

---

## メソッド詳細

### `exec(String userAgent, String url)`

```java
public static String exec(String userAgent, String url)
		throws IOException, InterruptedException;
```

#### 概要

指定された `url` に対して `userAgent` を設定したHTTP GETリクエストを送信し、レスポンスボディ文字列を返す。  
HTTPクライアントは try-with-resources で管理されるため、メソッド終了時に自動クローズされる。

#### 処理フロー

```
1. HttpClient.newBuilder()
     .version(HTTP_1_1)
     .followRedirects(NORMAL)
     .connectTimeout(Duration.ofSeconds(60))
     .build()
   → client（try-with-resourcesで管理）

2. HttpRequest.newBuilder(URI.create(url))
     .header("User-Agent", userAgent)
     .build()
   → request

3. client.send(request, HttpResponse.BodyHandlers.ofString())
   → response（HttpResponse<String>）

4. response.body() を返却
```

#### HttpClient の設定

| 設定項目      | 値          | 説明                             |
|-----------|------------|--------------------------------|
| HTTPバージョン | `HTTP_1_1` | HTTP/1.1 固定                    |
| リダイレクト    | `NORMAL`   | 通常のリダイレクト（HTTPSへのアップグレード以外）に従う |
| 接続タイムアウト  | 60秒        | 接続確立のタイムアウト（読み取りタイムアウトは未設定）    |

#### 引数

| 引数名         | 型        | 説明                                                                                           |
|-------------|----------|----------------------------------------------------------------------------------------------|
| `userAgent` | `String` | `User-Agent` ヘッダに設定する文字列。`WksConstants.OTHER_USER_AGENT`（`"wakasaba_orchestrator/1.0"`）が渡される |
| `url`       | `String` | リクエスト先のURL文字列                                                                                |

#### 戻り値

| 型        | 説明                                |
|----------|-----------------------------------|
| `String` | HTTPレスポンスボディ全体の文字列（通常はJSONフォーマット） |

#### 例外

| 例外クラス                  | 発生条件                 |
|------------------------|----------------------|
| `IOException`          | ネットワーク障害、または接続タイムアウト |
| `InterruptedException` | リクエスト待機中の割り込み        |

#### 呼び出し元と対象URL

| 呼び出し元メソッド                | URL（定数名）              | レスポンス用途                    |
|--------------------------|-----------------------|----------------------------|
| `SshCommand.wgetPaperMc` | `URL_PAPERMC_VERSION` | PaperMCバージョン一覧の取得          |
| `SshCommand.wgetPaperMc` | `URL_PAPERMC_DL_URL`  | PaperMCダウンロードURL・SHA256の取得 |
| `SshCommand.wgetPaperMc` | `URL_PL3XMAP_DL_URL`  | Pl3xMapダウンロードURL・SHA512の取得 |
| `SshCommand.movePaperMc` | （上記と同じ3URL）           | SHA検証用の期待値取得               |

---

## 設計上の注意点

- HTTPレスポンスのステータスコード検証を行っていない。APIが4xx/5xx を返してもレスポンスボディをそのまま返すため、後続の
  `PaperUrlGen` でJSONパースエラーが発生する可能性がある。
- 読み取りタイムアウト（`readTimeout`）が未設定であるため、サーバが応答を返さない場合に無期限にブロックする可能性がある。
- try-with-resources で `HttpClient` を管理しているため、メソッド終了時（正常・例外問わず）に接続プールがクローズされる。

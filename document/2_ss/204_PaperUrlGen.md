# クラス詳細設計：PaperUrlGen

## 基本情報

| 項目       | 内容                                                                          |
|----------|-----------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.papermc.PaperUrlGen`                                               |
| ファイル名    | `PaperUrlGen.java`                                                          |
| 種別       | class（public）                                                               |
| 責務       | PaperMC API・Modrinth API から取得したJSONレスポンスを解析し、バージョン文字列・ダウンロードURL・チェックサムを抽出する |
| 依存クラス    | `WksConstants`                                                              |
| 依存ライブラリ  | `tools.jackson.databind.ObjectMapper`, `tools.jackson.databind.JsonNode`    |

---

## フィールド一覧

なし（フィールドを持たないユーティリティクラス）

---

## メソッド一覧

| メソッド名               | 戻り値型     | 修飾子             | 説明                                                |
|---------------------|----------|-----------------|---------------------------------------------------|
| `getPaperMcVersion` | `String` | `public static` | PaperMC バージョン一覧JSONから最新バージョン文字列を取得する              |
| `getPaperMcUrl`     | `String` | `public static` | PaperMC ビルド情報JSONからダウンロードURLを取得する                 |
| `getPaperMcSha256`  | `String` | `public static` | PaperMC ビルド情報JSONからSHA-256チェックサムを取得する             |
| `getPl3xMapUrl`     | `String` | `public static` | Modrinth APIレスポンスJSONからPl3xMapのダウンロードURLを取得する     |
| `getPl3xMapSha512`  | `String` | `public static` | Modrinth APIレスポンスJSONからPl3xMapのSHA-512チェックサムを取得する |

---

## メソッド詳細

### `getPaperMcVersion(String json)`

```java
public static String getPaperMcVersion(String json);
```

#### 概要

`GET https://fill.papermc.io/v3/projects/paper` のレスポンスから最新のPaperMCバージョンを取得する。

#### 処理フロー

```
1. ObjectMapper でJSONをパース → src（JsonNode）
2. src.get("versions") → majorVersion（JsonNode、Mapのような構造）
3. majorVersion.propertyNames() からキー一覧をコレクション化し、先頭キーを取得
4. majorVersion.get(先頭キー) → fullVersion（バージョン文字列の配列ノード）
5. fullVersion.get(0).toString() → ダブルクオーテーション付きバージョン文字列
6. ダブルクオーテーションを除去して返却
```

#### 引数

| 引数名    | 型        | 説明                                             |
|--------|----------|------------------------------------------------|
| `json` | `String` | PaperMC API（`/v3/projects/paper`）のレスポンスJSON文字列 |

#### 戻り値

| 型        | 説明                        |
|----------|---------------------------|
| `String` | 最新バージョン文字列（例: `"1.21.4"`） |

#### 対象JSONの構造（例）

```json
{
  "versions": {
    "1.21": [
      "1.21.4",
      "1.21.3"
    ]
  }
}
```

---

### `getPaperMcUrl(String json)`

```java
public static String getPaperMcUrl(String json);
```

#### 概要

`GET https://fill.papermc.io/v3/projects/paper/versions/{version}/builds` のレスポンスから最新ビルドのダウンロードURLを取得する。

#### 処理フロー

```
1. ObjectMapper でJSONをパース → src（JsonNode）
2. src.get(0) → 最新ビルド情報
3. .get("downloads") → ダウンロード情報ノード
4. .get("server:default") → サーバ情報ノード
5. .get("url") → URL文字列ノード
6. ダブルクオーテーションを除去して返却
```

#### 引数

| 引数名    | 型        | 説明                                                                       |
|--------|----------|--------------------------------------------------------------------------|
| `json` | `String` | PaperMC API（`/v3/projects/paper/versions/{version}/builds`）のレスポンスJSON文字列 |

#### 戻り値

| 型        | 説明                   |
|----------|----------------------|
| `String` | JARファイルのダウンロードURL文字列 |

#### 対象JSONの構造（例）

```json
[
  {
    "downloads": {
      "server:default": {
        "url": "https://api.papermc.io/.../paper-1.21.4-xxx.jar",
        "checksums": {
          "sha256": "0123abc..."
        }
      }
    }
  }
]
```

---

### `getPaperMcSha256(String json)`

```java
public static String getPaperMcSha256(String json);
```

#### 概要

PaperMC ビルド情報JSONからSHA-256チェックサム文字列を取得する。

#### 処理フロー

```
1. ObjectMapper でJSONをパース → src（JsonNode）
2. src.get(0).get("downloads").get("server:default") → serverInfo
3. serverInfo.get("checksums").get("sha256") → sha256ノード
4. ダブルクオーテーションを除去して返却
```

#### 引数

| 引数名    | 型        | 説明                                                        |
|--------|----------|-----------------------------------------------------------|
| `json` | `String` | PaperMC ビルド情報APIのレスポンスJSON文字列（`getPaperMcUrl` と同一エンドポイント） |

#### 戻り値

| 型        | 説明                         |
|----------|----------------------------|
| `String` | SHA-256ハッシュ値文字列（64文字の16進数） |

---

### `getPl3xMapUrl(String json, String version)`

```java
public static String getPl3xMapUrl(String json, String version);
```

#### 概要

Modrinth APIレスポンスJSONからPl3xMapの最新バージョンのダウンロードURLを取得する。  
Pl3xMapの最新バージョンが対応するゲームバージョンがPaperMCの最新バージョンと一致しない場合は `null` を返す。

#### 処理フロー

```
1. ObjectMapper でJSONをパース → src（JsonNode）
2. src.get(0) → 最新バージョン情報 overView
3. overView.get("game_versions").get(0) → Pl3xMapが対応するゲームバージョン
4. 上記バージョンと引数 version を比較
   不一致 → null を返却
   一致 ↓
5. overView.get("files").get(0).get("url") → URLノード
6. ダブルクオーテーションを除去して返却
```

#### 引数

| 引数名       | 型        | 説明                                                       |
|-----------|----------|----------------------------------------------------------|
| `json`    | `String` | Modrinth API（`/v2/project/pl3xmap/version`）のレスポンスJSON文字列 |
| `version` | `String` | PaperMCの最新バージョン文字列（比較対象）                                 |

#### 戻り値

| 型        | 説明                             |
|----------|--------------------------------|
| `String` | ダウンロードURLまたは `null`（バージョン不一致時） |

#### 対象JSONの構造（例）

```json
[
  {
    "game_versions": [
      "1.21.4"
    ],
    "files": [
      {
        "url": "https://cdn.modrinth.com/.../Pl3xMap-xxx.jar",
        "hashes": {
          "sha512": "0123abc..."
        }
      }
    ]
  }
]
```

---

### `getPl3xMapSha512(String json, String version)`

```java
public static String getPl3xMapSha512(String json, String version);
```

#### 概要

Modrinth APIレスポンスJSONからPl3xMapのSHA-512チェックサム文字列を取得する。  
`getPl3xMapUrl` と同様にバージョン不一致時は `null` を返す。

#### 処理フロー

```
1. ObjectMapper でJSONをパース → src（JsonNode）
2. src.get(0) → overView
3. バージョン一致チェック（getPl3xMapUrl と同じ）
   不一致 → null を返却
   一致 ↓
4. overView.get("files").get(0).get("hashes").get("sha512") → sha512Info
5. ダブルクオーテーションを除去して返却
```

#### 引数

| 引数名       | 型        | 説明                         |
|-----------|----------|----------------------------|
| `json`    | `String` | Modrinth API のレスポンスJSON文字列 |
| `version` | `String` | PaperMCの最新バージョン文字列（比較対象）   |

#### 戻り値

| 型        | 説明                                               |
|----------|--------------------------------------------------|
| `String` | SHA-512ハッシュ値文字列（128文字の16進数）または `null`（バージョン不一致時） |

---

## 設計上の注意点

- すべてのメソッドが `ObjectMapper.readTree()` を直接呼び出している。`ObjectMapper.readTree()` は
  `JacksonException` を投げるが、Jackson 3.x では `JacksonException` は `RuntimeException`
  のサブクラスである非検査例外のため、メソッドシグネチャに `throws` 宣言は不要であり問題なく動作する。
- `getPl3xMapUrl` と `getPl3xMapSha512` はバージョン判定ロジックが重複している。将来的にはリファクタリングの対象となりうる。
- ダブルクオーテーション除去（`replace("\"", "")`）は、JsonNode の `toString()` が値をダブルクオーテーションで囲んで返すことへの対処。
  `asText()` を使えばより簡潔に記述できる。

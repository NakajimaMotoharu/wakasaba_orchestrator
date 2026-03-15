# wakasaba_orchestrator 基本設計書：PaperUrlGen

## 基本情報

| 項目       | 内容                                                                             |
|----------|--------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.papermc.PaperUrlGen`                                                  |
| ファイル名    | `PaperUrlGen.java`                                                             |
| 種別       | class（public）                                                                  |
| 責務       | PaperMC API・Modrinth API から取得した JSON レスポンスを解析し、バージョン文字列・ダウンロード URL・チェックサムを抽出する |
| 主な依存クラス  | `WksConstants`                                                                 |
| 依存ライブラリ  | `tools.jackson.databind.ObjectMapper`、`tools.jackson.databind.JsonNode`        |

---

## フィールド一覧

なし（フィールドを持たないユーティリティクラス）

---

## メソッド一覧

| メソッド名               | 戻り値型     | 修飾子             | 説明                                                       |
|---------------------|----------|-----------------|----------------------------------------------------------|
| `getPaperMcVersion` | `String` | `public static` | PaperMC バージョン一覧 JSON から最新バージョン文字列を取得する                   |
| `getPaperMcUrl`     | `String` | `public static` | PaperMC ビルド情報 JSON からダウンロード URL を取得する                    |
| `getPaperMcSha256`  | `String` | `public static` | PaperMC ビルド情報 JSON から SHA-256 チェックサムを取得する                |
| `getPl3xMapUrl`     | `String` | `public static` | Modrinth API レスポンス JSON から Pl3xMap のダウンロード URL を取得する     |
| `getPl3xMapSha512`  | `String` | `public static` | Modrinth API レスポンス JSON から Pl3xMap の SHA-512 チェックサムを取得する |

---

## 処理フロー

### `getPaperMcVersion(String json)`

PaperMC API（`/v3/projects/paper`）レスポンスから最新バージョン文字列を返す。

```
1. ObjectMapper.readTree(json) → src
2. src.get("versions") → majorVersion（Map 構造）
3. majorVersion.propertyNames() → キー一覧をリスト化し先頭キーを取得（最新メジャーバージョン）
4. majorVersion.get(先頭キー) → fullVersion（バージョン文字列配列ノード）
5. fullVersion.get(0).toString() のダブルクオーテーションを除去して返す（例: "1.21.4"）
```

#### 対象 JSON 構造（例）

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

PaperMC API（`/v3/projects/paper/versions/{version}/builds`）レスポンスからダウンロード URL を返す。

```
1. ObjectMapper.readTree(json) → src
2. src.get(0).get("downloads").get("server:default").get("url")
3. ダブルクオーテーションを除去して返す
```

---

### `getPaperMcSha256(String json)`

同 API レスポンスから SHA-256 チェックサムを返す。

```
1. ObjectMapper.readTree(json) → src
2. src.get(0).get("downloads").get("server:default").get("checksums").get("sha256")
3. ダブルクオーテーションを除去して返す
```

#### 対象 JSON 構造（例）

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

### `getPl3xMapUrl(String json, String version)`

Modrinth API レスポンスから Pl3xMap のダウンロード URL を返す。バージョン不一致時は `null` を返す。

```
1. ObjectMapper.readTree(json) → src
2. src.get(0) → overView
3. overView.get("game_versions").get(0) のバージョン文字列と引数 version を比較
   不一致 → null を返す
   一致  ↓
4. overView.get("files").get(0).get("url")
5. ダブルクオーテーションを除去して返す
```

---

### `getPl3xMapSha512(String json, String version)`

Modrinth API レスポンスから Pl3xMap の SHA-512 チェックサムを返す。バージョン不一致時は `null` を返す。

```
1. ObjectMapper.readTree(json) → src
2. src.get(0) → overView
3. バージョン一致チェック（getPl3xMapUrl と同じ）
   不一致 → null を返す
   一致  ↓
4. overView.get("files").get(0).get("hashes").get("sha512")
5. ダブルクオーテーションを除去して返す
```

#### 対象 JSON 構造（例）

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

## 例外

- すべてのメソッドに `throws` 宣言はない。
- `ObjectMapper.readTree()` が発生させる `JacksonException` は Jackson 3.x では非検査例外として扱われる。
- JSON のキーが存在しない場合、`NullPointerException`（非検査例外）が発生する。

---

## 設計上の注意点

- JSON ノードの値取得には `toString()` を使用しているためダブルクオーテーションが付くが、`JSON_REPLACE_DQ` /
  `JSON_REPLACE_ES` 定数で除去している。
- `getPl3xMapUrl` と `getPl3xMapSha512` はバージョン判定ロジックが重複している。
- PaperMC バージョン不一致時の挙動と Pl3xMap バージョン不一致時の挙動は異なる点に注意（PaperMC は呼出し元でエラーを検知するのに対し、Pl3xMap
  は `null` 返却で条件分岐する）。

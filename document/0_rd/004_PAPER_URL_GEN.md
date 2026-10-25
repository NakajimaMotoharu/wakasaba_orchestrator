# wakasaba_orchestrator 要件定義書：PaperUrlGen

## 基本情報

| 項目             | 内容                                                                                                                    |
|------------------|-------------------------------------------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.papermc.PaperUrlGen`                                                                                           |
| ファイル名       | `PaperUrlGen.java`                                                                                                      |
| 種別             | class（public）                                                                                                         |
| 責務             | PaperMC API・Modrinth API から取得したJSONレスポンスを解析し、バージョン文字列・ダウンロードURL・チェックサムを抽出する |
| 主な依存クラス   | `WksConstants`                                                                                                          |
| 依存ライブラリ   | Jackson Databind（`tools.jackson.databind.ObjectMapper`、`tools.jackson.databind.JsonNode`）                            |

---

## 要求機能

### PU-01：PaperMC最新正式版のバージョン文字列取得

- PaperMC API（`/v3/projects/paper`）のJSONレスポンスから、最新の正式版のバージョン文字列を取得できること。
- `versions` オブジェクトの系列と各系列内の候補をAPI応答順に探索し、ハイフン（`-`）を含むpre・rc等の候補を除外すること。
- ハイフンを含まない最初の候補を正式版として返すこと。系列内に正式版がなければ次の系列を探索すること。
- 全系列に正式版が存在しない場合（`versions` が空の場合を含む）、`OTHER_PAPERMC_EXCEPTION` をメッセージとして
  `RuntimeException` を送出すること。
- 返す文字列はダブルクオーテーションを除去した値であること（例：`26.2`）。

#### 参照JSONの構造

```json
{
  "versions": {
    "26.3": [
      "26.3-rc-3"
    ],
    "26.2": [
      "26.2-rc-2",
      "26.2"
    ]
  }
}
```

この例の返却値は `26.2` とする。

### PU-02：PaperMCダウンロードURLの取得

- PaperMC API（`/v3/projects/paper/versions/{version}/builds`）のJSONレスポンスから、最新ビルドのダウンロードURLを取得できること。
- 配列の先頭要素の `downloads` → `server:default` → `url` の値を返すこと。
- 返す文字列はダブルクオーテーションを除去した値であること。

### PU-03：PaperMC SHA-256チェックサムの取得

- PaperMC API（`/v3/projects/paper/versions/{version}/builds`）のJSONレスポンスから、最新ビルドのSHA-256チェックサム文字列を取得できること。
- 配列の先頭要素の `downloads` → `server:default` → `checksums` → `sha256` の値を返すこと。
- 返す文字列はダブルクオーテーションを除去した値であること。

#### 参照JSONの構造

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

### PU-04：Pl3xMapダウンロードURLの取得

- Modrinth API（`/v2/project/pl3xmap/version`）のJSONレスポンスから、Pl3xMapの最新ダウンロードURLを取得できること。
- 配列の先頭要素の `game_versions[0]` の値と引数のバージョン文字列を比較し、一致しない場合は `null` を返すこと。
- バージョンが一致する場合、`files[0]` → `url` の値を返すこと。
- 返す文字列はダブルクオーテーションを除去した値であること。

### PU-05：Pl3xMap SHA-512チェックサムの取得

- Modrinth API（`/v2/project/pl3xmap/version`）のJSONレスポンスから、Pl3xMapの最新SHA-512チェックサムを取得できること。
- `PU-04` と同様のバージョン一致チェックを行い、不一致の場合は `null` を返すこと。
- バージョンが一致する場合、`files[0]` → `hashes` → `sha512` の値を返すこと。
- 返す文字列はダブルクオーテーションを除去した値であること。

#### 参照JSONの構造

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

## 要求インタフェース

### フィールド

フィールドなし（ユーティリティクラス）。

### メソッド

| メソッド名          | 戻り値型 | 修飾子          | 要件概要                                                                                      |
|---------------------|----------|-----------------|-----------------------------------------------------------------------------------------------|
| `getPaperMcVersion` | `String` | `public static` | PaperMCバージョン一覧JSONからpre・rc等を除外し、最新正式版の文字列を返す                      |
| `getPaperMcUrl`     | `String` | `public static` | PaperMCビルド情報JSONからダウンロードURLを返す                                                |
| `getPaperMcSha256`  | `String` | `public static` | PaperMCビルド情報JSONからSHA-256チェックサムを返す                                            |
| `getPl3xMapUrl`     | `String` | `public static` | Modrinth APIレスポンスJSONからPl3xMapのダウンロードURLを返す。バージョン不一致時は `null`     |
| `getPl3xMapSha512`  | `String` | `public static` | Modrinth APIレスポンスJSONからPl3xMapのSHA-512チェックサムを返す。バージョン不一致時は `null` |

---

## 例外要件

- メソッドへの `throws` 宣言は不要。
- `getPaperMcVersion` で正式版が見つからない場合は `RuntimeException` を送出し、呼び出し元を経由して `Main.main`
  でスタックトレースをログへ記録すること。
- `ObjectMapper.readTree()` の失敗（Jackson非検査例外）、または JSONキーが存在しない場合の `NullPointerException`
  （非検査例外）は呼び出し元まで伝播する。

---

## 制約・注意事項

- 最新順はAPI応答の並び順を前提とし、数値比較・並べ替えは行わないこと。正式版の判定はハイフンの有無のみとし、数字・ピリオドの正規表現検証は行わないこと。
- JSONノードの値取得には `toString()` を使用するためダブルクオーテーションが付くが、`WksConstants.JSON_REPLACE_DQ` /
  `JSON_REPLACE_ES` 定数を用いて除去すること。
- `getPl3xMapUrl` と `getPl3xMapSha512` のバージョン判定ロジックは同一の仕様とすること。
- PaperMCバージョン不一致時（APIレスポンス構造変化等）の挙動と、Pl3xMapバージョン不一致時の挙動は異なる。Pl3xMapバージョン不一致は
  `null` 返却で正常処理であり、PaperMCの不一致は後続の JSON キー参照失敗（非検査例外）として扱われる。

# wakasaba_orchestrator 基本設計書：ConnectionInformation

## 基本情報

| 項目       | 内容                                                                          |
|----------|-----------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.ConnectionInformation`                                        |
| ファイル名    | `ConnectionInformation.java`                                                |
| 種別       | record（public）                                                              |
| 責務       | SSH サーバへの接続に必要な情報（ホスト・ポート・ユーザ名・秘密鍵パス）をイミュータブルに保持する。ファイルからのインスタンス生成メソッドを提供する |
| 主な依存クラス  | `WksConstants`                                                              |

---

## レコードコンポーネント（フィールド）

Java の `record` 型であるため、以下のコンポーネントに対してコンストラクタ引数・アクセサメソッドが自動生成される。

| コンポーネント名 | 型        | 説明                      |
|----------|----------|-------------------------|
| `host`   | `String` | SSH 接続先の IP アドレスまたはホスト名 |
| `port`   | `int`    | SSH 接続ポート番号（通常 22）      |
| `user`   | `String` | SSH 接続ユーザ名              |
| `key`    | `String` | SSH 認証に使用する秘密鍵ファイルのパス   |

---

## メソッド一覧

| メソッド名           | 戻り値型                    | 修飾子             | 説明                                                   |
|-----------------|-------------------------|-----------------|------------------------------------------------------|
| `getCiFromFile` | `ConnectionInformation` | `public static` | 指定パスのファイルを読み込み `ConnectionInformation` インスタンスを生成して返す |
| `toString`      | `String`                | `public`        | `key` を含まないセキュアなサーバ情報文字列を返す（record デフォルトをオーバーライド）    |

---

## 処理フロー

### `getCiFromFile(String filePath)`

```
1. Paths.get(filePath) → path
2. Files.readAllLines(path) → lines（List<String>）
3. new ConnectionInformation(
     lines.get(0),                   // host
     Integer.parseInt(lines.get(1)), // port（文字列 → int 変換）
     lines.get(2),                   // user
     lines.get(3)                    // key
   ) を返す
```

#### 接続情報ファイルのフォーマット

1行1値のテキストファイル（UTF-8）。

```
192.168.1.100        ← 1行目: host
22                   ← 2行目: port（整数値）
myuser               ← 3行目: user
/home/mini/.ssh/id_rsa  ← 4行目: key（秘密鍵ファイルの絶対パス）
```

---

### `toString()`

`WksConstants.OTHER_SERVER_INFO`（`{host: "%s", port: %d, user: "%s"}`）のフォーマットで `host`・`port`・`user` を文字列化して返す。
`key`（秘密鍵パス）はログに出力しない。

---

## 例外

| 例外クラス                            | 発生条件                            |
|----------------------------------|---------------------------------|
| `IOException`                    | ファイルが存在しない、読み取り権限がない等のファイル読込エラー |
| `NumberFormatException`（非検査）     | ファイルの 2 行目が int に変換できない場合       |
| `IndexOutOfBoundsException`（非検査） | ファイルの行数が 4 行未満の場合               |

---

## 設計上の注意点

- Java の `record` はイミュータブルであるため、生成後にコンポーネントの値を変更できない。
- `toString()` をオーバーライドすることで `key`（秘密鍵パス）がログに漏洩しないよう設計されている。
- `Files.readAllLines()` はデフォルトで UTF-8 を使用する。

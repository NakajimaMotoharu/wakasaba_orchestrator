# クラス詳細設計：ConnectionInformation

## 基本情報

| 項目       | 内容                                                                   |
|----------|----------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.ConnectionInformation`                                 |
| ファイル名    | `ConnectionInformation.java`                                         |
| 種別       | record（public）                                                       |
| 責務       | SSHサーバへの接続に必要な情報（ホスト・ポート・ユーザ名・秘密鍵パス）をイミュータブルに保持する。ファイルからの生成メソッドを提供する |
| 依存クラス    | `WksConstants`                                                       |

---

## レコードコンポーネント（フィールド）一覧

Javaの `record` 型であるため、以下のコンポーネントに対してコンストラクタ引数・アクセサメソッドが自動生成される。

| コンポーネント名 | 型        | 説明                   |
|----------|----------|----------------------|
| `host`   | `String` | SSH接続先のIPアドレスまたはホスト名 |
| `port`   | `int`    | SSH接続ポート番号（通常22）     |
| `user`   | `String` | SSH接続ユーザ名            |
| `key`    | `String` | SSH認証に使用する秘密鍵ファイルのパス |

### 自動生成されるアクセサメソッド

| メソッド名    | 戻り値型     | 説明                  |
|----------|----------|---------------------|
| `host()` | `String` | `host` コンポーネントの値を返す |
| `port()` | `int`    | `port` コンポーネントの値を返す |
| `user()` | `String` | `user` コンポーネントの値を返す |
| `key()`  | `String` | `key` コンポーネントの値を返す  |

---

## メソッド一覧

| メソッド名           | 戻り値型                    | 修飾子             | 説明                            |
|-----------------|-------------------------|-----------------|-------------------------------|
| `getCiFromFile` | `ConnectionInformation` | `public static` | 指定パスのファイルからインスタンスを生成する        |
| `toString`      | `String`                | `public`        | インスタンスを人間可読な文字列に変換する（オーバーライド） |

---

## メソッド詳細

### `getCiFromFile(String filePath)`

```java
public static ConnectionInformation getCiFromFile(String filePath) throws IOException;
```

#### 概要

指定パスのファイルを読み込み、各行を対応するコンポーネントとして `ConnectionInformation` インスタンスを生成して返す。

#### 処理フロー

```
1. Paths.get(filePath) → path（Pathオブジェクト）
2. Files.readAllLines(path) → lines（List<String>）
3. new ConnectionInformation(
     lines.get(0),              // host
     Integer.parseInt(lines.get(1)),  // port（文字列→int変換）
     lines.get(2),              // user
     lines.get(3)               // key
   )
4. 生成したインスタンスを返却
```

#### 引数

| 引数名        | 型        | 説明          |
|------------|----------|-------------|
| `filePath` | `String` | 接続情報ファイルのパス |

#### 戻り値

| 型                       | 説明                     |
|-------------------------|------------------------|
| `ConnectionInformation` | ファイルから読み込んだ接続情報のインスタンス |

#### 例外

| 例外クラス                       | 発生条件                                        |
|-----------------------------|---------------------------------------------|
| `IOException`               | ファイルが存在しない、読み取り権限がない等のファイル読み込みエラー           |
| `NumberFormatException`     | ファイルの2行目がint値に変換できない場合（非検査例外、`throws` 宣言なし） |
| `IndexOutOfBoundsException` | ファイルの行数が4行未満の場合（非検査例外、`throws` 宣言なし）        |

#### 接続情報ファイルのフォーマット

各行に値を1つずつ記載する。行の空白トリムは行われないため、余分な空白や改行コードに注意する。

```
192.168.1.100       ← 1行目: host
22                  ← 2行目: port（整数値）
myuser              ← 3行目: user
/home/user/.ssh/id_rsa  ← 4行目: key（秘密鍵ファイルの絶対パス）
```

詳細は [補足資料：接続情報ファイル仕様](./290_SERVER_FILE_SPEC) を参照。

---

### `toString()`

```java

@Override
public String toString();
```

#### 概要

`record` のデフォルト `toString()` をオーバーライドし、`key` を含まないセキュアな文字列表現を返す。

#### 処理フロー

```
String.format(WksConstants.OTHER_SERVER_INFO, host, port, user)
```

`WksConstants.OTHER_SERVER_INFO` = `"{host: \"%s\", port: %d, user: \"%s\"}"`

#### 戻り値

| 型        | 説明                                                     |
|----------|--------------------------------------------------------|
| `String` | 例: `{host: "192.168.1.100", port: 22, user: "myuser"}` |

> **注意:** `key`（秘密鍵パス）はログに出力されない。セキュリティ上の配慮。

---

## 設計上の注意点

- Java の `record` は本質的にイミュータブルであるため、生成後にコンポーネントの値を変更することはできない。
- `toString()` をオーバーライドすることで秘密鍵パス（`key`）がログに漏洩しないように設計されている。
- `Files.readAllLines()` はデフォルトでUTF-8を使用する。ファイルのエンコーディングが異なる場合は文字化けが生じる可能性がある。

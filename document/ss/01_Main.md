# クラス詳細設計：Main

## 基本情報

| 項目       | 内容                                      |
|----------|-----------------------------------------|
| 完全修飾クラス名 | `com.wks.main.Main`                     |
| ファイル名    | `Main.java`                             |
| 種別       | class（public）                           |
| 責務       | アプリケーションのエントリポイント。引数検証・処理起動・ログファイル出力を担う |
| 依存クラス    | `WksWorkFlow`, `WksConstants`           |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                   | 初期値                 | 説明                                          |
|--------|---------------------|-----------------------|---------------------|---------------------------------------------|
| `log`  | `ArrayList<String>` | `public static final` | `new ArrayList<>()` | アプリケーション全体で共有するログリスト。他クラスは `Main.log` で参照する |

### フィールド詳細

#### `log`

```java
public static final ArrayList<String> log = new ArrayList<>();
```

- アプリケーション実行中に各クラスが書き込むすべてのログ行を格納する。
- `static final` であるため、クラスロード時に1度だけ初期化されリスト本体は不変参照として各クラスに共有される（リストの中身は可変）。
- 実行終了後に `outLog()` でファイルへ書き出される。

---

## メソッド一覧

| メソッド名         | 戻り値型     | 修飾子              | 説明                               |
|---------------|----------|------------------|----------------------------------|
| `main`        | `void`   | `public static`  | エントリポイント。引数検証・ワークフロー起動・ログ出力を行う   |
| `outLog`      | `void`   | `public static`  | ログリストの内容をファイルに出力する               |
| `getDateTime` | `String` | `private static` | 現在日時を `yyyyMMddHHmmss` 形式の文字列で返す |

---

## メソッド詳細

### `main(String[] args)`

```java
public static void main(String[] args)
		throws IOException, InterruptedException, JSchException;
```

#### 処理フロー

```
args.length == 3 ?
  Yes:
    1. log に開始時刻を追記（WksConstants.LOG_START_TIME）
    2. WksWorkFlow.execScheduledJob(args) を呼び出す
    3. log に終了時刻を追記（WksConstants.LOG_END_TIME）
    4. outLog(ログファイルパス) を呼び出す
  No:
    System.out.println(WksConstants.OTHER_ARGS_MSG) を出力して終了
```

#### 引数

| 引数名    | 型          | 説明                                 |
|--------|------------|------------------------------------|
| `args` | `String[]` | コマンドライン引数。3要素必須（各要素はサーバ接続情報ファイルパス） |

#### 例外

| 例外クラス                  | 発生条件                          |
|------------------------|-------------------------------|
| `IOException`          | サーバファイル読み込み失敗、またはログファイル書き込み失敗 |
| `InterruptedException` | SSHコマンド実行中の割り込み               |
| `JSchException`        | SSH接続・実行失敗                    |

---

### `outLog(String path)`

```java
public static void outLog(String path) throws FileNotFoundException;
```

#### 処理フロー

```
1. PrintStream を指定パスで生成
2. log の各要素を1行ずつ println で書き出す
3. PrintStream をクローズ
```

#### 引数

| 引数名    | 型        | 説明                                                      |
|--------|----------|---------------------------------------------------------|
| `path` | `String` | 出力先ファイルパス（`WksConstants.PATH_EXEC_LOG` にタイムスタンプを埋め込んだ値） |

#### 例外

| 例外クラス                   | 発生条件                |
|-------------------------|---------------------|
| `FileNotFoundException` | 指定パスへのファイル生成・書き込み失敗 |

---

### `getDateTime()`

```java
private static String getDateTime();
```

#### 処理フロー

```
1. DateTimeFormatter を WksConstants.OTHER_DATE_TIME_FMT（"yyyyMMddHHmmss"）で生成
2. ZonedDateTime.now(ZoneId.of("Asia/Tokyo")) でタイムゾーン付き現在日時を取得
3. フォーマットして文字列で返却
```

#### 戻り値

| 型        | 説明                                                    |
|----------|-------------------------------------------------------|
| `String` | 現在日時を `yyyyMMddHHmmss` 形式に変換した文字列（例：`20240101120000`） |

---

## 設計上の注意点

- `log` を `public static final` にしているため、各クラスから `Main.log` として直接参照・追記が可能。シングルトン的なグローバルログバッファとして機能している。
- ログファイルのパスには開始時刻と異なるタイムスタンプ（終了後の `getDateTime()` 呼出し）が使われるため、ファイル名は終了時刻を示す。
- 処理中の例外はすべて `throws` で上位に委譲しており、`main` メソッド内での個別ハンドリングは行っていない。

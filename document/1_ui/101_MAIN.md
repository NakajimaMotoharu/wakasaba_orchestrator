# wakasaba_orchestrator 基本設計書：Main

## 基本情報

| 項目       | 内容                                      |
|----------|-----------------------------------------|
| 完全修飾クラス名 | `com.wks.main.Main`                     |
| ファイル名    | `Main.java`                             |
| 種別       | class（public）                           |
| 責務       | アプリケーションのエントリポイント。引数検証・処理起動・ログファイル出力を担う |
| 主な依存クラス  | `WksWorkFlow`、`WksConstants`            |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                   | 説明                                                  |
|--------|---------------------|-----------------------|-----------------------------------------------------|
| `log`  | `ArrayList<String>` | `public static final` | アプリケーション全体で共有するグローバルログリスト。他クラスは `Main.log` で参照・追記する |

---

## メソッド一覧

| メソッド名         | 戻り値型     | 修飾子              | 説明                                 |
|---------------|----------|------------------|------------------------------------|
| `main`        | `void`   | `public static`  | エントリポイント。引数検証・ワークフロー起動・ログファイル出力を行う |
| `outLog`      | `void`   | `public static`  | グローバルログリストの内容をファイルに出力する            |
| `getDateTime` | `String` | `private static` | 現在日時を `yyyyMMddHHmmss` 形式で返す       |

---

## 処理フロー

### `main(String[] args)`

```
args.length == 3 ?
  Yes:
    1. log に開始時刻を追記（LOG_START_TIME フォーマット）
    2. WksWorkFlow.execScheduledJob(args) を呼び出す
    3. log に終了時刻を追記（LOG_END_TIME フォーマット）
    4. outLog(PATH_EXEC_LOG にタイムスタンプを埋め込んだパス) を呼び出す
  No:
    System.out.println(OTHER_ARGS_MSG) を出力して終了
```

### `outLog(String path)`

```
1. PrintStream を指定パスで生成
2. log の各要素を1行ずつ println で書き出す
3. PrintStream をクローズ
```

### `getDateTime()`

```
1. DateTimeFormatter を OTHER_DATE_TIME_FMT（"yyyyMMddHHmmss"）で生成
2. ZonedDateTime.now(ZoneId.of("Asia/Tokyo")) で現在日時を取得
3. フォーマットして文字列で返す
```

---

## 例外

| メソッド名    | 例外クラス                  | 発生条件                      |
|----------|------------------------|---------------------------|
| `main`   | `IOException`          | サーバファイル読込失敗、またはログファイル書込失敗 |
| `main`   | `InterruptedException` | SSH コマンド実行中の割り込み          |
| `main`   | `JSchException`        | SSH 接続・実行失敗               |
| `outLog` | `IOException`          | 指定パスへのファイル生成・書込失敗         |

---

## 設計上の注意点

- `log` は `public static final` であるため、クラスロード時に1度だけ初期化される。各クラスは同一リストを参照することで、アプリケーション全体のログを一元管理する。
- ログファイルのファイル名には `outLog` 呼出し時点（終了後）の `getDateTime()` 値を使用するため、ファイル名は終了時刻を示す。
- 例外はすべて `throws` で上位（JVM）に委譲する。`main` 内での個別回復処理は行わない。
- 処理途中で例外が発生した場合、`outLog()` に到達しないためログファイルは生成されない。

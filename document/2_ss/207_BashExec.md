# クラス詳細設計：BashExec

## 基本情報

| 項目             | 内容                                                                                                              |
|------------------|-------------------------------------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.BashExec`                                                                                           |
| ファイル名       | `BashExec.java`                                                                                                   |
| 種別             | class（public）                                                                                                   |
| 責務             | Java の `ProcessBuilder` を使用してローカルサーバ（自サーバ）上でBashコマンドを実行する。標準出力をログに記録する |
| 依存クラス       | `Main`, `WksConstants`                                                                                            |

---

## フィールド一覧

| フィールド名 | 型                  | 修飾子                 | 初期値     | 説明                                                                      |
|--------------|---------------------|------------------------|------------|---------------------------------------------------------------------------|
| `log`        | `ArrayList<String>` | `private static final` | `Main.log` | `Main.log` への参照。実行コマンドと標準出力をログに追記するために使用する |

---

## メソッド一覧

| メソッド名   | 戻り値型   | 修飾子           | 説明                                                   |
|--------------|------------|------------------|--------------------------------------------------------|
| `update`     | `void`     | `public static`  | `sudo apt update` をローカルで実行する                 |
| `upgrade`    | `void`     | `public static`  | `sudo apt upgrade -y` をローカルで実行する             |
| `shutdown`   | `void`     | `public static`  | 60秒後に再起動するコマンドをバックグラウンドで起動する |
| `waitOneMin` | `void`     | `public static`  | 管理端末でsleep 60の完了を待つ                         |
| `getNowTime` | `long`     | `public static`  | dateの先頭行をUNIX秒として返す                         |
| `runCommand` | `String[]` | `private static` | 任意コマンドの実行・出力読み取り・ログ記録の共通処理   |

---

## メソッド詳細

### `update()`

```java
public static void update() throws IOException, InterruptedException;
```

`runCommand(WksConstants.CMD_UPDATE)` を呼び出す単純なラッパー。

---

### `upgrade()`

```java
public static void upgrade() throws IOException, InterruptedException;
```

`runCommand(WksConstants.CMD_UPGRADE)` を呼び出す単純なラッパー。

---

### `shutdown()`

```java
public static void shutdown() throws IOException;
```

#### 概要

60秒待機後に再起動するコマンドをバックグラウンド（`&`）で起動する。  
プロセスの終了を待機しないため、`main` メソッドはこのコマンド起動後すぐに処理を続行（終了）できる。

#### 処理フロー

```
1. shutdownCmd = WksConstants.CMD_SLEEP_SHUTDOWN
   → "(sleep 60 && sudo shutdown -r now) &"
2. cmd[] = {"sh", "-c", shutdownCmd}
3. new ProcessBuilder(cmd)
4. processBuilder.start()  ← waitFor() を呼ばない（バックグラウンド）
5. log に "$ {shutdownCmd}" を追記
```

#### 例外

| 例外クラス    | 発生条件         |
|---------------|------------------|
| `IOException` | プロセス起動失敗 |

> `InterruptedException` が発生しないのは `process.waitFor()` を呼ばないため。

#### SSHコマンドの `shutdown` との相違点

| 項目         | `SshCommand.shutdown()`        | `BashExec.shutdown()`                  |
|--------------|--------------------------------|----------------------------------------|
| コマンド     | `sudo shutdown -r now`         | `(sleep 60 && sudo shutdown -r now) &` |
| 実行方式     | SSH / 即時実行                 | Bash / バックグラウンド・60秒遅延      |
| プロセス待機 | `SshExec.execute()` で完了待ち | `start()` のみ（待機なし）             |
| 目的         | リモートサーバの即時再起動     | 自サーバをバッチ終了後に再起動         |

---

### `waitOneMin()`

```java
public static void waitOneMin() throws IOException, InterruptedException;
```

`runCommand(CMD_WAIT_ONE_MIN)` を呼び、管理端末の `sleep 60` を同期待機する。
両サービス停止後に1回、全台への再起動指示後に1回利用する。接続先でのsleepは行わない。

### `getNowTime()`

```java
public static long getNowTime() throws IOException, InterruptedException;
```

`runCommand(CMD_GET_TIME)` の先頭行を `Long.parseLong` で変換して返す。
コマンドは `date '+%s'` で、単位は秒。復帰待ちの開始時刻と現在時刻に使用する。
先頭行の存在と整数形式を前提とし、不正値なら非検査例外がMainへ伝播する。

---

### `runCommand(String cmd)` ※private

```java
private static String[] runCommand(String cmd) throws IOException, InterruptedException;
```

#### 概要

指定されたコマンドを `ProcessBuilder` で実行し、標準出力をログに記録する共通処理。  
`update`・`upgrade`・`waitOneMin`・`getNowTime` から呼ばれる。

#### 処理フロー

```
1. new ProcessBuilder("sh", "-c", cmd)
2. process = processBuilder.start()
3. process.waitFor()  ← コマンド完了まで同期待機

[標準出力の読み取り]
4. InputStreamReader(process.getInputStream(), UTF_8) → inputStreamReader
5. BufferedReader(inputStreamReader) → bufferedReader

[ログ書き込み]
6. log に "$ {cmd}" を追記

[出力の行読み取りループ]
7. loop:
     line = bufferedReader.readLine()
     if (line == null): break
     log.add(line)
     retList.add(line)

[クローズ]
8. bufferedReader.close()
9. inputStreamReader.close()
10. 読み取った全行をString[]に格納して返却
```

#### 引数

| 引数名 | 型       | 説明                       |
|--------|----------|----------------------------|
| `cmd`  | `String` | 実行するBashコマンド文字列 |

#### 例外

| 例外クラス             | 発生条件                                       |
|------------------------|------------------------------------------------|
| `IOException`          | プロセス起動失敗、または標準出力の読み取り失敗 |
| `InterruptedException` | `process.waitFor()` 中の割り込み               |

---

## 設計上の注意点

- `runCommand` では `process.waitFor()` の後に `getInputStream()`
  で出力を読み取っている。プロセス完了後に読み取るため、出力バッファがオーバーフローするほど大量の出力がある場合にデッドロックのリスクがある（
  `apt upgrade` の出力量によっては注意が必要）。
- `SshExec` と異なり、実行済みフラグ（`executed`）を持たないため、同じコマンドを複数回呼ぶことが可能。
- `shutdown()` は `InterruptedException` を `throws` しないが、`update()`・`upgrade()` は `throws InterruptedException`
  を宣言している点に注意。

- `runCommand` の戻り値は標準出力の行配列（空出力は長さ0）。実行コマンドと各行は従来どおりログにも記録する。
- `waitOneMin`・`getNowTime` のプロセス起動・読み取り失敗は `IOException`、完了待ち割り込みは `InterruptedException`
  として上位へ伝播する。

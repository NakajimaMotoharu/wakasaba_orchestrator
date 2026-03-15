# wakasaba_orchestrator 基本設計書：BashExec

## 基本情報

| 項目       | 内容                                                                       |
|----------|--------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.BashExec`                                                  |
| ファイル名    | `BashExec.java`                                                          |
| 種別       | class（public）                                                            |
| 責務       | Java の `ProcessBuilder` を使用してローカルサーバ（自サーバ）上で Bash コマンドを実行する。標準出力をログに記録する |
| 主な依存クラス  | `Main`、`WksConstants`                                                    |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                    | 説明                                         |
|--------|---------------------|------------------------|--------------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` への参照。実行コマンドと標準出力をログに追記するために使用する |

---

## メソッド一覧

| メソッド名        | 戻り値型   | 修飾子              | 説明                               |
|--------------|--------|------------------|----------------------------------|
| `update`     | `void` | `public static`  | `sudo apt update` をローカルで実行する     |
| `upgrade`    | `void` | `public static`  | `sudo apt upgrade -y` をローカルで実行する |
| `shutdown`   | `void` | `public static`  | 60 秒後に再起動するコマンドをバックグラウンドで起動する    |
| `runCommand` | `void` | `private static` | コマンド実行・出力読み取り・ログ記録の共通処理          |

---

## 処理フロー

### `update` / `upgrade`

`runCommand(CMD_UPDATE)` / `runCommand(CMD_UPGRADE)` を呼び出す単純なラッパー。

---

### `shutdown()`

60 秒待機後に再起動するコマンドをバックグラウンドで起動する。プロセスの終了を待機しない。

```
1. shutdownCmd = CMD_SLEEP_SHUTDOWN（"(sleep 60 && sudo shutdown -r now) &"）
2. cmd = {"sh", "-c", shutdownCmd}
3. new ProcessBuilder(cmd)
4. processBuilder.start()  ← waitFor() を呼ばない（バックグラウンド実行）
5. log に "$ {shutdownCmd}" を追記
```

---

### `runCommand(String cmd)` ※private

```
1. new ProcessBuilder("sh", "-c", cmd)
2. process = processBuilder.start()
3. process.waitFor()  ← コマンド完了まで同期待機
4. InputStreamReader(process.getInputStream(), UTF_8) → inputStreamReader
5. BufferedReader(inputStreamReader) → bufferedReader
6. log に "$ {cmd}" を追記
7. loop:
     line = bufferedReader.readLine()
     if (line == null): break
     log.add(line)
8. bufferedReader.close()
9. inputStreamReader.close()
```

---

## `SshCommand.shutdown()` との比較

| 項目     | `SshCommand.shutdown()`   | `BashExec.shutdown()`                  |
|--------|---------------------------|----------------------------------------|
| コマンド   | `sudo shutdown -r now`    | `(sleep 60 && sudo shutdown -r now) &` |
| 実行方式   | SSH / 即時実行                | Bash / バックグラウンド・60 秒遅延                 |
| プロセス待機 | `SshExec.execute()` で完了待ち | `start()` のみ（待機なし）                     |
| 目的     | リモートサーバの即時再起動             | 自サーバをバッチ終了後に再起動                        |

---

## 例外

| メソッド名      | 例外クラス                  | 発生条件                       |
|------------|------------------------|----------------------------|
| `update`   | `IOException`          | プロセス起動失敗・標準出力読み取り失敗        |
| `update`   | `InterruptedException` | `process.waitFor()` 中の割り込み |
| `upgrade`  | `IOException`          | プロセス起動失敗・標準出力読み取り失敗        |
| `upgrade`  | `InterruptedException` | `process.waitFor()` 中の割り込み |
| `shutdown` | `IOException`          | プロセス起動失敗                   |

---

## 設計上の注意点

- `runCommand` では `process.waitFor()` の後に `getInputStream()` で出力を読み取るため、出力バッファがオーバーフローするほど大量の出力がある場合にデッドロックのリスクがある。
- `SshExec` と異なり実行済みフラグを持たないため、同じコマンドを複数回呼ぶことが可能。
- `shutdown()` は `InterruptedException` を宣言しないが、`update()` / `upgrade()` は宣言している点に注意。

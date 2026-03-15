# wakasaba_orchestrator 基本設計書：SshExec

## 基本情報

| 項目       | 内容                                                                             |
|----------|--------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.SshExec`                                                         |
| ファイル名    | `SshExec.java`                                                                 |
| 種別       | class（public）                                                                  |
| 責務       | JSch ライブラリを使用して SSH セッションを確立し、1コマンドを実行して標準出力を返す。接続確認（疎通チェック）機能も提供する            |
| 主な依存クラス  | `ConnectionInformation`、`WksConstants`                                         |
| 依存ライブラリ  | `com.jcraft.jsch.JSch`、`com.jcraft.jsch.Session`、`com.jcraft.jsch.ChannelExec` |

---

## フィールド一覧

| フィールド名     | 型                       | 修飾子             | 説明                                |
|------------|-------------------------|-----------------|-----------------------------------|
| `ci`       | `ConnectionInformation` | `private final` | SSH 接続に使用するサーバ接続情報                |
| `cmd`      | `String`                | `private final` | 実行対象コマンド文字列                       |
| `executed` | `boolean`               | `private`       | コマンドの実行済みフラグ（2重実行防止用）。初期値 `false` |

---

## コンストラクタ

### `SshExec(ConnectionInformation ci, String cmd)`

```
1. this.ci = ci
2. this.cmd = cmd
3. this.executed = false
```

---

## メソッド一覧

| メソッド名                | 戻り値型       | 修飾子       | 説明                           |
|----------------------|------------|-----------|------------------------------|
| `isAlive`            | `boolean`  | `public`  | 対象サーバへの SSH 接続可否を確認して返す      |
| `execute`            | `String[]` | `public`  | コマンドを実行し、標準出力を行配列で返す         |
| `getSessionInstance` | `Session`  | `private` | JSch セッションインスタンスを生成して返す（未接続） |

---

## 処理フロー

### `isAlive()`

`SshCommand.waitForBecomeActive()` から呼ばれ、サーバ応答確認のポーリングに使用される。

```
1. getSessionInstance() → session
2. session.connect() を試みる
   成功 → session.disconnect()、true を返す
   JSchException 発生 → false を返す（接続不可として吸収）
```

---

### `execute()`

SSH セッションを確立し、コマンドを `ChannelExec` モードで実行する。標準出力を全行収集して配列で返す。

```
1. executed == true → null を返してリターン（2重実行ガード）
2. executed = true
3. getSessionInstance() → session
4. session.connect()
5. session.openChannel("exec") → ChannelExec
6. channelExec.setCommand(cmd)
7. channelExec.getInputStream() → inputStream
8. channelExec.connect()
9. BufferedReader(InputStreamReader(inputStream, UTF_8)) を生成

[読み取りループ]
10. loop:
      while (bufferedReader.ready()):
        responseList.add(bufferedReader.readLine())
      if (channelExec.isClosed()): break
      Thread.sleep(1000)

11. channelExec.disconnect()
12. session.disconnect()
13. responseList を String[] に変換して返す
```

---

### `getSessionInstance()` ※private

```
1. new JSch() → jSch
2. jSch.addIdentity(ci.key())  ← 秘密鍵を登録
3. jSch.getSession(ci.user(), ci.host(), ci.port()) → session
4. session.setConfig("StrictHostKeyChecking", "no")  ← known_hosts チェック無効化
5. session を返す（この時点では未接続）
```

---

## 例外

| メソッド名     | 例外クラス                  | 発生条件                                     |
|-----------|------------------------|------------------------------------------|
| `isAlive` | `JSchException`        | セッション生成失敗（秘密鍵読込エラー等。接続失敗は吸収して false を返す） |
| `execute` | `JSchException`        | SSH 接続・チャンネル接続失敗                         |
| `execute` | `IOException`          | 標準出力の読み取り失敗                              |
| `execute` | `InterruptedException` | `Thread.sleep` 中の割り込み                    |

---

## 設計上の注意点

- `executed` フラグにより、同一インスタンスで `execute()` を2回呼ぶことはできない。再実行が必要な場合は新しいインスタンスを生成する。
- `StrictHostKeyChecking=no` により known_hosts 検証を行わないため、再起動後のサーバへの接続が失敗しない。
- `isAlive()` における `session.connect()` の `JSchException` は接続不可として吸収するが、`getSessionInstance()` 内での
  `JSchException`（秘密鍵読込失敗等）は呼出し元に伝播する。
- 読み取りループは `channelExec.isClosed()` でコマンド完了を検知する。

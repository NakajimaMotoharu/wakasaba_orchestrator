# クラス詳細設計：SshExec

## 基本情報

| 項目       | 内容                                                                               |
|----------|----------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.SshExec`                                                           |
| ファイル名    | `SshExec.java`                                                                   |
| 種別       | class（public）                                                                    |
| 責務       | JSch ライブラリを使用してSSHセッションを確立し、1コマンドを実行して標準出力を返す。接続確認（疎通チェック）機能も提供する                |
| 依存クラス    | `ConnectionInformation`, `WksConstants`                                          |
| 依存ライブラリ  | `com.jcraft.jsch.JSch`, `com.jcraft.jsch.Session`, `com.jcraft.jsch.ChannelExec` |

---

## フィールド一覧

| フィールド名     | 型                       | 修飾子             | 説明                                |
|------------|-------------------------|-----------------|-----------------------------------|
| `ci`       | `ConnectionInformation` | `private final` | SSH接続に使用するサーバ接続情報                 |
| `cmd`      | `String`                | `private final` | 実行対象コマンド文字列                       |
| `executed` | `boolean`               | `private`       | コマンドの実行済みフラグ（2重実行防止用）。初期値 `false` |

---

## コンストラクタ

### `SshExec(ConnectionInformation ci, String cmd)`

```java
public SshExec(ConnectionInformation ci, String cmd);
```

#### 処理フロー

```
1. this.ci = ci
2. this.cmd = cmd
3. this.executed = false
```

#### 引数

| 引数名   | 型                       | 説明          |
|-------|-------------------------|-------------|
| `ci`  | `ConnectionInformation` | 接続先サーバ情報    |
| `cmd` | `String`                | 実行するコマンド文字列 |

---

## メソッド一覧

| メソッド名                | 戻り値型       | 修飾子       | 説明                      |
|----------------------|------------|-----------|-------------------------|
| `isAlive`            | `boolean`  | `public`  | 対象サーバへのSSH接続可否を確認する     |
| `execute`            | `String[]` | `public`  | コマンドを実行し、標準出力を行配列で返す    |
| `getSessionInstance` | `Session`  | `private` | JSch セッションインスタンスを生成して返す |

---

## メソッド詳細

### `isAlive()`

```java
public boolean isAlive() throws JSchException;
```

#### 概要

接続情報に基づいてSSH接続を試み、成功すれば `true`、失敗すれば `false` を返す。  
`SshCommand.waitForBecomeActive()` から繰り返し呼ばれ、サーバが応答可能になるまでのポーリングに使用される。

#### 処理フロー

```
1. getSessionInstance() → session
2. session.connect() を試みる
   成功 → session.disconnect()、true を返す
   JSchException 発生 → false を返す
```

#### 戻り値

| 型         | 説明                                                  |
|-----------|-----------------------------------------------------|
| `boolean` | `true`: SSH接続成功（サーバ生存）/ `false`: 接続失敗（サーバ未応答・再起動中等） |

#### 例外

| 例外クラス           | 発生条件                                    |
|-----------------|-----------------------------------------|
| `JSchException` | セッションインスタンスの生成失敗（秘密鍵読み込みエラー等、接続失敗以外の例外） |

> `session.connect()` が投げる `JSchException` は接続不可として `false` で吸収する。それ以外の `JSchException`
> （セッション生成失敗等）は呼び出し元に伝播する。

---

### `execute()`

```java
public String[] execute() throws JSchException, IOException, InterruptedException;
```

#### 概要

SSHセッションを確立し、コンストラクタで指定されたコマンドを `ChannelExec` モードで実行する。  
標準出力をすべて読み取り、行の配列として返す。`executed` フラグにより2重実行を防止する。

#### 処理フロー

```
1. executed == true の場合 → null を返してリターン（2重実行ガード）
2. executed = true に設定
3. getSessionInstance() → session
4. session.connect()
5. session.openChannel("exec") → ChannelExec として cast
6. channelExec.setCommand(cmd)
7. channelExec.getInputStream() → inputStream
8. channelExec.connect()
9. BufferedReader(InputStreamReader(inputStream, UTF_8)) を生成

[読み取りループ]
10. loop:
      while (bufferedReader.ready()):
        responseList.add(bufferedReader.readLine())
      if (channelExec.isClosed()):
        break
      Thread.sleep(1000)

11. channelExec.disconnect()
12. session.disconnect()

[結果変換]
13. responseList を String[] に変換して返却
```

#### 読み取りロジックの詳細

```
outer loop:
  inner loop:
    bufferedReader.ready() が true の間 → readLine() で1行読み取り → responseList に追加
  channelExec.isClosed() が true → outer loop を break
  そうでなければ Thread.sleep(1000) → 次のデータ到着を待つ
```

> この方式により、コマンドの実行が完了するまで出力を逃さず収集できる。チャンネルが閉じる = コマンド完了の合図。

#### 戻り値

| 型          | 説明                                                     |
|------------|--------------------------------------------------------|
| `String[]` | コマンドの標準出力を行に分割した配列。出力がなければ空配列。`executed` 済みの場合は `null` |

#### 例外

| 例外クラス                  | 発生条件                  |
|------------------------|-----------------------|
| `JSchException`        | SSH接続・チャンネル接続失敗       |
| `IOException`          | 標準出力の読み取り失敗           |
| `InterruptedException` | `Thread.sleep` 中の割り込み |

---

### `getSessionInstance()` ※private

```java
private Session getSessionInstance() throws JSchException;
```

#### 概要

JSch を使用してSSHセッションオブジェクトを生成する。この時点では接続（`connect()`）は行わない。

#### 処理フロー

```
1. new JSch() → jSch
2. jSch.addIdentity(ci.key())  ← 秘密鍵を登録
3. jSch.getSession(ci.user(), ci.host(), ci.port()) → session
4. session.setConfig("StrictHostKeyChecking", "no")  ← known_hostsチェック無効化
5. session を返却
```

#### 戻り値

| 型         | 説明                   |
|-----------|----------------------|
| `Session` | 未接続状態のSSHセッションインスタンス |

#### 例外

| 例外クラス           | 発生条件                    |
|-----------------|-------------------------|
| `JSchException` | 秘密鍵の読み込み失敗、またはセッション生成失敗 |

---

## 設計上の注意点

- `executed` フラグにより、同一インスタンスで `execute()` を2回呼ぶことは不可能。再実行が必要な場合は新しいインスタンスを生成する必要がある。
- `StrictHostKeyChecking=no` を設定しているため、既知ホストの検証を行わない。セキュリティ上のトレードオフとして、新しいサーバや再起動後のサーバへの接続が失敗しない。
- `execute()` 内の読み取りループは `channelExec.isClosed()` をトリガーに終了するが、`isClosed()` チェック前の最後の
  `ready()` ループで残りの出力を取得する設計になっている。
- `isAlive()` は新しい `Session` インスタンスを毎回生成するが、`getSessionInstance()` を使い回しているため JSch
  インスタンスも毎回生成される。

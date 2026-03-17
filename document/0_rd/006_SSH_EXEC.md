# wakasaba_orchestrator 要件定義書：SshExec

## 基本情報

| 項目       | 内容                                                                                                                                                           |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.util.SshExec`                                                                                                                                       |
| ファイル名    | `SshExec.java`                                                                                                                                               |
| 種別       | class（public）                                                                                                                                                |
| 責務       | JSchライブラリを使用してSSHセッションを確立し、1コマンドを実行して標準出力を返す。接続確認（疎通チェック）機能も提供する                                                                                             |
| 主な依存クラス  | `ConnectionInformation`、`WksConstants`                                                                                                                       |
| 依存ライブラリ  | JSch（`com.jcraft.jsch.JSch`、`com.jcraft.jsch.Session`、`com.jcraft.jsch.ChannelExec`）（build.gradle: `com.github.mwiede:jsch`、JCraft 版 JSch のフォーク版が同パッケージ名で提供） |

---

## 要求機能

### SE-01：SSHセッションの生成

- `ConnectionInformation`（host/port/user/key）を用いてJSchの `Session` インスタンスを生成できること。
- 秘密鍵ファイルを `addIdentity` に登録すること。
- SSH接続設定 `StrictHostKeyChecking=no` を適用し、known_hostsの検証を無効化すること（再起動後のサーバへの接続失敗を防ぐため）。
- セッション生成時点では未接続状態とすること。

### SE-02：SSHコマンドの実行と標準出力の取得

- 接続情報とコマンド文字列をもとにSSHセッションを確立し、`ChannelExec` モードで1コマンドを実行できること。
- コマンド実行中の標準出力を全行収集し、`String[]` として返すこと。
- コマンド完了は `ChannelExec.isClosed()` で検知すること。完了まで1秒間隔でポーリングを行うこと。
- 処理完了後、チャンネルおよびセッションを切断すること。
- 同一インスタンスで2回以上の実行を防止する機構（実行済みフラグ）を持つこと。2回目以降の呼び出しには `null` を返すこと。

### SE-03：SSH疎通確認（接続可否チェック）

- 対象サーバへのSSH接続が可能かどうかを `boolean` で返せること。
- 接続成功時は `session.disconnect()` 後に `true` を返すこと。
- `session.connect()` が投げるすべての `JSchException` を吸収し `false` を返すこと。これにより再起動中のサーバへのポーリング待機が実現されること。
- `getSessionInstance()` 内での `JSchException`（秘密鍵読込エラー等、セッション生成に起因するもの）は吸収せず呼び出し元へ伝播させること。

---

## 要求インタフェース

### フィールド

| フィールド名     | 型                       | 修飾子             | 初期値            | 要件                                |
|------------|-------------------------|-----------------|----------------|-----------------------------------|
| `ci`       | `ConnectionInformation` | `private final` | （コンストラクタ引数で設定） | SSH接続に使用するサーバ接続情報                 |
| `cmd`      | `String`                | `private final` | （コンストラクタ引数で設定） | 実行対象のコマンド文字列                      |
| `executed` | `boolean`               | `private`       | `false`        | コマンドの実行済みフラグ（2重実行防止用）。初期値 `false` |

### コンストラクタ

- `SshExec(ConnectionInformation ci, String cmd)` によって `ci`・`cmd`・`executed` を初期化できること。

### メソッド

| メソッド名                | 戻り値型       | 修飾子       | 要件概要                                                  |
|----------------------|------------|-----------|-------------------------------------------------------|
| `isAlive`            | `boolean`  | `public`  | SSH接続可否を確認して返す。接続失敗（`JSchException`）は `false` として吸収する |
| `execute`            | `String[]` | `public`  | コマンドを実行し、標準出力を行配列で返す。実行済みの場合は `null` を返す              |
| `getSessionInstance` | `Session`  | `private` | JSchセッションインスタンスを生成して返す（未接続）                           |

---

## 例外要件

| メソッド名     | 例外クラス                  | 想定発生状況                                                                             |
|-----------|------------------------|------------------------------------------------------------------------------------|
| `isAlive` | `JSchException`        | セッション生成失敗（秘密鍵読込エラー等）。`session.connect()` が投げる `JSchException` はすべて吸収して `false` を返す |
| `execute` | `JSchException`        | SSH接続・チャンネル接続失敗                                                                    |
| `execute` | `IOException`          | 標準出力の読み取り失敗                                                                        |
| `execute` | `InterruptedException` | `Thread.sleep` 中の割り込み                                                              |

---

## 制約・注意事項

- `executed` フラグにより、同一インスタンスで `execute()` を2回呼ぶことはできない。再実行が必要な場合は新しいインスタンスを生成すること。
- `StrictHostKeyChecking=no` を設定するため、known_hosts の検証は行わない。これは再起動後のサーバへの接続を確実に行うための設計上の判断である。
- `isAlive()` での例外吸収は `session.connect()` が投げるすべての `JSchException` を対象とする。`getSessionInstance()`
  内での `JSchException`（秘密鍵読込失敗等）は上位へ伝播させること。
- 標準出力の読み取りには文字コード UTF-8 を使用すること。

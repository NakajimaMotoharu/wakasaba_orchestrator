# クラス詳細設計：SshCommand

## 基本情報

| 項目       | 内容                                                                                |
|----------|-----------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.cmd.SshCommand`                                                          |
| ファイル名    | `SshCommand.java`                                                                 |
| 種別       | class（public）                                                                     |
| 責務       | SSH経由で実行する各種コマンドの業務ロジックラッパー。コマンドの組み立て・実行・ログ記録を集約する                                |
| 依存クラス    | `Main`, `SshExec`, `ConnectionInformation`, `Curl`, `PaperUrlGen`, `WksConstants` |

---

## フィールド一覧

| フィールド名 | 型                   | 修飾子                    | 初期値        | 説明                                         |
|--------|---------------------|------------------------|------------|--------------------------------------------|
| `log`  | `ArrayList<String>` | `private static final` | `Main.log` | `Main.log` への参照。実行コマンドと標準出力をログに追記するために使用する |

---

## メソッド一覧

| メソッド名                 | 戻り値型   | 修飾子              | 説明                                           |
|-----------------------|--------|------------------|----------------------------------------------|
| `update`              | `void` | `public static`  | `sudo apt update` を実行する                      |
| `upgrade`             | `void` | `public static`  | `sudo apt upgrade -y` を実行する                  |
| `shutdown`            | `void` | `public static`  | `sudo shutdown -r now` を実行する                 |
| `wgetPaperMc`         | `void` | `public static`  | PaperMC・Pl3xMap の最新版をダウンロードする                |
| `movePaperMc`         | `void` | `public static`  | ダウンロードしたPaperMC・Pl3xMapをSHA検証後に本番ディレクトリへ移動する |
| `startPaperMC`        | `void` | `public static`  | `sudo systemctl start papermc` を実行する         |
| `stopPaperMC`         | `void` | `public static`  | `sudo systemctl stop papermc` を実行する          |
| `backupPaperMC`       | `void` | `public static`  | バックアップシェルを実行する                               |
| `runCommand`          | `void` | `private static` | 任意コマンドの実行・ログ記録の共通処理                          |
| `waitForBecomeActive` | `void` | `private static` | 対象サーバが疎通可能になるまでポーリング待機する                     |

---

## メソッド詳細

### `update(ConnectionInformation ci)`

```java
public static void update(ConnectionInformation ci)
		throws JSchException, InterruptedException, IOException;
```

`runCommand(ci, WksConstants.CMD_UPDATE)` を呼び出す単純なラッパー。

---

### `upgrade(ConnectionInformation ci)`

```java
public static void upgrade(ConnectionInformation ci)
		throws JSchException, InterruptedException, IOException;
```

`runCommand(ci, WksConstants.CMD_UPGRADE)` を呼び出す単純なラッパー。

---

### `shutdown(ConnectionInformation ci)`

```java
public static void shutdown(ConnectionInformation ci)
		throws JSchException, InterruptedException, IOException;
```

`runCommand(ci, WksConstants.CMD_SHUTDOWN)` を呼び出す単純なラッパー。  
対象サーバは即時再起動する（`sudo shutdown -r now`）。

---

### `wgetPaperMc(ConnectionInformation ci)`

```java
public static void wgetPaperMc(ConnectionInformation ci)
		throws IOException, InterruptedException, JSchException;
```

PaperMCおよびPl3xMapの最新版JARファイルをリモートサーバ上にダウンロードする。

#### 処理フロー

```
1. userAgent = WksConstants.OTHER_USER_AGENT を設定
2. Curl.exec(userAgent, URL_PAPERMC_VERSION) → versionJson
3. PaperUrlGen.getPaperMcVersion(versionJson) → version（例: "1.21.4"）
4. Curl.exec(userAgent, URL_PAPERMC_DL_URL % version) → urlJson
5. PaperUrlGen.getPaperMcUrl(urlJson) → url（PaperMC JARのダウンロードURL）
6. cmd = CMD_WGET_PAPERMC % (userAgent, url) を組み立て
7. runCommand(ci, cmd) → リモートサーバ上でwgetを実行、PATH_DL_PAPERMCに保存

8. Curl.exec(userAgent, URL_PL3XMAP_DL_URL) → pl3xMapUrlJson
9. PaperUrlGen.getPl3xMapUrl(pl3xMapUrlJson, version) → pl3xMapUrl
   ├─ pl3xMapUrl == null の場合: Pl3xMapのダウンロードはスキップ
   └─ pl3xMapUrl != null の場合:
      10. cmd = CMD_WGET_PL3XMAP % (userAgent, pl3xMapUrl) を組み立て
      11. runCommand(ci, cmd) → リモートサーバ上でwgetを実行、PATH_DL_PL3XMAPに保存
```

> Pl3xMapのダウンロードは、Pl3xMapの対応バージョンがPaperMC最新版と一致する場合のみ実行される。

---

### `movePaperMc(ConnectionInformation ci)`

```java
public static void movePaperMc(ConnectionInformation ci)
		throws IOException, InterruptedException, JSchException;
```

ダウンロード済みのPaperMC・Pl3xMap JARファイルをSHAチェックサムで検証し、正常な場合のみ本番ディレクトリへ移動する。

#### 処理フロー

```
[事前情報取得]
1. Curl.exec → versionJson → version
2. Curl.exec → sha256Json → expectedSHA256（PaperMCのSHA256期待値）
3. Curl.exec → sha512Json → expectedSHA512（Pl3xMapのSHA512期待値、バージョン不一致時null）

[PaperMC検証・配置]
4. waitForBecomeActive(ci)
5. SshExec(ci, CMD_PAPERMC_HASH).execute() → ret（sha256sum出力）
6. log にコマンドと結果を追記
7. ret[0].substring(0, 64) == expectedSHA256 ?
   Yes:
     8.  waitForBecomeActive(ci)
     9.  SshExec(ci, CMD_PAPERMC_RM).execute()  → 本番JARを削除
     10. log に追記
     11. waitForBecomeActive(ci)
     12. SshExec(ci, CMD_PAPERMC_MV).execute()  → DLファイルを本番へ移動
     13. log に追記
   No: スキップ（ハッシュ不一致のためファイルは移動しない）

[Pl3xMap旧ファイル削除（常に実行）]
14. waitForBecomeActive(ci)
15. SshExec(ci, CMD_PL3XMAP_RM).execute()  → 本番プラグインJARを削除
16. log に追記

[Pl3xMap検証・配置（expectedSHA512 != null の場合のみ）]
17. waitForBecomeActive(ci)
18. SshExec(ci, CMD_PL3XMAP_HASH).execute() → ret（sha512sum出力）
19. log に追記
20. ret[0].substring(0, 128) == expectedSHA512 ?
    Yes:
      21. waitForBecomeActive(ci)
      22. SshExec(ci, CMD_PL3XMAP_MV).execute() → DLファイルを本番へ移動
      23. log に追記
    No: スキップ
```

#### ハッシュ検証仕様

| 対象          | アルゴリズム  | 期待値取得元                          | 比較文字数   |
|-------------|---------|---------------------------------|---------|
| PaperMC JAR | SHA-256 | PaperMC API（`checksums.sha256`） | 先頭64文字  |
| Pl3xMap JAR | SHA-512 | Modrinth API（`hashes.sha512`）   | 先頭128文字 |

---

### `startPaperMC(ConnectionInformation ci)`

```java
public static void startPaperMC(ConnectionInformation ci)
		throws JSchException, InterruptedException, IOException;
```

`runCommand(ci, WksConstants.CMD_PAPERMC_START)` を呼び出す単純なラッパー。

---

### `stopPaperMC(ConnectionInformation ci)`

```java
public static void stopPaperMC(ConnectionInformation ci)
		throws JSchException, InterruptedException, IOException;
```

`runCommand(ci, WksConstants.CMD_PAPERMC_END)` を呼び出す単純なラッパー。

---

### `backupPaperMC(ConnectionInformation ci)`

```java
public static void backupPaperMC(ConnectionInformation ci)
		throws JSchException, InterruptedException, IOException;
```

`runCommand(ci, WksConstants.CMD_PAPERMC_BACKUP)` を呼び出す単純なラッパー。  
`sh /home/mini/mcs/shell/backup.sh` を実行する。

---

### `runCommand(ConnectionInformation ci, String cmd)` ※private

```java
private static void runCommand(ConnectionInformation ci, String cmd)
		throws JSchException, InterruptedException, IOException;
```

コマンド実行の共通処理。`update`・`upgrade`・`shutdown`・`startPaperMC`・`stopPaperMC`・`backupPaperMC` から呼ばれる（
`movePaperMc` は内部で直接 `SshExec` を操作するため、このメソッドを経由しない）。

#### 処理フロー

```
1. waitForBecomeActive(ci) → サーバが応答するまで待機
2. SshExec(ci, cmd) のインスタンスを生成
3. sshExec.execute() → ret（標準出力の行配列）
4. log に "$ {cmd}" を追記
5. log に ret の全要素を追記
```

---

### `waitForBecomeActive(ConnectionInformation ci)` ※private

```java
private static void waitForBecomeActive(ConnectionInformation ci)
		throws JSchException, InterruptedException;
```

対象サーバへのSSH接続が成功するまで1秒間隔でポーリングする。  
`shutdown` 実行後の再起動待ちで主に使用される。

#### 処理フロー

```
1. SshExec(ci, CMD_DO_NOTHING) のインスタンスを生成
2. while (!sshExec.isAlive()):
     Thread.sleep(1000)
3. isAlive() が true になったらループを抜けてリターン
```

| 使用定数             | 値     | 意味                       |
|------------------|-------|--------------------------|
| `CMD_DO_NOTHING` | `":"` | Bashのno-opコマンド。接続確認のみに使用 |

---

## 設計上の注意点

- `runCommand` はすべてのコマンド実行の前に `waitForBecomeActive` を呼ぶため、サーバが再起動中であっても応答待ちで自動リトライされる。
- `movePaperMc` におけるPl3xMapの旧ファイル削除（`CMD_PL3XMAP_RM`）は、新バージョンのダウンロード有無に関わらず**常に実行**
  される。これにより、バージョン不一致時でも旧ファイルは削除される。
- `wgetPaperMc` と `movePaperMc` の両方でAPIを呼び出してバージョン情報を取得しており、処理の間にAPIレスポンスが変わるケースへの考慮はない（実用上ほぼ問題なし）。

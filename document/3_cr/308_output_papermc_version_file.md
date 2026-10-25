# 変更管理資料 No.308：PaperMCバージョンファイルの作成

## 基本情報

| 項目             | 内容                                                                      |
|------------------|---------------------------------------------------------------------------|
| 変更管理No       | 308                                                                       |
| 変更概要         | PaperMCダウンロード時に、取得対象のバージョンを管理端末上のファイルへ出力 |
| ステータス       | 適用済み（ソース確認・PGレビュー・設計書反映済み）                        |
| 変更対象ファイル | `SshCommand.java`、`WksConstants.java`                                    |
| 完全修飾クラス名 | `com.wks.cmd.SshCommand`、`com.wks.parts.WksConstants`                    |

---

## 変更背景・目的

### 変更前の課題

変更前の `SshCommand.wgetPaperMc` は、APIからPaperMCのバージョンを取得し、そのバージョンのJARをリモートサーバへダウンロードしていた。
取得対象のバージョン文字列はメソッド内のローカル変数として保持され、管理端末に専用ファイルとして保存されていなかった。

### 変更目的

wakasaba_orchestrator実行端末（管理端末）にPaperMCのバージョンファイルを作成し、
ダウンロード処理で使用したバージョンをファイルから確認できるようにする。

---

## 変更内容

### 変更ファイル：`SshCommand.java`

#### 変更箇所：`wgetPaperMc` メソッドの末尾

既存のPaperMCおよびPl3xMapのダウンロード処理後に、`PrintStream` によるバージョンファイル出力を追加した。
以下のコードは変更後のメソッドを抜粋し、既存のダウンロード処理の詳細を省略している。

```java
public class SshCommand {
    public static void wgetPaperMc(ConnectionInformation ci)
            throws IOException, InterruptedException, JSchException {
        String userAgent = WksConstants.OTHER_USER_AGENT;
        String versionJson = Curl.exec(userAgent, WksConstants.URL_PAPERMC_VERSION);
        String version = PaperUrlGen.getPaperMcVersion(versionJson);

        // 既存のPaperMCおよびPl3xMapのダウンロード処理（省略）

        // PaperMCのバージョンファイル出力
        PrintStream ps = new PrintStream(WksConstants.PATH_PAPERMC_VERSION);
        // バージョン情報書き込み
        ps.print(version);
        // ファイルクローズ
        ps.close();
    }
}
```

- `java.io.PrintStream` のインポートを追加した。
- ファイルには、同メソッドのダウンロード処理で使用した `version` を出力する。
- ファイル出力は管理端末のJavaプロセスで直接実行する。
- Pl3xMapのダウンロードがバージョン不一致によりスキップされた場合も、その後のファイル出力を実行する。

---

### 変更ファイル：`WksConstants.java`

#### 追加したパス定数およびログ出力先定数の変更

**変更前**

```java
public static final String PATH_EXEC_LOG = "/home/mini/wakasaba_orchestrator/log/log_%s.txt";
```

**変更後**

```java
public static final String PATH_WAKASABA_EXEC = "/home/mini/wakasaba_orchestrator/";
public static final String PATH_EXEC_LOG = PATH_WAKASABA_EXEC + "log/log_%s.txt";
public static final String PATH_PAPERMC_VERSION = PATH_WAKASABA_EXEC + "papermc_version.txt";
```

| 定数名                 | 用途                                                                           |
|------------------------|--------------------------------------------------------------------------------|
| `PATH_WAKASABA_EXEC`   | 管理端末上のwakasaba_orchestratorディレクトリ                                  |
| `PATH_EXEC_LOG`        | 既存のログ出力先を共通ディレクトリ定数との連結へ変更。展開後のパスは従来と同じ |
| `PATH_PAPERMC_VERSION` | 管理端末上のPaperMCバージョンファイルの出力先                                  |

---

## 変更による動作の差異

### ファイル出力比較

| 観点             | 変更前                       | 変更後                                                 |
|------------------|------------------------------|--------------------------------------------------------|
| バージョンの保持 | メソッド内のローカル変数のみ | ローカル変数に加え、管理端末上のファイルへ出力         |
| 出力先           | 専用ファイルなし             | `/home/mini/wakasaba_orchestrator/papermc_version.txt` |
| 出力タイミング   | 出力なし                     | `wgetPaperMc` 内のPaperMC・Pl3xMapダウンロード処理後   |
| 出力内容         | 出力なし                     | ダウンロード対象のPaperMCバージョン文字列              |
| 既存ファイル     | 専用ファイルなし             | 同一パスのファイルを上書き                             |
| 実行ログのパス   | 直接指定                     | 共通ディレクトリ定数から構成。出力先の変更なし         |

### 出力ファイル仕様

| 項目               | 内容                                                                           |
|--------------------|--------------------------------------------------------------------------------|
| 出力端末           | wakasaba_orchestrator実行端末（管理端末）                                      |
| ファイル名         | `papermc_version.txt`                                                          |
| 出力先ディレクトリ | `/home/mini/wakasaba_orchestrator/`                                            |
| 形式               | バージョン文字列のみのテキスト。ラベル・JSON・ダブルクオーテーションを付けない |
| 改行               | `print` を使用するため末尾改行なし                                             |
| 文字コード         | `PrintStream(String)` の既定文字コードを使用                                   |
| 更新方式           | ファイルがなければ新規作成、存在すれば内容を上書き                             |

バージョン `26.3` を取得した場合の内容は以下のとおりとなる。

```text
26.3
```

コードブロックは表示例であり、実ファイルの末尾には改行を付加しない。

---

## 変更後のダウンロード・ファイル出力フロー

```
SshCommand.wgetPaperMc(ci)
  1. PaperMCバージョン一覧JSONを取得
  2. PaperUrlGen.getPaperMcVersion(versionJson) → version
       └─ 変更管理No.307の正式版選択を使用
  3. version に対応するビルド情報JSONを取得し、ダウンロードURLを抽出
  4. SSH経由でPaperMCのwgetコマンドを実行
  5. Pl3xMapの情報JSONを取得し、ダウンロードURLを判定
       ├─ URLがnull: Pl3xMapのダウンロードをスキップ
       └─ URLあり: SSH経由でPl3xMapのwgetコマンドを実行
  6. 管理端末上でPATH_PAPERMC_VERSIONを指定してPrintStreamを生成
  7. version をprintで書き込み、close
  8. 呼び出し元へ復帰

WksWorkFlowの後続処理
  movePaperMc → shutdown → startPaperMC
```

バージョンファイルは `movePaperMc` によるSHA検証・本番配置より前に作成する。
ファイルの内容はダウンロード対象のバージョンを示し、本番配置・起動完了を判定した結果ではない。

---

## 影響範囲

| 対象                     | 影響                                                                             |
|--------------------------|----------------------------------------------------------------------------------|
| `SshCommand.wgetPaperMc` | メソッド末尾に管理端末上のバージョンファイル出力を追加                           |
| `WksConstants`           | 共通ディレクトリとバージョンファイルのパス定数を追加し、既存ログパスの定義を変更 |
| `PaperUrlGen`            | 308による変更なし。変更管理No.307の選択結果をファイル出力に使用                  |
| `WksWorkFlow`            | 変更なし。既存の `wgetPaperMc` 呼び出し内で追加処理を実行                        |
| `Main`                   | 変更なし。ファイルオープン時の例外は既存の例外捕捉・ログ記録で処理               |
| 管理端末のファイル       | `papermc_version.txt` を新規作成または上書き                                     |
| 実行インタフェース       | 変更なし。コマンドライン引数数および接続情報ファイル形式に変更なし               |
| 既存ドキュメント         | 要件定義・基本設計・詳細設計および補足資料へ反映済み                             |

---

## ドキュメント更新結果

| ドキュメント群                                                                       | 更新結果                                                                                       |
|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`       | `wgetPaperMc` の管理端末上のファイル出力と実行タイミングを追加済み                             |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md` | `PATH_WAKASABA_EXEC`・`PATH_PAPERMC_VERSION` の追加および `PATH_EXEC_LOG` の定義変更を反映済み |
| 各階層のシステム概要                                                                 | 出力ファイル一覧にバージョンファイルのパス・内容・上書き仕様を追加済み                         |
| 各階層のWksWorkFlow設計書                                                            | PaperMCダウンロード処理内で管理端末へファイル出力することを反映済み                            |
| 各階層の例外・エラーハンドリング資料                                                 | バージョンファイルのオープン失敗時の例外伝播・ログ記録を反映済み                               |

---

## 備考

- 管理端末には出力先ディレクトリが存在し、実行ユーザにファイル作成・上書き権限があることを前提とする。親ディレクトリの自動作成は行わない。
- ファイル出力方式は既存の `Main.outLog` と同じ `PrintStream` を使用する。十分なディスク容量がある運用を前提とし、書き込み・クローズ時のエラーフラグ確認は追加しない。
- ファイルオープンに失敗した場合の `FileNotFoundException` は `IOException` として上位へ伝播し、既存の `Main.main`
  でスタックトレースをログへ記録する。
- PaperMCサーバが初期疎通確認でスキップされた場合や、ファイル出力に到達する前に例外が発生した場合は、新規作成・上書きを行わない。既存ファイルがあれば以前の内容が残る。
- 正式版を選択する処理は、変更管理No.307で管理する。

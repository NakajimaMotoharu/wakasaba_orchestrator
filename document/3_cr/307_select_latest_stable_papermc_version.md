# 変更管理資料 No.307：PaperMC最新正式版の選択

## 基本情報

| 項目             | 内容                                                                         |
|------------------|------------------------------------------------------------------------------|
| 変更管理No       | 307                                                                          |
| 変更概要         | PaperMCバージョン一覧からpre・rc等を除外し、最新の正式版を取得するように変更 |
| ステータス       | 適用済み（ソース確認・PGレビュー・設計書反映済み）                           |
| 変更対象ファイル | `PaperUrlGen.java`、`WksConstants.java`                                      |
| 完全修飾クラス名 | `com.wks.papermc.PaperUrlGen`、`com.wks.parts.WksConstants`                  |

---

## 変更背景・目的

### 変更前の課題

変更前の `PaperUrlGen.getPaperMcVersion` は、PaperMC APIの `versions` オブジェクトから先頭のメジャーバージョン系列を取得し、
その配列の先頭要素を無条件に返却していた。

API応答にpre版やrc版が含まれ、それらが先頭に配置されている場合、正式版ではないバージョンがダウンロード対象となる課題があった。

### 変更目的

メジャーバージョン系列と各系列のバージョン候補を順に探索し、pre・rc等を除外して最新の正式版を取得する。
対象系列に正式版が存在しない場合は次の系列を探索し、すべての系列に正式版が存在しない場合は実行時例外を送出する。

---

## 変更内容

### 変更ファイル：`PaperUrlGen.java`

#### 変更箇所：`getPaperMcVersion` メソッド

**変更前**

```java
public class PaperUrlGen {
    public static String getPaperMcVersion(String json) {
        ObjectMapper om = new ObjectMapper();
        JsonNode src = om.readTree(json);
        JsonNode majorVersion = src.get(WksConstants.JSON_PAPERMC_GV);
        JsonNode fullVersion = majorVersion.get(List.copyOf(majorVersion.propertyNames()).getFirst());
        return fullVersion.get(0).toString().replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
    }
}
```

**変更後**

```java
public class PaperUrlGen {
    public static String getPaperMcVersion(String json) {
        ObjectMapper om = new ObjectMapper();
        JsonNode src = om.readTree(json);
        JsonNode majorVersion = src.get(WksConstants.JSON_PAPERMC_GV);
        String version = WksConstants.JSON_ES;
        boolean majorVersionFlag = false;
        List<String> majorVersionNames = List.copyOf(majorVersion.propertyNames());
        for (int i = 0; i < majorVersionNames.size() && !majorVersionFlag; i++) {
            String majorVersionString = majorVersionNames.get(i);
            JsonNode majorVersionJNode = majorVersion.get(majorVersionString);
            for (int j = 0; j < majorVersionJNode.size() && !majorVersionFlag; j++) {
                version = majorVersionJNode.get(j).toString();
                if (!version.contains(WksConstants.JSON_HYPHEN)) majorVersionFlag = true;
            }
        }

        if (!majorVersionFlag) throw new RuntimeException(WksConstants.OTHER_PAPERMC_EXCEPTION);

        return version.replace(WksConstants.JSON_REPLACE_DQ, WksConstants.JSON_REPLACE_ES);
    }
}
```

- メジャーバージョン系列と各系列内の候補を、API応答に含まれる順序で探索するように変更した。
- ハイフン（`-`）を含む候補を除外し、含まない最初の候補を正式版として返却する。
- 正式版を見つけた時点で内側・外側のループを終了する。
- 正式版が見つからなかった場合、空文字やpre・rc版を返さず `RuntimeException` を送出する。

---

### 変更ファイル：`WksConstants.java`

#### 追加定数

```java
public static final String JSON_ES = "";
public static final String JSON_HYPHEN = "-";
public static final String OTHER_PAPERMC_EXCEPTION = "Exception: No official version was found.";
```

| 定数名                    | 用途                                           |
|---------------------------|------------------------------------------------|
| `JSON_ES`                 | バージョン文字列の初期値に使用する空文字       |
| `JSON_HYPHEN`             | pre・rc等を除外するための判定文字              |
| `OTHER_PAPERMC_EXCEPTION` | 正式版が見つからない場合の実行時例外メッセージ |

---

## 変更による動作の差異

### バージョン選択比較

以下の候補は、API応答で左から右へ並んでいるものとする。

| API応答内の候補                                   | 変更前                 | 変更後             |
|---------------------------------------------------|------------------------|--------------------|
| 系列 `26.3`：`26.3`、`26.3-rc-3`                  | `26.3`                 | `26.3`             |
| 系列 `26.3`：`26.3-rc-3`、`26.3-pre-1`、`26.3`    | `26.3-rc-3`            | `26.3`             |
| 系列 `26.3`：`26.3-rc-3`、次の系列 `26.2`：`26.2` | `26.3-rc-3`            | `26.2`             |
| 系列 `26.1`：`26.1.2`、`26.1.1`                   | `26.1.2`               | `26.1.2`           |
| 全系列の候補がpre・rc版のみ                       | 先頭系列の先頭候補     | `RuntimeException` |
| `versions` が空                                   | 先頭系列の取得時に例外 | `RuntimeException` |

ユーザ提示のAPI応答例では、先頭系列 `26.3` の先頭候補 `26.3` が正式版のため、返却値は `26.3` となる。

### 呼び出し元への影響

`getPaperMcVersion` は `SshCommand.wgetPaperMc` と `SshCommand.movePaperMc` の両方から使用されるため、
ダウンロードURLの取得とSHA-256期待値の取得の双方に、今回の正式版選択が適用される。

正式版が見つからない場合の実行時例外は `WksWorkFlow.execScheduledJob` を経由して `Main.main` へ伝播する。
既存の `catch (Exception e)` でスタックトレースをグローバルログへ追記し、終了時刻およびログファイルを出力する。
例外発生後のワークフローの残りの処理は実行しない。

---

## 変更後のバージョン選択フロー

```
1. API応答JSONを解析し、versions ノードを取得
2. version を空文字、majorVersionFlag を false で初期化
3. メジャーバージョン系列名をAPI応答順にリスト化
4. 各系列について、配列の先頭から候補を順に取得
     ├─ ハイフンを含む: 次の候補へ進む
     └─ ハイフンを含まない: majorVersionFlag を true にして探索終了
5. 系列内に正式版がなければ次の系列を探索
6. 全系列を探索しても正式版がなければ RuntimeException を送出
7. 正式版文字列からダブルクオーテーションを除去して返却
```

---

## 影響範囲

| 対象                            | 影響                                                                    |
|---------------------------------|-------------------------------------------------------------------------|
| `PaperUrlGen.getPaperMcVersion` | 先頭要素の無条件取得から、ハイフンを含まない候補の探索へ変更            |
| `WksConstants`                  | 空文字・判定文字・例外メッセージの定数を追加                            |
| `SshCommand.wgetPaperMc`        | 呼び出し元として正式版選択が適用される。307によるメソッド本文の変更なし |
| `SshCommand.movePaperMc`        | SHA-256期待値の取得対象にも正式版選択が適用される                       |
| `Main`                          | 変更なし。新規の実行時例外も既存の例外捕捉・ログ記録で処理              |
| 実行インタフェース              | 変更なし。コマンドライン引数数および接続情報ファイル形式に変更なし      |
| 既存ドキュメント                | 要件定義・基本設計・詳細設計および補足資料へ反映済み                    |

---

## ドキュメント更新結果

| ドキュメント群                                                                                   | 更新結果                                                            |
|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| `0_rd/004_PAPER_URL_GEN.md`、`1_ui/104_PAPER_URL_GEN.md`、`2_ss/204_PaperUrlGen.md`              | 正式版の探索・除外条件・未検出時の実行時例外を反映済み              |
| `0_rd/003_SSH_COMMAND.md`、`1_ui/103_SSH_COMMAND.md`、`2_ss/203_SshCommand.md`                   | ダウンロードおよびSHA-256期待値取得で正式版を使用することを反映済み |
| `0_rd/009_WKS_CONSTANTS.md`、`1_ui/109_WKS_CONSTANTS.md`、`2_ss/209_WksConstants.md`             | `JSON_ES`・`JSON_HYPHEN`・`OTHER_PAPERMC_EXCEPTION` を追加済み      |
| `0_rd/091_EXTERNAL_API_SPEC.md`、`1_ui/191_EXTERNAL_API_SPEC.md`、`2_ss/291_PAPERMC_API_NOTE.md` | バージョン一覧の探索手順とpre・rc等の除外を反映済み                 |
| 各階層のシステム概要                                                                             | PaperMC最新版取得に正式版の選択条件を反映済み                       |
| 各階層の例外・エラーハンドリング資料                                                             | 正式版未検出時の実行時例外と既存の伝播・ログ記録経路を反映済み      |

---

## 備考

- 正式版の判定は、APIのバージョン文字列にハイフンが含まれないことを条件とする。数字・ピリオドの正規表現検証は追加しない。
- 最新順の判定はAPI応答の並び順を前提とし、プログラム内で数値比較や並べ替えは行わない。
- 全系列に正式版が存在しない状況は通常運用では想定しないが、発生時は `RuntimeException` として扱う。
- バージョンファイルの作成は、変更管理No.308で管理する。

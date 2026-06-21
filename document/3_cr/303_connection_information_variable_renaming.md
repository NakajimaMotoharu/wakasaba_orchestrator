# 変更管理資料 No.303：サーバ接続情報変数名の採番修正

## 基本情報

| 項目       | 内容                                        |
|----------|-------------------------------------------|
| 変更管理No   | 303                                       |
| 変更概要     | サーバ0～2の接続情報変数名を、サーバ番号と一致する `ci0` 起点の連番へ修正 |
| ステータス    | 適用済み（ソース確認・ビルド確認済み）                       |
| 変更対象ファイル | `WksWorkFlow.java`                        |
| 完全修飾クラス名 | `com.wks.workflow.WksWorkFlow`            |

---

## 変更背景・目的

### 変更前の課題

変更前の `WksWorkFlow.execScheduledJob` では、コマンドライン引数の配列インデックスおよびサーバ番号と、
接続情報を保持するローカル変数名の採番が1つずれていた。

| 対象   | 接続情報ファイル     | 変更前の変数名 |
|------|--------------|---------|
| サーバ0 | `servers[0]` | `ci1`   |
| サーバ1 | `servers[1]` | `ci2`   |
| サーバ2 | `servers[2]` | `ci3`   |

動作上は各変数に正しい接続情報が格納されていたため問題はなかったが、サーバ番号と変数名が一致しておらず、
処理対象の判別や保守時の確認を難しくする要因となっていた。

### 変更目的

接続情報変数名をサーバ番号および `servers` 配列のインデックスと一致させ、
コードの可読性・保守性を向上させる。

---

## 変更内容

### 変更ファイル：`WksWorkFlow.java`

#### 変更箇所：`execScheduledJob` メソッド内、サーバ0～2の処理ブロック

**変更前**

```java
public class WksWorkFlow {
    public static void execScheduledJob(String[] servers) {
        // サーバ0
        ConnectionInformation ci1 = ConnectionInformation.getCiFromFile(servers[0]);
        SshCommand.update(ci1);

        // サーバ1
        ConnectionInformation ci2 = ConnectionInformation.getCiFromFile(servers[1]);
        SshCommand.stopPaperMC(ci2);

        // サーバ2
        ConnectionInformation ci3 = ConnectionInformation.getCiFromFile(servers[2]);
        SshCommand.update(ci3);
    }
}
```

**変更後**

```java
public class WksWorkFlow {
    public static void execScheduledJob(String[] servers) {
        // サーバ0
        ConnectionInformation ci0 = ConnectionInformation.getCiFromFile(servers[0]);
        SshCommand.update(ci0);

        // サーバ1
        ConnectionInformation ci1 = ConnectionInformation.getCiFromFile(servers[1]);
        SshCommand.stopPaperMC(ci1);

        // サーバ2
        ConnectionInformation ci2 = ConnectionInformation.getCiFromFile(servers[2]);
        SshCommand.update(ci2);
    }
}
```

各変数の宣言箇所だけでなく、区切りログへの出力および各 `SshCommand` 呼び出しに渡す変数も同じ名称へ変更した。

---

## 変更による動作の差異

### 変数名比較

| 対象   | 配列インデックス     | 変更前   | 変更後   |
|------|--------------|-------|-------|
| サーバ0 | `servers[0]` | `ci1` | `ci0` |
| サーバ1 | `servers[1]` | `ci2` | `ci1` |
| サーバ2 | `servers[2]` | `ci3` | `ci2` |

### 実行時動作

本変更はローカル変数名のみを対象とするリファクタリングであり、以下の動作に変更はない。

- 接続情報ファイルの読み込み元
- 各サーバの処理順序
- 各サーバに対して実行するSSHコマンド
- ログへ記録される接続情報およびコマンド実行結果
- 例外の種類および伝播経路

---

## 変更後の変数対応

```
servers[0] → ci0 → サーバ0
servers[1] → ci1 → サーバ1（PaperMCサーバ）
servers[2] → ci2 → サーバ2
```

変更管理No.304によって追加されたサーバ3についても、同じ採番規則で以下の対応となる。

```
servers[3] → ci3 → サーバ3（Schubertサーバ）
```

---

## 影響範囲

| 対象                             | 影響                            |
|--------------------------------|-------------------------------|
| `WksWorkFlow.execScheduledJob` | サーバ0～2の接続情報変数名と、その参照箇所を一括変更   |
| `Main`                         | 変更なし                          |
| `SshCommand`                   | 変更なし                          |
| `ConnectionInformation`        | 変更なし                          |
| 実行結果・外部インタフェース                 | 変更なし                          |
| 既存ドキュメント                       | 変数名を記載している要件定義・基本設計・詳細設計へ反映済み |

---

## ドキュメント更新結果

| ドキュメント群                                                                           | 更新結果                                     |
|-----------------------------------------------------------------------------------|------------------------------------------|
| `0_rd/002_WKS_WORK_FLOW.md`、`1_ui/102_WKS_WORK_FLOW.md`、`2_ss/202_WksWorkFlow.md` | サーバ0～2の処理フローに記載された接続情報変数名を `ci0` 起点へ更新済み |
| 各階層のシステム概要                                                                        | 接続情報変数名を記載している箇所を同じ採番規則へ更新済み             |
| `3_cr/302_wait_after_papermc_stop.md`                                             | サーバ1の変更前後コードおよび処理フロー内の変数名を `ci1` へ更新済み   |

---

## 備考

- 本変更は識別子の変更のみであり、機能追加・処理順変更・外部仕様変更を伴わない。
- 変数名は `servers` 配列のインデックスおよびコメント上のサーバ番号と一致させる。
- 変更管理No.304のサーバ追加後も、`ci0`～`ci3` の連番規則を維持する。

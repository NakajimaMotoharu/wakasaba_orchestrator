# wakasaba_orchestrator 基本設計書 補足：外部API仕様

## 概要

本アプリケーションが使用する外部 REST API の設計方針と呼び出し仕様をまとめる。  
PaperMC 公式 API・Modrinth API の 2 種類を使用する。

---

## 1. PaperMC API

### 1-1. バージョン一覧取得

| 項目      | 内容                                                |
|---------|---------------------------------------------------|
| URL 定数  | `WksConstants.URL_PAPERMC_VERSION`                |
| エンドポイント | `GET https://fill.papermc.io/v3/projects/paper`   |
| 利用メソッド  | `SshCommand.wgetPaperMc`、`SshCommand.movePaperMc` |
| パーサ     | `PaperUrlGen.getPaperMcVersion`                   |

#### レスポンス JSON 構造（簡略）

```json
{
  "versions": {
    "1.21": [
      "1.21.4",
      "1.21.3",
      "1.21.1"
    ]
  }
}
```

#### 取得ロジック

- `versions` オブジェクトのプロパティ名一覧を取得し、**先頭のキー**（最新メジャーバージョン）を選択する。
- そのキーに対応する配列の**インデックス 0** の値（最新マイナーバージョン）を最終的なバージョン文字列とする。

---

### 1-2. ビルド情報取得（ダウンロード URL・SHA-256）

| 項目      | 内容                                                                        |
|---------|---------------------------------------------------------------------------|
| URL 定数  | `WksConstants.URL_PAPERMC_DL_URL`                                         |
| エンドポイント | `GET https://fill.papermc.io/v3/projects/paper/versions/{version}/builds` |
| 利用メソッド  | `SshCommand.wgetPaperMc`（URL 取得）、`SshCommand.movePaperMc`（SHA-256 取得）     |
| パーサ     | `PaperUrlGen.getPaperMcUrl`、`PaperUrlGen.getPaperMcSha256`                |

#### レスポンス JSON 構造（簡略）

```json
[
  {
    "downloads": {
      "server:default": {
        "url": "https://api.papermc.io/.../paper-1.21.4-xxx.jar",
        "checksums": {
          "sha256": "0123abc..."
        }
      }
    }
  }
]
```

#### 取得ロジック

- 配列の**インデックス 0**（最新ビルド）を参照する。
- `downloads` → `server:default` → `url` でダウンロード URL を取得する。
- `downloads` → `server:default` → `checksums` → `sha256` で SHA-256 値を取得する。

---

## 2. Modrinth API（Pl3xMap）

| 項目      | 内容                                                                    |
|---------|-----------------------------------------------------------------------|
| URL 定数  | `WksConstants.URL_PL3XMAP_DL_URL`                                     |
| エンドポイント | `GET https://api.modrinth.com/v2/project/pl3xmap/version`             |
| 利用メソッド  | `SshCommand.wgetPaperMc`（URL 取得）、`SshCommand.movePaperMc`（SHA-512 取得） |
| パーサ     | `PaperUrlGen.getPl3xMapUrl`、`PaperUrlGen.getPl3xMapSha512`            |

#### レスポンス JSON 構造（簡略）

```json
[
  {
    "game_versions": [
      "1.21.4"
    ],
    "files": [
      {
        "url": "https://cdn.modrinth.com/.../Pl3xMap-xxx.jar",
        "hashes": {
          "sha512": "0123abc..."
        }
      }
    ]
  }
]
```

#### 取得ロジック

- 配列の**インデックス 0**（最新バージョン）を参照する。
- `game_versions[0]` の値が引数 `version`（PaperMC の最新バージョン）と**一致しない場合は `null` を返す**（バージョンミスマッチ）。
- 一致する場合、`files[0]` → `url` でダウンロード URL、`files[0]` → `hashes` → `sha512` で SHA-512 値を取得する。

---

## 3. バージョン整合性チェックの設計

| 対象      | チェック内容                      | 不一致時の挙動                                    |
|---------|-----------------------------|--------------------------------------------| 
| PaperMC | SHA-256 が一致するか              | ファイル移動および旧本番 JAR の削除をスキップ（旧本番 JAR は残存する）   |
| Pl3xMap | PaperMC バージョンと対応バージョンが一致するか | ダウンロード・移動をスキップ                             |
| Pl3xMap | SHA-512 が一致するか              | ファイル移動をスキップ（旧ファイルはバージョン一致・不一致にかかわらず常に削除済み） |

**設計上の注意点：**

- Pl3xMap の旧ファイル（`CMD_PL3XMAP_RM`）は、バージョン一致・SHA 検証の結果にかかわらず**常に削除**する。これにより、古いバージョンの
  Pl3xMap がサーバに残留しないことを保証する。
- PaperMC の旧本番 JAR（`CMD_PAPERMC_RM`）は SHA-256 検証が一致した場合にのみ削除する。SHA-256 不一致の場合、新 JAR
  のファイル移動とともに旧本番 JAR の削除もスキップされるため、旧本番 JAR はサーバに残存する。

---

## 4. HTTP リクエスト共通仕様

| 項目         | 値                           |
|------------|-----------------------------| 
| メソッド       | GET                         |
| User-Agent | `wakasaba_orchestrator/1.0` |
| HTTP バージョン | HTTP/1.1                    |
| 接続タイムアウト   | 60 秒                        |
| 読み取りタイムアウト | 未設定（無制限）                    |
| リダイレクト     | 通常リダイレクトに追従する               |

HTTP クライアントの実装詳細については [108_CURL.md](./108_CURL.md) を参照。  
JSON レスポンスの解析ロジックの詳細については [104_PAPER_URL_GEN.md](./104_PAPER_URL_GEN.md) を参照。

---

## 設計上の注意点

- HTTP レスポンスのステータスコード検証は `Curl.exec` では行わない。API が 4xx/5xx を返した場合は後続の `PaperUrlGen` で
  JSON パースエラーが発生する。
- `wgetPaperMc` と `movePaperMc` の両方で独立して API を呼び出してバージョン情報を取得するため、2 つのメソッドの呼び出し間に
  API レスポンスが変わるケースへの対処はない（実運用上の問題は想定されない）。

# wakasaba_orchestrator 要件定義書 補足：外部API仕様

## 概要

本アプリケーションが使用する外部REST APIの仕様をまとめる。  
PaperMC公式API・Modrinth APIの2種類を使用する。

---

## 1. PaperMC API

### 1-1. バージョン一覧取得

| 項目      | 内容                                                |
|---------|---------------------------------------------------|
| URL定数   | `WksConstants.URL_PAPERMC_VERSION`                |
| エンドポイント | `GET https://fill.papermc.io/v3/projects/paper`   |
| 利用クラス   | `SshCommand.wgetPaperMc`、`SshCommand.movePaperMc` |
| パーサ     | `PaperUrlGen.getPaperMcVersion`                   |

#### レスポンスJSON構造（簡略）

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

#### 取得ロジック要件

- `versions` オブジェクトのプロパティ名一覧を取得し、**先頭のキー**（最新メジャーバージョン）を選択すること。
- そのキーに対応する配列の**インデックス0**の値（最新マイナーバージョン）を最終的なバージョン文字列とすること。

---

### 1-2. ビルド情報取得（ダウンロードURL・SHA256）

| 項目      | 内容                                                                        |
|---------|---------------------------------------------------------------------------|
| URL定数   | `WksConstants.URL_PAPERMC_DL_URL`                                         |
| エンドポイント | `GET https://fill.papermc.io/v3/projects/paper/versions/{version}/builds` |
| 利用クラス   | `SshCommand.wgetPaperMc`（URL取得）、`SshCommand.movePaperMc`（SHA256取得）        |
| パーサ     | `PaperUrlGen.getPaperMcUrl`、`PaperUrlGen.getPaperMcSha256`                |

#### レスポンスJSON構造（簡略）

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

#### 取得ロジック要件

- 配列の**インデックス0**（最新ビルド）を参照すること。
- `downloads` → `server:default` → `url` でダウンロードURLを取得すること。
- `downloads` → `server:default` → `checksums` → `sha256` でSHA256値を取得すること。

---

## 2. Modrinth API（Pl3xMap）

| 項目      | 内容                                                                 |
|---------|--------------------------------------------------------------------|
| URL定数   | `WksConstants.URL_PL3XMAP_DL_URL`                                  |
| エンドポイント | `GET https://api.modrinth.com/v2/project/pl3xmap/version`          |
| 利用クラス   | `SshCommand.wgetPaperMc`（URL取得）、`SshCommand.movePaperMc`（SHA512取得） |
| パーサ     | `PaperUrlGen.getPl3xMapUrl`、`PaperUrlGen.getPl3xMapSha512`         |

#### レスポンスJSON構造（簡略）

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

#### 取得ロジック要件

- 配列の**インデックス0**（最新バージョン）を参照すること。
- `game_versions[0]` の値が引数 `version`（PaperMCの最新バージョン）と**一致しない場合は `null` を返す**（バージョンミスマッチ）こと。
- 一致する場合、`files[0]` → `url` でダウンロードURL、`files[0]` → `hashes` → `sha512` でSHA512値を取得すること。

---

## 3. バージョン整合性チェックの要件

| 対象      | チェック内容                     | 不一致時の挙動                                    |
|---------|----------------------------|--------------------------------------------|
| PaperMC | SHA256が一致するか               | ファイル移動および旧本番JARの削除をスキップ（旧本番JARは残存する）       |
| Pl3xMap | PaperMCバージョンと対応バージョンが一致するか | ダウンロード・移動をスキップ                             |
| Pl3xMap | SHA512が一致するか               | ファイル移動をスキップ（旧ファイルはバージョン一致・不一致にかかわらず常に削除済み） |

> **要件：** Pl3xMapの旧ファイル（`CMD_PL3XMAP_RM`）は、バージョン一致・SHA検証の結果にかかわらず**常に削除**すること。  
> これにより、古いバージョンのPl3xMapがサーバに残留しないことを保証する。
>
> **注意：** PaperMCの旧本番JAR（`CMD_PAPERMC_RM`
> ）はSHA256検証が一致した場合にのみ削除される。SHA256不一致の場合、新JARのファイル移動とともに旧本番JARの削除もスキップされるため、旧本番JARはサーバに残存する。

---

## 4. HTTPリクエスト共通要件

| 項目         | 要件値                         |
|------------|-----------------------------|
| メソッド       | GET                         |
| User-Agent | `wakasaba_orchestrator/1.0` |
| HTTPバージョン  | HTTP/1.1                    |
| 接続タイムアウト   | 60秒                         |
| 読み取りタイムアウト | 未設定（無制限）                    |
| リダイレクト     | 通常リダイレクトに追従すること             |

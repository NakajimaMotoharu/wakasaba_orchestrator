# wakasaba_orchestrator 基本設計書：Main

## 基本情報

| 項目             | 内容                                                                         |
|------------------|------------------------------------------------------------------------------|
| 完全修飾クラス名 | `com.wks.main.Main`                                                          |
| ファイル名       | `Main.java`                                                                  |
| 種別             | class（public）                                                              |
| 責務             | 引数検証・ワークフロー起動・AplLogによる開始/例外/終了ログ受付と出力終了待ち |
| 主な依存クラス   | `WksWorkFlow`、`WksConstants`、`AplLog`                                      |

---

## フィールド一覧

なし。ログリストはAplLog内部で保持する。

---

## メソッド一覧

| メソッド名            | 戻り値型 | 修飾子          | 処理                                                       |
|-----------------------|----------|-----------------|------------------------------------------------------------|
| `main(String[] args)` | `void`   | `public static` | 6引数検証・開始通知・ワークフロー・ERR記録・終了通知・join |

---

## 処理フロー

```
1. Mainで6引数を検証
2. AplLog.getInstance(): 初回だけ生成・ファイルオープン・自動start
3. MainがlocalhostのSYS開始ログを追加
4. ワークフロー実行。各部品が接続先とレベルを指定して追加
5. Mainがワークフロー例外を捕捉した場合は例外全体をERRとして追加
6. 全ログ発行処理の完了後、MainがlocalhostのSYS終了ログを追加
7. endOfLogOutput(): 受付終了を通知（この呼び出しだけでは完了待ちしない）
8. AplLogは受付済みログを排出し、PrintStreamをcloseして終了
9. Mainはjoin()で出力スレッドの終了を待って復帰
```

引数不正時は `System.out.println(OTHER_ARGS_MSG)` のみで終了する。
開始メッセージは `BATCH MAINTENANCE START`、終了メッセージは `BATCH MAINTENANCE END`。
Mainの公開ログフィールド・一括ファイル出力・日時生成は廃止し、AplLogへ責務を移す。

---

## 例外

| 箇所            | 例外                                 | 扱い                                                       |
|-----------------|--------------------------------------|------------------------------------------------------------|
| 初回getInstance | FileNotFoundException（IOException） | 開始ログ追加前の失敗。ワークフロー用catchの外で上位へ伝播  |
| ワークフロー    | Exception                            | AplLog.outExceptionLog(localhost, e)へ渡し、終了処理へ進む |
| AplLog.join     | InterruptedException                 | mainのthrowsで上位へ伝播                                   |

Errorは捕捉対象外であり、正常な終了処理・ログ完了を保証しない。

---

## 設計上の注意点

- ログのファイル名はAplLog初回生成時刻で決まり、バッチ終了時刻ではない。
- 全ログ発行スレッドの完了後に最終ログを追加する通常経路を前提とする。
- endOfLogOutputは非同期の終了通知であり、ファイルクローズの完了確認にはjoinが必要。
- ファイル出力先の存在と権限を事前に用意する。詳細は同階層のAplLog設計書を参照。

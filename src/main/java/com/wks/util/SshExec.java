package com.wks.util;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SshExec {

	/** サーバ情報 */
	private final ConnectionInformation ci;
	/** 実行コマンド */
	private final String cmd;
	/** 実行フラグ */
	private boolean executed;

	/** コンストラクタ */
	public SshExec(ConnectionInformation ci, String cmd){
		this.ci = ci;
		this.cmd = cmd;
		executed = false;
	}

	/** 接続対象サーバと疎通可能か */
	public boolean isAlive() throws JSchException {
		// セッションインスタンス取得
		Session session = getSessionInstance();

		try {
			// 接続テスト
			session.connect();
		} catch (JSchException e){
			// 接続失敗した場合、疎通不可と判断
			return false;
		}

		// セッションを閉じる
		session.disconnect();

		// 接続成功なので、疎通可能と判断
		return true;
	}

	/** コマンド実行 */
	public String[] execute() throws JSchException, IOException, InterruptedException {
		// すでに実行されていたら何もせずに呼び出し元に復帰
		if (executed){
			return null;
		}
		// 実行フラグを立てる
		executed = true;

		// セッションインスタンスを取得
		Session session = getSessionInstance();

		// セッションに接続
		session.connect();

		// 1コマンド実行モードでチャンネル生成
		ChannelExec channelExec = (ChannelExec) session.openChannel("exec");

		// 実行コマンドを設定
		channelExec.setCommand(cmd);

		// 出力待ち受け用streamを作成
		InputStream inputStream = channelExec.getInputStream();

		// チャンネル接続
		channelExec.connect();

		// 出力読み取り用BufferedReaderを作成
		BufferedReader bufferedReader =
				new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

		// 応答格納用文字列リストを作成
		ArrayList<String> responseList = new ArrayList<>();

		// 応答をすべて読み取るまでループ
		while (true){
			// 読める部分すべて読み取るまでループ
			while (bufferedReader.ready()){
				// 応答格納用文字列リストに格納
				responseList.add(bufferedReader.readLine());
			}

			// チェンネルが閉じた場合ループを抜ける
			if (channelExec.isClosed()){
				break;
			}

			// 次の文字列が出力されるのを待つための一定時間待機
			Thread.sleep(1000);
		}

		// チャンネル・セッションを切断
		channelExec.disconnect();
		session.disconnect();

		// 応答格納用文字列リストを配列に変換
		String[] response = new String[responseList.size()];
		for (int i = 0; i < responseList.size(); i++){
			response[i] = responseList.get(i);
		}

		// 呼び出し元に復帰する
		return response;
	}

	/** セッションインスタンス作成 */
	private Session getSessionInstance() throws JSchException {
		// JSchインスタンスを作成
		JSch jSch = new JSch();

		// 認証用の秘密鍵を設定
		jSch.addIdentity(ci.key());

		// SSHセッション作成
		Session session = jSch.getSession(
				ci.user(), // ユーザ名
				ci.host(), // ホスト名
				ci.port() // ポート番号
		);

		// known_hostsチェック無効化
		session.setConfig("StrictHostKeyChecking", "no");

		// セッション返却
		return session;
	}
}

package net.frontlinesms.install4j.custom;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;

import static net.frontlinesms.install4j.custom.Util.*;

public class Testsend {
	public boolean submitData(Map<String, String> data) {
		String uuid = generateUUID();
		data.put("UUID", uuid);
		try {
			HttpURLConnection conn = initConnection();

			// TODO handle streams safely
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			writeRequestBody(out, data);

			readResponse(conn);
			createRegistrationPropertiesFile(uuid, true);
			return true;
		} catch(Exception e) {
			createRegistrationPropertiesFile(uuid, false);
			return false;
		}
	}

	private HttpURLConnection initConnection() throws IOException {
		URL siteUrl = new URL(Util.PROCESSING_URL);
		HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		return conn;
	}

	// TODO unit test this method
	private void writeRequestBody(DataOutputStream out, Map<String, String> data) throws IOException {
		String content = "";
		for(Entry<String, String> e: data.entrySet()) {
			String key = e.getKey();
			if(content.length() > 0) {
				content += "&";
			}
			content += e.getKey() + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
		}
		System.out.println(content);
		// TODO close streams safely
		out.writeBytes(content);
		// TODO do not flush if it's not needed
		out.flush();
		out.close();
	}

	private void readResponse(HttpURLConnection conn) throws IOException {
		// TODO handle streams safely
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while((line=in.readLine()) != null) {
			System.out.println(line);
		}
		in.close();
	}
}


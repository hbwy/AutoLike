<<<<<<< HEAD
package com.dealmoon.autolike.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class MyUtils {

	private static Map<String, String> tokens = new HashMap<String, String>(); //用户token
	private static Map<String, String> config = new HashMap<String, String>(); //配置文件
	private static Map<String, List<String>> appRequestData = new HashMap<String, List<String>>(); //app接口请求数据

	public static Map<String, String> getTokens() {
		return tokens;
	}

	public static Map<String, String> getConfig() {
		return config;
	}

	public static Map<String, List<String>> getAppRequestData() {
		return appRequestData;
	}

	static {
		//读取token
		tokens = proReader("pro/token.properties");
		//读取请求的url
		config = proReader("pro/config.properties");
	}

	//读取app接口请求数据配置文件
	static {
		String unchanged_json_path = "pro/app_unchanged.properties";
		String changed_json_path = "pro/app_changed.properties";

		String[] strs = appUnchangedJsonReader(unchanged_json_path);
		List<String> command_infos = changedJsonReader(changed_json_path);

		List<String> reqjsons;
		for (int i = 0; i < command_infos.size(); i++) {
			String changed_json = command_infos.get(i);
			String reqjson = changed_json + strs[1];
			JSONObject jo = JSONObject.fromObject("{" + reqjson + "}");
			String interName = jo.getString("command").replaceAll("/", "");

			if (appRequestData.get(interName) == null) {
				reqjsons = new ArrayList<String>();
				reqjsons.add(reqjson);
			} else {
				reqjsons = (List<String>) appRequestData.get(interName);
				reqjsons.add(reqjson);
			}

			appRequestData.put(interName, reqjsons);
		}
	}

	//读取app接口请求中不变的json
	public static String[] appUnchangedJsonReader(String app_unchanged_path) {

		String unchanged_json = stringReader(app_unchanged_path);
		String model = matchString(unchanged_json, "(\"model\":\\s*\"(.*)\",\\s*\"smsCenter)", 2);
		String activeTime = matchString(unchanged_json, "(\"activeTime\":\\s*\"(.*)\",\\s*\"resourceUpdateTime)", 2);

		String _model = removeBlank(model);
		String _activeTime = removeBlank(activeTime);
		String _unchanged_json = removeBlank(unchanged_json);

		String finalUnchangedJsonData = _unchanged_json.replace(_model, model).replace(_activeTime, activeTime);

		String token = matchString(finalUnchangedJsonData, "(\"token\":\"\\S*\",)\"protocol\"", 1);

		if (token != null && !token.equals("")) {
			finalUnchangedJsonData = finalUnchangedJsonData.replace(token, "");
		}
		String[] strings = { token, finalUnchangedJsonData };
		return strings;
	}

	//读取app接口请求中变的json
	public static List<String> changedJsonReader(String changed_json_path) {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, List<Map<String, Object>>> map = new HashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> list;
		List<String> command_info = new ArrayList<String>();

		try {
			map = mapper.readValue(new File(changed_json_path),
					new TypeReference<Map<String, List<Map<String, Object>>>>() {
					});
			Set<?> entries = map.entrySet();
			for (Iterator<?> iterator = entries.iterator(); iterator.hasNext();) {
				Map.Entry<String, List<Map<String, Object>>> entry = (Map.Entry<String, List<Map<String, Object>>>) iterator
						.next();

				String command = entry.getKey();
				list = entry.getValue();
				for (int i = 0; i < list.size(); i++) {
					Map<String, Object> maps = list.get(i);
					String commandInfo = mapper.writeValueAsString(maps);
					command_info.add("\"command\":\"" + command + "\",\"commandInfo\":" + commandInfo + ",");
				}

			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return command_info;
	}

	//发送请求的方法
	public static String sendPost(String jsonData) {
		HttpURLConnection conn = null;
		StringBuffer sbf = new StringBuffer();
		try {
			URL realUrl = new URL(getConfig().get("url"));
			conn = (HttpURLConnection) realUrl.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);

			String DMAuthorization = MD5("deal" + jsonData + "moon");
			conn.setRequestProperty("DMAuthorization", DMAuthorization);
			conn.setRequestProperty("Accept-Charset", "UTF-8");
			conn.connect();

			PrintWriter out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "utf-8"));
			out.println(jsonData);
			out.flush();
			out.close();

			if (conn.getResponseCode() == 200) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String lines;
				while ((lines = reader.readLine()) != null) {
					lines = new String(lines.getBytes(), "utf-8");
					sbf.append(lines);
				}
				reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
				conn = null;
			}
		}
		return sbf.toString();
	}

	//读取properties配置文件
	public static Map<String, String> proReader(String path) {
		Reader reader;
		Map<String, String> map = new HashMap<String, String>();
		try {
			reader = new FileReader(path);
			Properties prop = new Properties();
			Set keySet = null;
			prop.load(reader);
			keySet = prop.keySet();
			Iterator it = keySet.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				map.put(key, (String) prop.get(key));
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static String MD5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1, messageDigest);
			String hashtext = number.toString(16);
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}
			return hashtext;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	//以流的形式读取文件
	public static String stringReader(String path) {
		File file = new File(path);
		StringBuffer jsonData = new StringBuffer();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String lines;

			while ((lines = br.readLine()) != null) {
				lines = new String(lines.getBytes(), "UTF-8");
				jsonData.append(lines);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonData.toString();
	}

	//用正则表达式匹配字符串
	public static String matchString(String strSource, String pattern, int groupId) {
		Pattern _pattern = Pattern.compile(pattern);
		Matcher matcher = _pattern.matcher(strSource);
		String strResult = null;
		if (matcher.find()) {
			strResult = matcher.group(groupId);
		}
		return strResult;
	}

	//去掉字符串中的空格
	public static String removeBlank(String strSource) {
		return (strSource.replaceAll("\\s*", ""));
	}

	//判断post发布时间是否大于10秒
	public static boolean judgeTime(long publishedTime) {
		return System.currentTimeMillis() - publishedTime > 10000;
	}

	//获取满足点赞条件的post列表
	public static List<String> getPostList(String type, int pageNum, int pageSize) {
		Map<String, List<String>> reqData = getAppRequestData();
		List<String> reqJsons = (List<String>) reqData.get("postgetlist");
		List<String> postid_author = new ArrayList<String>();

		String reqJson = "{" + reqJsons.get(0) + "}";
		String response = MyUtils.sendPost(reqJson);

		JSONArray array = JSONObject.fromObject(response).getJSONObject("responseData").getJSONArray("posts");
		for (Iterator<?> iterator = array.iterator(); iterator.hasNext();) {

			JSONObject obj = (JSONObject) iterator.next();
			JSONObject author = obj.getJSONObject("author");

			if (obj.getInt("likeNum") == 0 && judgeTime(obj.getLong("publishedTime")) && author.getInt("postNum") <= 5) {
				postid_author.add("" + obj.getInt("id") + "_" + author.getInt("id"));
			}
		}
		return postid_author;
	}

	//随机获取1-3个用户token
	public static List<String> getRandomToken(String authorid) {

		List<String> rtokens = new ArrayList<String>();
		Random random = new Random();

		Map<String, String> user_token = getTokens();
		user_token.remove(authorid);

		String[] tokens = user_token.values().toArray(new String[0]);

		int count = random.nextInt(3) + 1;
		while (rtokens.size() < count) {
			String token = tokens[new Random().nextInt(tokens.length - 1)];
			//集合中不包含该token才添加
			if (!rtokens.contains(token)) {
				rtokens.add("\"token\":" + "\"" + token + "\",");
			}
		}
		return rtokens;
	}

	//替换commandInfo中的id
	public static String replaceIdinBackCommand(String reqJson, String newId) {
		int oldId = JSONObject.fromObject(reqJson).getJSONObject("commandInfo").getInt("id");
		return reqJson.replace("\"id\":" + oldId, "\"id\":" + newId);
	}
}
=======
package com.dealmoon.autolike.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class MyUtils {

	private static Map<String, String> tokens = new HashMap<String, String>(); //�û�id_token
	private static Map<String, String> config = new HashMap<String, String>(); //�����ļ�
	private static Map<String, List<String>> appRequestData = new HashMap<String, List<String>>(); //app����������

	public static Map<String, String> getTokens() {
		return tokens;
	}

	public static Map<String, String> getConfig() {
		return config;
	}

	public static Map<String, List<String>> getAppRequestData() {
		return appRequestData;
	}

	static {
		//��ȡ�û�id �� token
		tokens = proReader("pro/token.properties");
		//��ȡ�����ļ�
		config = proReader("pro/config.properties");
	}

	//��ȡapp����������
	static {
		// �ļ��������·���µ�,Ĭ�ϵ�Ŀ¼Ϊ���̸�Ŀ¼,pro�ļ���Ϊ�����ļ��洢���ļ���
		String unchanged_json_path = "pro/app_unchanged.properties";
		String changed_json_path = "pro/app_changed.properties";

		String[] strs = appUnchangedJsonReader(unchanged_json_path);
		List<String> command_infos = changedJsonReader(changed_json_path);

		List<String> reqjsons;
		for (int i = 0; i < command_infos.size(); i++) {
			String changed_json = command_infos.get(i);
			String reqjson = changed_json + strs[1];
			JSONObject jo = JSONObject.fromObject("{" + reqjson + "}");
			String interName = jo.getString("command").replaceAll("/", "");

			if (appRequestData.get(interName) == null) {
				reqjsons = new ArrayList<String>();
				reqjsons.add(reqjson);
			} else {
				reqjsons = (List<String>) appRequestData.get(interName);
				reqjsons.add(reqjson);
			}

			appRequestData.put(interName, reqjsons);
		}
	}

	/**
	 * ��ȡ�����ļ��е�unchangedjson����
	 * 
	 * @param app_unchanged_path unchangedjson�ļ�·��
	 * @return ȥ���հ��ַ���unchangedjson�ַ���
	 */
	public static String[] appUnchangedJsonReader(String app_unchanged_path) {

		String unchanged_json = stringReader(app_unchanged_path);
		// ��������ʽ�����ַ���,model��activeTime�����пո�
		String model = matchString(unchanged_json, "(\"model\":\\s*\"(.*)\",\\s*\"smsCenter)", 2);
		String activeTime = matchString(unchanged_json, "(\"activeTime\":\\s*\"(.*)\",\\s*\"resourceUpdateTime)", 2);

		String _model = removeBlank(model);
		String _activeTime = removeBlank(activeTime);
		String _unchanged_json = removeBlank(unchanged_json);

		String finalUnchangedJsonData = _unchanged_json.replace(_model, model).replace(_activeTime, activeTime);

		String token = matchString(finalUnchangedJsonData, "(\"token\":\"\\S*\",)\"protocol\"", 1);

		if (token != null && !token.equals("")) {
			finalUnchangedJsonData = finalUnchangedJsonData.replace(token, "");
		}
		// ���ش������json �ַ���
		String[] strings = { token, finalUnchangedJsonData };
		return strings;
	}

	/**
	 * ��ȡ�����ļ��е�changedjson����
	 * 
	 * @param changed_json_path changedjson�ļ�·��
	 * @return �洢command commandInfo�ַ����ļ���
	 */
	public static List<String> changedJsonReader(String changed_json_path) {
		ObjectMapper mapper = new ObjectMapper();

		//changed_jsonת����map,�ṹΪ:Map<String, List<Map<String, Object>>>
		Map<String, List<Map<String, Object>>> map = new HashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> list;
		List<String> command_info = new ArrayList<String>();

		try {
			//��ȡchanged_json,ת����map����
			map = mapper.readValue(new File(changed_json_path),
					new TypeReference<Map<String, List<Map<String, Object>>>>() {
					});
			//����map
			Set<?> entries = map.entrySet();
			for (Iterator<?> iterator = entries.iterator(); iterator.hasNext();) {
				Map.Entry<String, List<Map<String, Object>>> entry = (Map.Entry<String, List<Map<String, Object>>>) iterator
						.next();

				String command = entry.getKey();
				list = entry.getValue();
				//����list����
				for (int i = 0; i < list.size(); i++) {
					Map<String, Object> maps = list.get(i);
					String commandInfo = mapper.writeValueAsString(maps);
					//����command,commandInfo json�ṹ
					command_info.add("\"command\":\"" + command + "\",\"commandInfo\":" + commandInfo + ",");
				}

			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return command_info;
	}

	/**
	 * ��ָ�� URL ����POST����
	 * 
	 * @param url url ����� URL
	 * @param headers ͷ��Ϣ,��ֵ��,���Ϊ�ն�ȡĬ�ϵ�
	 * @param jsonData requestData
	 * @return responseData
	 */
	public static String sendPost(String jsonData) {
		HttpURLConnection conn = null;
		StringBuffer sbf = new StringBuffer();
		try {
			URL realUrl = new URL(getConfig().get("url"));
			// ����http����
			conn = (HttpURLConnection) realUrl.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);

			// ����DMAuthorization��֤��Ϣ
			String DMAuthorization = MD5("deal" + jsonData + "moon");
			conn.setRequestProperty("DMAuthorization", DMAuthorization);
			conn.setRequestProperty("Accept-Charset", "UTF-8");
			conn.connect();

			// ��jsonData ���͵�������
			PrintWriter out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "utf-8"));
			out.println(jsonData);
			out.flush();
			out.close();

			if (conn.getResponseCode() == 200) {
				// ��ȡ�ظ���Ϣ
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String lines;
				while ((lines = reader.readLine()) != null) {
					lines = new String(lines.getBytes(), "utf-8");
					sbf.append(lines);
				}
				reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
				conn = null;
			}
		}
		return sbf.toString();
	}

	/**
	 * ��ȡproperties���͵������ļ�
	 * 
	 * @param path �ļ�·��
	 * @return map �洢��ֵ��
	 */
	public static Map<String, String> proReader(String path) {
		Reader reader;
		Map<String, String> map = new HashMap<String, String>();
		try {
			reader = new FileReader(path);
			Properties prop = new Properties();
			Set keySet = null;
			prop.load(reader);
			keySet = prop.keySet();
			Iterator it = keySet.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				// �������ļ��еļ�ֵ�Դ浽map��
				map.put(key, (String) prop.get(key));
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * MD5�����ַ������γ�32λ����
	 * 
	 * @param plainText �����ܵ��ַ���
	 * @return ���ܺ���ַ���
	 */
	public static String MD5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1, messageDigest);
			String hashtext = number.toString(16);
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}
			return hashtext;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * ��ȡ�ļ�
	 * 
	 * @param path �ļ�·��
	 * @return �ַ���
	 */
	public static String stringReader(String path) {
		File file = new File(path);
		StringBuffer jsonData = new StringBuffer();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String lines;

			while ((lines = br.readLine()) != null) {
				lines = new String(lines.getBytes(), "UTF-8");
				jsonData.append(lines);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonData.toString();
	}

	/**
	 * ��������ʽƥ���ַ���,ѡ��ĳ����
	 * 
	 * @param strSource Ҫƥ����ַ���
	 * @param pattern ������ʽ
	 * @param groupId ����ƥ�䣬ѡȡ�ĸ���
	 * @return ��Ӧ��Ľ���ַ���
	 */
	public static String matchString(String strSource, String pattern, int groupId) {
		Pattern _pattern = Pattern.compile(pattern);
		Matcher matcher = _pattern.matcher(strSource);
		String strResult = null;
		if (matcher.find()) {
			strResult = matcher.group(groupId);
		}
		return strResult;
	}

	/**
	 * �滻�ַ����еĿհ��ַ�,���绻��/�ո�/�س���
	 * 
	 * @param strSource ��������ַ���
	 * @return �������ַ���
	 */
	public static String removeBlank(String strSource) {
		return (strSource.replaceAll("\\s*", ""));
	}

	/**
	 * �жϷ�ɹ��ʱ���Ƿ����10��
	 * 
	 * @param publishedTime
	 * @return
	 */
	public static boolean judgeTime(long publishedTime) {
		return System.currentTimeMillis() - publishedTime > 10000;
	}

	/**
	 * ��ȡ�������������post�б�
	 * 
	 * @param url ����url
	 * @param type post����
	 * @param pageNum ҳ��
	 * @param pageSize ҳ���С
	 * @return �������������post id����
	 */
	public static List<String> getPostList(String type, int pageNum, int pageSize) {
		Map<String, List<String>> reqData = getAppRequestData();
		List<String> reqJsons = (List<String>) reqData.get("postgetlist");
		List<String> postid_author = new ArrayList<String>();

		String reqJson = "{" + reqJsons.get(0) + "}";
		String response = MyUtils.sendPost(reqJson);

		JSONArray array = JSONObject.fromObject(response).getJSONObject("responseData").getJSONArray("posts");
		for (Iterator<?> iterator = array.iterator(); iterator.hasNext();) {
			
			JSONObject obj = (JSONObject) iterator.next();
			JSONObject author = obj.getJSONObject("author");
			
			//���޵�����Ϊ0,��postʱ����10��,�û�������С��5��
			if (obj.getInt("likeNum") == 0 && judgeTime(obj.getLong("publishedTime")) && author.getInt("postNum") <= 5) {
				postid_author.add("" + obj.getInt("id") + "_" + author.getInt("id"));
			}
		}
		return postid_author;
	}

	/**
	 * �����ȡ1-3�����ظ���token
	 * 
	 * @param userId
	 * @return
	 */
	public static List<String> getRandomToken(String authorid) {

		List<String> rtokens = new ArrayList<String>();
		Random random = new Random();

		Map<String, String> user_token = getTokens();
		//ȥ����post������
		user_token.remove(authorid);

		String[] tokens = user_token.values().toArray(new String[0]);

		int count = random.nextInt(3) + 1;
		while(rtokens.size()<count){
			String token = tokens[new Random().nextInt(tokens.length - 1)];
			rtokens.add("\"token\":" + "\"" + token + "\",");
		}
		return rtokens;
	}

	/**
	 * ��̨�滻commandInfo�е�id
	 * 
	 * @param reqJson
	 * @param newId
	 * @return
	 */
	public static String replaceIdinBackCommand(String reqJson, String newId) {
		int oldId = JSONObject.fromObject(reqJson).getJSONObject("commandInfo").getInt("id");
		return reqJson.replace("\"id\":" + oldId, "\"id\":" + newId);
	}
}
>>>>>>> d06834c89164fab17bfda82de80cc12aac7f1547

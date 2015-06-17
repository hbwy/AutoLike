<<<<<<< HEAD
package com.dealmoon.autolike.service;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dealmoon.autolike.utils.MyUtils;

public class Service {

	private static Logger logger = Logger.getLogger(Service.class);
	private static Map<String, List<String>> reqData = MyUtils.getAppRequestData();

	/**
	 * éšæœº1-3ä¸ªç”¨æˆ·ç‚¹èµ
	 */
	public void postaddlike() {

		List<String> reqJsons = (List<String>) reqData.get("postaddlike");
		String reqJson = reqJsons.get(0);

		//è·å–æ»¡è¶³ç‚¹èµæ¡ä»¶çš„post
		List<String> postid_authors = MyUtils.getPostList("new", 1, 20);
		
		for (String postid_author : postid_authors) {
			String[] id_author = postid_author.split("_");
			
			List<String> tokens = MyUtils.getRandomToken(id_author[1]);
			//å¾ªç¯ç”¨æˆ·åˆ—è¡¨ç‚¹èµ
			for (String token : tokens) {
				addLike(token, reqJson, id_author[0]);
			}
		}
	}

	private void addLike(String token, String reqJson, String postId) {
		//1ä¸ªç”¨æˆ·ç»™1ä¸ªæ™’å•ç‚¹èµ
		String reqJson0 = "{" + token + reqJson + "}";
		reqJson0 = MyUtils.replaceIdinBackCommand(reqJson0, postId);
		String response = MyUtils.sendPost(reqJson0);
		logger.debug("postId:" + postId + " " + token + " " + "response:" + response);
	}
}
=======
package com.dealmoon.autolike.service;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dealmoon.autolike.utils.MyUtils;

public class Service {

	private static Logger logger = Logger.getLogger(Service.class);
	private static Map<String, List<String>> reqData = MyUtils.getAppRequestData();

	/**
	 * 1-3¸öËæ»úÓÃ»§¸øĞÂÓÃ»§µÄÉ¹µ¥µãÔŞ
	 */
	public void postaddlike() {

		List<String> reqJsons = (List<String>) reqData.get("postaddlike");
		String reqJson = reqJsons.get(0);

		//»ñÈ¡Âú×ãµãÔŞÌõ¼şµÄpostµÄid¼¯ºÏ
		List<String> postid_authors = MyUtils.getPostList("new", 1, 20);
		
		for (String postid_author : postid_authors) {
			String[] id_author = postid_author.split("_");
			
			List<String> tokens = MyUtils.getRandomToken(id_author[1]);
			//Ëæ»ú1-3¸öÓÃ»§µãÔŞ£¨³ıÈ¥ÁËpostµÄ×÷Õß£©
			for (String token : tokens) {
				addLike(token, reqJson, id_author[0]);
			}
		}
	}

	/**
	 * 1¸öÓÃ»§¸ø1¸öÉ¹µ¥µãÔŞ
	 * 
	 * @param token ÓÃ»§token
	 * @param reqJson ÇëÇóµÄjson´®
	 * @param postId É¹µ¥id
	 */
	private void addLike(String token, String reqJson, String postId) {
		//ÓÃ»§¸ø¸ÃpostµãÔŞ
		String reqJson0 = "{" + token + reqJson + "}";
		reqJson0 = MyUtils.replaceIdinBackCommand(reqJson0, postId);
		String response = MyUtils.sendPost(reqJson0);
		logger.debug("postId:" + postId + " " + token + " " + "response:" + response);
	}
}
>>>>>>> d06834c89164fab17bfda82de80cc12aac7f1547

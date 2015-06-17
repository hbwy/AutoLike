package com.dealmoon.autolike.service;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dealmoon.autolike.utils.MyUtils;

public class Service {

	private static Logger logger = Logger.getLogger(Service.class);
	private static Map<String, List<String>> reqData = MyUtils.getAppRequestData();

	/**
	 * 随机1-3个用户点赞
	 */
	public void postaddlike() {

		List<String> reqJsons = (List<String>) reqData.get("postaddlike");
		String reqJson = reqJsons.get(0);

		//获取满足点赞条件的post
		List<String> postid_authors = MyUtils.getPostList("new", 1, 20);
		
		for (String postid_author : postid_authors) {
			String[] id_author = postid_author.split("_");
			
			List<String> tokens = MyUtils.getRandomToken(id_author[1]);
			//循环用户列表点赞
			for (String token : tokens) {
				addLike(token, reqJson, id_author[0]);
			}
		}
	}

	private void addLike(String token, String reqJson, String postId) {
		//1个用户给1个晒单点赞
		String reqJson0 = "{" + token + reqJson + "}";
		reqJson0 = MyUtils.replaceIdinBackCommand(reqJson0, postId);
		String response = MyUtils.sendPost(reqJson0);
		logger.debug("postId:" + postId + " " + token + " " + "response:" + response);
	}
}

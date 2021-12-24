/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 提供通过HTTP协议获取内容的方法
 * 所有提供方法中的params参数在内部不会进行自动的url encode，如果提交参数需要进行url encode，请调用方自行处理
 * <p>
 * https://blog.csdn.net/RobinsStruggle/article/details/86074058
 *
 * @author bwcx_jzy
 */
public class HttpUtils {

	/**
	 * 日志输出
	 */
	private static final Log log = new SystemStreamLog();

	/**
	 * 支持的Http method
	 */
	private enum HttpMethod {
		/**
		 *
		 */
		POST, DELETE, GET, PUT, HEAD
	}

	/**
	 * 构建参数 字符串
	 *
	 * @param params   参数
	 * @param encoding 编码格式
	 * @return xx=xx&xxxxx=sss
	 * @throws UnsupportedEncodingException 编码错误
	 */
	private static String buildParams(Map<String, String> params, String encoding) throws UnsupportedEncodingException {
		if (params == null) {
			return null;
		}
		StringBuilder paramsStr = new StringBuilder();
		Set<Map.Entry<String, String>> entries = params.entrySet();
		for (Map.Entry<String, String> entry : entries) {
			String value = (entry.getValue() != null) ? (entry.getValue()) : "";
			value = URLEncoder.encode(value, encoding);
			paramsStr.append(entry.getKey()).append("=").append(value).append("&");
		}
		return paramsStr.toString();
	}


	private static String invokeUrl(String url, Map<String, String> params, Map<String, String> headers, int connectTimeout, int readTimeout, String encoding, HttpMethod method) throws UnsupportedEncodingException {
		//构造请求参数字符串
		String paramsStr = buildParams(params, encoding);
		//只有POST方法才能通过OutputStream(即form的形式)提交参数
		if (method != HttpMethod.POST) {
			url += "?" + paramsStr;
		}

		HttpURLConnection conn = null;
		BufferedWriter out = null;
		BufferedReader in = null;
		try {
			//创建和初始化连接
			URL uUrl = new URL(url);
			conn = (HttpURLConnection) uUrl.openConnection();
			conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");
			conn.setRequestMethod(method.toString());
			conn.setDoOutput(true);
			conn.setDoInput(true);
			//设置连接超时时间
			conn.setConnectTimeout(connectTimeout);
			//设置读取超时时间
			conn.setReadTimeout(readTimeout);
			//指定请求header参数
			if (headers != null && headers.size() > 0) {
				Set<String> headerSet = headers.keySet();
				for (String key : headerSet) {
					conn.setRequestProperty(key, headers.get(key));
				}
			}

			if (paramsStr != null && method == HttpMethod.POST) {
				//发送请求参数
				out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), encoding));
				out.write(paramsStr);
				out.flush();
			}

			//接收返回结果
			StringBuilder result = new StringBuilder();
			try (InputStream inputStream = conn.getInputStream()) {
				in = new BufferedReader(new InputStreamReader(inputStream, encoding));
				String line = "";
				while ((line = in.readLine()) != null) {
					result.append(line);
				}
			}
			return result.toString();
		} catch (Exception e) {
			log.error("网络异常", e);
			if (conn == null) {
				return null;
			}
			try {
				//处理错误流，提高http连接被重用的几率
				byte[] buf = new byte[100];
				InputStream es = conn.getErrorStream();
				if (es != null) {
					while (es.read(buf) > 0) {
					}
				}
				IoUtil.close(es);
			} catch (Exception e1) {
				log.error("网络异常", e);
			}
		} finally {
			IoUtil.close(out);
			IoUtil.close(in);
			//关闭连接
			if (conn != null) {
				conn.disconnect();
			}
		}
		return null;
	}


	/**
	 * POST方法提交Http请求，语义为“增加”
	 * <p>
	 * 注意：Http方法中只有POST方法才能使用body来提交内容
	 *
	 * @param url            资源路径（如果url中已经包含参数，则params应该为null）
	 * @param params         参数
	 * @param headers        请求头参数
	 * @param connectTimeout 连接超时时间（单位为ms）
	 * @param readTimeout    读取超时时间（单位为ms）
	 * @param charset        字符集（一般该为“utf-8”）
	 * @return resopen
	 */
	public static String post(String url, Map<String, String> params, Map<String, String> headers, int connectTimeout, int readTimeout, String charset) throws UnsupportedEncodingException {
		return invokeUrl(url, params, headers, connectTimeout, readTimeout, charset, HttpMethod.POST);
	}


	public static String get(String url, Map<String, String> params, int connectTimeout, int readTimeout, String charset) throws UnsupportedEncodingException {
		return invokeUrl(url, params, null, connectTimeout, readTimeout, charset, HttpMethod.GET);
	}

	public static void main(String[] args) throws UnsupportedEncodingException {

		//
		Map<String, String> par = new HashMap<>();
		par.put("tset", "sdsdf+dfsfsdf");

		String poststr = post("http://localhost:2122/api/build/60bef020e2b04ba5a92248192ad1802f/f8chn0mji3", par, null, 3000, 3000, "UTF-8");
		System.out.println(poststr);

		String str = get("http://localhost:2122/api/build/60bef020e2b04ba5a92248192ad1802f/f8chn0mji3", null, 3000, 3000, "UTF-8");
		System.out.println(str);
	}


}


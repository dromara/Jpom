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
package io.jpom.service.node.tomcat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.service.node.NodeService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * tomcat
 *
 * @author lf
 */
@Service
public class TomcatService {


	private final NodeService nodeService;

	public TomcatService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * 查询tomcat列表
	 *
	 * @param nodeModel 节点信息
	 * @return tomcat的信息
	 */
	public JSONArray getTomcatList(NodeModel nodeModel) {
		if (!nodeModel.isOpenStatus()) {
			return null;
		}
		return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_List, JSONArray.class, null, null);
	}

	/**
	 * 查询tomcat信息
	 *
	 * @param nodeModel 节点信息
	 * @param id        tomcat的id
	 * @return tomcat的信息
	 */
	public JSONObject getTomcatInfo(NodeModel nodeModel, String id) {
		return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_GetItem, JSONObject.class, "id", id);
	}


	public JSONArray getTomcatProjectList(NodeModel nodeModel, String id) {
		return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_GetTomcatProjectList, JSONArray.class, "id", id);
	}

	/**
	 * tomcat项目管理
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String tomcatProjectManage(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_TomcatProjectManage).toString();
	}

	/**
	 * 新增Tomcat
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String addTomcat(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Add).toString();
	}

	/**
	 * 更新Tomcat信息
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String updateTomcat(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Update).toString();
	}

	/**
	 * 查询tomcat运行状态
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String getTomcatStatus(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_GetTomcatStatus).toString();
	}

	/**
	 * 启动tomcat
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String start(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Start).toString();
	}

	/**
	 * 停止tomcat
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String stop(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Stop).toString();
	}

	/**
	 * 重启tomcat
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String restart(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Restart).toString();
	}

	/**
	 * 删除tomcat
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String delete(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Delete).toString();
	}

	/**
	 * 获取文件列表
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String getFileList(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_GetFileList).toString();
	}

	/**
	 * 上传文件
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String upload(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_Upload).toString();
	}

	/**
	 * 下载文件
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @param response  响应信息
	 */
	public void download(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response) {
		NodeForward.requestDownload(nodeModel, request, response, NodeUrl.Tomcat_File_Download);
	}

	/**
	 * 删除文件
	 *
	 * @param nodeModel 节点信息
	 * @param request   请求信息
	 * @return 操作结果
	 */
	public String deleteFile(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_DeleteFile).toString();
	}

	/**
	 * 上传War包
	 *
	 * @param node         节点信息
	 * @param multiRequest 请求信息
	 * @return 操作结果
	 */
	public String uploadWar(NodeModel node, MultipartHttpServletRequest multiRequest) {
		return NodeForward.requestMultipart(node, multiRequest, NodeUrl.Tomcat_File_UploadWar).toString();
	}
}

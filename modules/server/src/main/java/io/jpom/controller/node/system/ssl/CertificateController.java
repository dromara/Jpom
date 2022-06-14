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
package io.jpom.controller.node.system.ssl;

import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.WhitelistDirectoryService;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 证书管理
 *
 * @author Arno
 */
@Controller
@RequestMapping(value = "/node/system/certificate")
@Feature(cls = ClassFeature.SSL)
@SystemPermission
public class CertificateController extends BaseServerController {

	private final WhitelistDirectoryService whitelistDirectoryService;

	public CertificateController(WhitelistDirectoryService whitelistDirectoryService) {
		this.whitelistDirectoryService = whitelistDirectoryService;
	}

	/**
	 * @return
	 * @author Hotstrip
	 * load Cert white list data
	 */
	@RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String loadWhiteList() {
		List<String> list = whitelistDirectoryService.getCertificateDirectory(getNode());
		return JsonMessage.getString(200, "success", list);
	}

	/**
	 * 保存证书
	 *
	 * @return json
	 */
	@RequestMapping(value = "/saveCertificate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String saveCertificate() {
		if (ServletFileUpload.isMultipartContent(getRequest())) {
			return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.System_Certificate_saveCertificate).toString();
		}
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Certificate_saveCertificate).toString();
	}


	/**
	 * 证书列表
	 *
	 * @return json
	 */
	@RequestMapping(value = "/getCertList", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String getCertList() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Certificate_getCertList).toString();
	}

	/**
	 * 删除证书
	 *
	 * @return json
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DEL)
	public String delete() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Certificate_delete).toString();
	}


	/**
	 * 导出证书
	 */
	@RequestMapping(value = "/export", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DOWNLOAD)
	public void export() {
		NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.System_Certificate_export);
	}
}

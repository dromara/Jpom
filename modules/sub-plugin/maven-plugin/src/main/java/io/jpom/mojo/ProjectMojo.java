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
package io.jpom.mojo;

import io.jpom.entity.NodeProjectInfo;
import io.jpom.entity.ProjectInfo;
import io.jpom.util.HttpUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * mojo
 *
 * @author bwcx_jzy
 * @since 2019/11/18
 */
@Mojo(name = "jpom-project", defaultPhase = LifecyclePhase.PACKAGE)
public class ProjectMojo extends AbstractMojo {

	@Parameter(required = true)
	private String url;

	@Parameter(required = true)
	private String token;

	@Parameter(required = true)
	private List<String> nodeIds;

	@Parameter
	private final ProjectInfo project = new ProjectInfo();

	@Parameter
	private final List<NodeProjectInfo> nodeProjects = new ArrayList<>();

	public ProjectMojo() {
	}


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// 验证参数
//        String regex = "://(.*?):(.*)";
//        Pattern p = Pattern.compile(regex);
//        Matcher m = p.matcher(url);
//        if (!m.find()) {
//            getLog().error("请配置 正确的Jpom Server地址");
//            return;
//        }
		Log log = getLog();
		if (nodeIds != null) {
			for (String nodeId : nodeIds) {
				NodeProjectInfo projectInfo = findItem(nodeId);
				if (projectInfo == null) {
					projectInfo = new NodeProjectInfo(project);
					projectInfo.setNodeId(nodeId);
				} else {
					projectInfo.copy(project);
				}
				try {
					this.send(projectInfo);
				} catch (UnsupportedEncodingException e) {
					log.error("编码错误", e);
				}
			}
		}

		for (NodeProjectInfo nodeProject : nodeProjects) {
			try {
				this.send(nodeProject);
			} catch (UnsupportedEncodingException e) {
				log.error("编码错误", e);
			}
		}
		log.info("处理结束");
	}

	private NodeProjectInfo findItem(String nodeId) {
		Iterator<NodeProjectInfo> iterator = nodeProjects.iterator();
		while (iterator.hasNext()) {
			NodeProjectInfo nodeProjectInfo = iterator.next();
			if (nodeId.equals(nodeProjectInfo.getNodeId())) {
				iterator.remove();
				return nodeProjectInfo;
			}
		}
		return null;
	}

	/**
	 * 验证信息是否满足条件
	 *
	 * @param nodeProjectInfo info
	 * @return true 满足
	 */
	private boolean checkInfo(NodeProjectInfo nodeProjectInfo) {
		String name = nodeProjectInfo.getName();
		Log log = getLog();
		if (name == null || "".equals(name)) {
			log.error("请配置 project.name");
			return false;
		}

		String id = nodeProjectInfo.getId();
		if (id == null || "".equals(id)) {
			log.error("请配置 project.id");
			return false;
		}

		String runMode = nodeProjectInfo.getRunMode();
		if (runMode == null || "".equals(runMode)) {
			log.error("请配置 project.runMode");
			return false;
		}

		String whitelistDirectory = nodeProjectInfo.getWhitelistDirectory();
		if (whitelistDirectory == null || "".equals(whitelistDirectory)) {
			log.error("请配置 project.whitelistDirectory");
			return false;
		}

		String path = nodeProjectInfo.getPath();
		if (path == null || "".equals(path)) {
			log.error("请配置 project.path");
			return false;
		}
		return true;
	}

	/**
	 * 发送同步信息请求
	 *
	 * @param nodeProjectInfo info
	 */
	private void send(NodeProjectInfo nodeProjectInfo) throws UnsupportedEncodingException {
		if (!checkInfo(nodeProjectInfo)) {
			return;
		}
		Map<String, String> parameter = new HashMap<>(20);
		parameter.put("name", nodeProjectInfo.getName());
		parameter.put("group", nodeProjectInfo.getGroup());
		parameter.put("id", nodeProjectInfo.getId());
		parameter.put("runMode", nodeProjectInfo.getRunMode());
		parameter.put("whitelistDirectory", nodeProjectInfo.getWhitelistDirectory());
		parameter.put("lib", nodeProjectInfo.getPath());
		//
		parameter.put("mainClass", nodeProjectInfo.getMainClass());
		parameter.put("jvm", nodeProjectInfo.getJvm());
		parameter.put("args", nodeProjectInfo.getArgs());
		parameter.put("token", nodeProjectInfo.getWebHook());
		// 处理副本集
		List<NodeProjectInfo.JavaCopy> javaCopys = nodeProjectInfo.getJavaCopys();
		if (javaCopys != null) {
			StringBuilder ids = new StringBuilder();
			for (NodeProjectInfo.JavaCopy javaCopy : javaCopys) {
				String copyId = javaCopy.getId();
				if (copyId == null || copyId.length() <= 0) {
					continue;
				}
				if (ids.length() > 0) {
					ids.append(",");
				}
				ids.append(copyId);
				parameter.put("jvm_" + copyId, javaCopy.getJvm());
				parameter.put("args_" + copyId, javaCopy.getArgs());
			}
			parameter.put("javaCopyIds", ids.toString());
		}
		// header
		Map<String, String> header = new HashMap<>(5);
		parameter.put("nodeId", nodeProjectInfo.getNodeId());
		// url
		String allUrl = String.format("%s/node/manage/saveProject", url);
		header.put("JPOM-USER-TOKEN", token);
		Log log = getLog();
		log.info("处理：" + nodeProjectInfo.getNodeId());
		String post = HttpUtils.post(allUrl, parameter, header,
				(int) TimeUnit.MINUTES.toMillis(1),
				(int) TimeUnit.MINUTES.toMillis(1), "utf-8");
		log.info(post);
	}
}

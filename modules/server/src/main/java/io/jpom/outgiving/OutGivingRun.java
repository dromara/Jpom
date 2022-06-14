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
package io.jpom.outgiving;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.outgiving.OutGivingNodeProject;
import io.jpom.service.outgiving.OutGivingServer;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 分发线程
 *
 * @author bwcx_jzy
 * @since 2019/7/18
 **/
public class OutGivingRun {

	/**
	 * 开始异步执行分发任务
	 *
	 * @param id        分发id
	 * @param file      文件
	 * @param userModel 操作的用户
	 * @param unzip     解压
	 */
	public static void startRun(String id,
								File file,
								UserModel userModel,
								boolean unzip) {
		OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
		OutGivingModel item = outGivingServer.getByKey(id);
		Objects.requireNonNull(item, "不存在分发");
		AfterOpt afterOpt = ObjectUtil.defaultIfNull(EnumUtil.likeValueOf(AfterOpt.class, item.getAfterOpt()), AfterOpt.No);

		//
		List<OutGivingNodeProject> outGivingNodeProjects = item.outGivingNodeProjectList();
		// 开启线程
		if (afterOpt == AfterOpt.Order_Restart || afterOpt == AfterOpt.Order_Must_Restart) {
			ThreadUtil.execute(() -> {
				// 截取睡眠时间
				int sleepTime = ObjectUtil.defaultIfNull(item.getIntervalTime(), 10);
				//
				boolean cancel = false;
				for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjects) {
					if (cancel) {
						String userId = userModel == null ? JpomApplication.SYSTEM_ID : userModel.getId();
						OutGivingItemRun.updateStatus(null, id, outGivingNodeProject, OutGivingNodeProject.Status.Cancel, "前一个节点分发失败，取消分发", userId);
					} else {
						OutGivingItemRun outGivingRun = new OutGivingItemRun(item, outGivingNodeProject, file, userModel, unzip);
						OutGivingNodeProject.Status status = outGivingRun.call();
						if (status != OutGivingNodeProject.Status.Ok) {
							if (afterOpt == AfterOpt.Order_Must_Restart) {
								// 完整重启，不再继续剩余的节点项目
								cancel = true;
							}
						}
						// 休眠x秒 等待之前项目正常启动
						ThreadUtil.sleep(sleepTime, TimeUnit.SECONDS);
					}
				}
			});
		} else if (afterOpt == AfterOpt.Restart || afterOpt == AfterOpt.No) {
			outGivingNodeProjects.forEach(outGivingNodeProject ->
					ThreadUtil.execAsync(new OutGivingItemRun(item, outGivingNodeProject, file, userModel, unzip))
			);
		} else {
			//
			throw new IllegalArgumentException("Not implemented " + afterOpt.getDesc());
		}
	}


	/**
	 * 上传项目文件
	 *
	 * @param file      需要上传的文件
	 * @param projectId 项目id
	 * @param unzip     是否需要解压
	 * @param afterOpt  是否需要重启
	 * @param nodeModel 节点
	 * @param userModel 操作用户
	 * @return json
	 */
	public static JsonMessage<String> fileUpload(File file, String levelName, String projectId,
												 boolean unzip,
												 AfterOpt afterOpt,
												 NodeModel nodeModel,
												 UserModel userModel,
												 boolean clearOld) {
		JSONObject data = new JSONObject();
		data.put("file", file);
		data.put("id", projectId);
		if (StrUtil.isNotEmpty(levelName)) {
			data.put("levelName", levelName);
		}
		if (unzip) {
			// 解压
			data.put("type", "unzip");
		}
		if (clearOld) {
			// 清空
			data.put("clearType", "clear");
		}
		// 操作
		if (afterOpt != AfterOpt.No) {
			data.put("after", afterOpt.getCode());
		}
		return NodeForward.request(nodeModel, NodeUrl.Manage_File_Upload, userModel, data);
	}
}

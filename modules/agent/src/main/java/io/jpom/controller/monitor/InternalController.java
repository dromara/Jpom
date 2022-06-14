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
package io.jpom.controller.monitor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.system.NetstatModel;
import io.jpom.model.system.ProcessModel;
import io.jpom.system.AgentConfigBean;
import io.jpom.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * 内存查看
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class InternalController extends BaseAgentController {

	/**
	 * 获取内存信息
	 *
	 * @param tag 程序运行标识
	 * @return json
	 * @throws Exception 异常
	 */
	@RequestMapping(value = "internal_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getInternal(String tag, String copyId) throws Exception {
		String tagId = NodeProjectInfoModel.JavaCopyItem.getTagId(tag, copyId);
		int pid = AbstractProjectCommander.getInstance().getPid(tagId);
		if (pid <= 0) {
			return JsonMessage.getString(400, "");
		}
		JSONObject jsonObject = new JSONObject();
		ProcessModel item = AbstractSystemCommander.getInstance().getPidInfo(pid);
		jsonObject.put("process", item);
		//JSONObject beanMem = getBeanMem(tagId);
		//jsonObject.put("beanMem", beanMem);
		//获取端口信息
		List<NetstatModel> netstatModels = AbstractProjectCommander.getInstance().listNetstat(pid, false);
		jsonObject.put("netstat", netstatModels);
		return JsonMessage.getString(200, "", jsonObject);
//		return JsonMessage.getString(404, "功能已经下架");
	}

	/**
	 * 查询监控线程列表
	 *
	 * @param tag 程序运行标识
	 * @return json
	 * @throws Exception 异常
	 */
	@RequestMapping(value = "threadInfos", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getThreadInfos(String tag, String copyId) throws Exception {
//		int limit = getParameterInt("limit", 10);
//		int page = getParameterInt("page", 1);
//
//		ThreadMXBean bean = JvmUtil.getThreadMXBean(ProjectInfoModel.JavaCopyItem.getTagId(tag, copyId));
//		if (bean == null) {
//			return JsonMessage.getString(400, "未获取到对应信息");
//		}
//		//启用线程争用监视
//		bean.setThreadContentionMonitoringEnabled(true);
//		ThreadInfo[] threadInfos = bean.dumpAllThreads(false, false);
//		if (threadInfos == null || threadInfos.length <= 0) {
//			return JsonMessage.getString(404, "没有获取到任何线程");
//		}
//
//		JSONArray array = new JSONArray();
//		int index = PageUtil.getStart(page, limit);
//		int len = limit + index;
//		int length = threadInfos.length;
//		if (len > length) {
//			len = length;
//		}
//		for (int i = index; i < len; i++) {
//			ThreadInfo threadInfo = threadInfos[i];
//			Thread.State threadState = threadInfo.getThreadState();
//			JSONObject object = new JSONObject();
//			object.put("id", threadInfo.getThreadId());
//			object.put("name", threadInfo.getThreadName());
//			object.put("status", threadState);
//			object.put("waitedCount", threadInfo.getWaitedCount());
//			object.put("waitedTime", threadInfo.getWaitedTime());
//			object.put("blockedCount", threadInfo.getBlockedCount());
//			object.put("blockedTime", threadInfo.getBlockedTime());
//			object.put("isInNative", threadInfo.isInNative());
//			object.put("isSuspended", threadInfo.isSuspended());
//			array.add(object);
//		}
//		JSONObject object = new JSONObject();
//		object.put("count", length);
//		object.put("data", array);
//		return JsonMessage.getString(200, "", object);
		return JsonMessage.getString(404, "功能已经下架");
	}

//	/**
//	 * 获取jvm内存
//	 *
//	 * @param tag tag
//	 * @return JSONObject
//	 */
//	private JSONObject getBeanMem(String tag) {
//		try {
//			MemoryMXBean bean = JvmUtil.getMemoryMXBean(tag);
//			if (bean == null) {
//				return null;
//			}
//			JSONObject jsonObject = new JSONObject();
//			jsonObject.put("mount", bean.getObjectPendingFinalizationCount());
//			//堆内存
//			MemoryUsage memory = bean.getHeapMemoryUsage();
//			//非堆内存
//			MemoryUsage nonHeapMemoryUsage = bean.getNonHeapMemoryUsage();
//			long used = memory.getUsed();
//			long max = memory.getMax();
//			//未定义
//			if (-1 == max) {
//				max = memory.getCommitted();
//			}
//			//计算使用内存占最大内存的百分比
//			double v = new BigDecimal(used).divide(new BigDecimal(max), 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
//			jsonObject.put("heapUsed", FileUtil.readableFileSize(used));
//			jsonObject.put("heapProportion", String.format("%.2f", v) + "%");
//			jsonObject.put("heapCommitted", FileUtil.readableFileSize(memory.getCommitted()));
//			long nonUsed = nonHeapMemoryUsage.getUsed();
//			long nonMax = nonHeapMemoryUsage.getMax();
//			long nonCommitted = nonHeapMemoryUsage.getCommitted();
//			if (-1 == nonMax) {
//				nonMax = nonCommitted;
//			}
//			jsonObject.put("nonHeapUsed", FileUtil.readableFileSize(nonUsed));
//			double proportion = new BigDecimal(nonUsed).divide(new BigDecimal(nonMax), 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
//			jsonObject.put("nonHeapProportion", String.format("%.2f", proportion) + "%");
//			jsonObject.put("nonHeapCommitted", FileUtil.readableFileSize(nonCommitted));
//			return jsonObject;
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		}
//		return null;
//	}


	/**
	 * 导出堆栈信息
	 *
	 * @param tag 程序运行标识
	 * @return json
	 */
	@RequestMapping(value = "internal_stack", method = RequestMethod.GET)
	@ResponseBody
	public String stack(String tag, String copyId) {
		tag = NodeProjectInfoModel.JavaCopyItem.getTagId(tag, copyId);
		//
		String fileName = AgentConfigBean.getInstance().getTempPathName() + StrUtil.SLASH + tag + "_java_cpu.txt";
		fileName = FileUtil.normalize(fileName);
		try {
			int pid = AbstractProjectCommander.getInstance().getPid(tag);
			if (pid <= 0) {
				return JsonMessage.getString(400, "未运行");
			}
			String command = String.format("jstack -F %s >> %s ", pid, fileName);
			CommandUtil.execSystemCommand(command);
			downLoad(getResponse(), fileName);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
//            getResponse().sendRedirect("internal?tag=" + tag);
		}
		return JsonMessage.getString(200, "");
	}

	/**
	 * 导出内存信息
	 *
	 * @param tag 程序运行标识
	 * @return json
	 */
	@RequestMapping(value = "internal_ram", method = RequestMethod.GET)
	@ResponseBody
	public String ram(String tag, String copyId) {
		tag = NodeProjectInfoModel.JavaCopyItem.getTagId(tag, copyId);
		//
		String fileName = AgentConfigBean.getInstance().getTempPathName() + StrUtil.SLASH + tag + "_java_ram.txt";
		fileName = FileUtil.normalize(fileName);
		try {
			int pid = AbstractProjectCommander.getInstance().getPid(tag);
			if (pid <= 0) {
				return JsonMessage.getString(400, "未运行");
			}
			String command = String.format("jmap -histo:live %s >> %s", pid, fileName);
			CommandUtil.execSystemCommand(command);
			downLoad(getResponse(), fileName);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
//            getResponse().sendRedirect("internal?tag=" + tag);
		}
		return JsonMessage.getString(200, "");
	}

	/**
	 * 下载文件
	 *
	 * @param response response
	 * @param fileName 文件名字
	 */
	private void downLoad(HttpServletResponse response, String fileName) {
		//获取项目根路径
		File file = new File(fileName);
		ServletUtil.write(response, file);
		FileUtil.del(file);
	}
}

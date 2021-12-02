/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
//package io.jpom.controller.build;
//
//import cn.hutool.core.convert.Convert;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.io.LineHandler;
//import cn.hutool.core.thread.ThreadUtil;
//import cn.hutool.core.util.CharsetUtil;
//import cn.jiangzeyin.common.JsonMessage;
//import cn.jiangzeyin.common.validator.ValidatorConfig;
//import cn.jiangzeyin.common.validator.ValidatorItem;
//import cn.jiangzeyin.common.validator.ValidatorRule;
//import com.alibaba.fastjson.JSONObject;
//import io.jpom.build.BuildManage;
//import io.jpom.build.BuildUtil;
//import io.jpom.build.ReleaseManage;
//import io.jpom.common.BaseServerController;
//import io.jpom.common.interceptor.OptLog;
//import io.jpom.model.BaseEnum;
//import io.jpom.model.data.BuildModel;
//import io.jpom.model.data.UserModel;
//import io.jpom.model.log.BuildHistoryLog;
//import io.jpom.model.log.UserOperateLogV1;
//import io.jpom.plugin.ClassFeature;
//import io.jpom.plugin.Feature;
//import io.jpom.plugin.MethodFeature;
//import io.jpom.service.build.BuildService;
//import io.jpom.service.dblog.DbBuildHistoryLogService;
//import io.jpom.util.LimitQueue;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//import java.io.File;
//import java.util.Objects;
//
///**
// * -- @author bwcx_jzy
// * -- @date 2019/7/16
// *
// * @author Hotstrip
// * @see BuildInfoManageController
// * @since 2021-08-23
// */
//@RestController
//@RequestMapping(value = "/build")
//@Feature(cls = ClassFeature.BUILD)
//@Deprecated
//public class BuildManageController extends BaseServerController {
//
//	@Resource
//	private BuildService buildService;
//	@Resource
//	private DbBuildHistoryLogService dbBuildHistoryLogService;
//
//	/**
//	 * 开始构建
//	 *
//	 * @param id id
//	 * @return json
//	 */
//	@RequestMapping(value = "start.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//	@OptLog(UserOperateLogV1.OptType.StartBuild)
//	@Feature(method = MethodFeature.EXECUTE)
//	public String start(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id) {
//		return buildService.start(getUser(), id);
//	}
//
//	/**
//	 * 取消构建
//	 *
//	 * @param id id
//	 * @return json
//	 */
//	@RequestMapping(value = "cancel.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//	@OptLog(UserOperateLogV1.OptType.CancelBuild)
//	@Feature(method = MethodFeature.EXECUTE)
//	public String cancel(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id) {
//		BuildModel item = buildService.getItem(id);
//		Objects.requireNonNull(item, "没有对应数据");
//		BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, item.getStatus());
//		Objects.requireNonNull(nowStatus);
//		if (BuildStatus.Ing != nowStatus && BuildStatus.PubIng != nowStatus) {
//			return JsonMessage.getString(501, "当前状态不在进行中");
//		}
//		boolean status = BuildManage.cancel(item.getId());
//		if (!status) {
//			item.setStatus(BuildStatus.Cancel.getCode());
//			buildService.updateItem(item);
//		}
//		return JsonMessage.getString(200, "取消成功");
//	}
//
//	/**
//	 * 重新发布
//	 *
//	 * @param logId logId
//	 * @return json
//	 */
//	@RequestMapping(value = "reRelease.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//	@OptLog(UserOperateLogV1.OptType.ReReleaseBuild)
//	@Feature(method = MethodFeature.EXECUTE)
//	public String reRelease(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) {
//		BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId);
//		Objects.requireNonNull(buildHistoryLog, "没有对应构建记录.");
//		BuildModel item = buildService.getItem(buildHistoryLog.getBuildDataId());
//		Objects.requireNonNull(item, "没有对应数据");
//		String e = buildService.checkStatus(item.getStatus());
//		if (e != null) {
//			return e;
//		}
//		UserModel userModel = getUser();
//		ReleaseManage releaseManage = new ReleaseManage(buildHistoryLog, userModel);
//		// 标记发布中
//		releaseManage.updateStatus(BuildStatus.PubIng);
//		ThreadUtil.execute(releaseManage::start2);
//		return JsonMessage.getString(200, "重新发布中");
//	}
//
//	/**
//	 * 获取构建的日志
//	 *
//	 * @param id      id
//	 * @param buildId 构建编号
//	 * @param line    需要获取的行号
//	 * @return json
//	 */
//	@RequestMapping(value = "getNowLog.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//	@Feature(method = MethodFeature.EXECUTE)
//	public String getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
//							@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "没有buildId") int buildId,
//							@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
//		BuildModel item = buildService.getItem(id);
//		if (item == null) {
//			return JsonMessage.getString(404, "没有对应数据");
//		}
//		if (buildId > item.getBuildId()) {
//			return JsonMessage.getString(405, "还没有对应的构建记录");
//		}
//		File file = BuildUtil.getLogFile(item.getId(), buildId);
//		if (file.isDirectory()) {
//			return JsonMessage.getString(300, "日志文件错误");
//		}
//		if (!file.exists()) {
//			if (buildId == item.getBuildId()) {
//				return JsonMessage.getString(201, "还没有日志文件");
//			}
//			return JsonMessage.getString(300, "日志文件不存在");
//		}
//		JSONObject data = new JSONObject();
//		// 运行中
//		data.put("run", item.getStatus() == BuildStatus.Ing.getCode() || item.getStatus() == BuildStatus.PubIng.getCode());
//		// 构建中
//		data.put("buildRun", item.getStatus() == BuildStatus.Ing.getCode());
//		// 读取文件
//		int linesInt = Convert.toInt(line, 1);
//		LimitQueue<String> lines = new LimitQueue<>(500);
//		final int[] readCount = {0};
//		FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8, (LineHandler) line1 -> {
//			readCount[0]++;
//			if (readCount[0] < linesInt) {
//				return;
//			}
//			lines.add(line1);
//		});
//		// 下次应该获取的行数
//		data.put("line", readCount[0] + 1);
//		data.put("getLine", linesInt);
//		data.put("dataLines", lines);
//		return JsonMessage.getString(200, "ok", data);
//	}
//}

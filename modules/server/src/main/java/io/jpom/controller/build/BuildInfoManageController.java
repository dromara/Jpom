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
package io.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.build.BuildInfoManage;
import io.jpom.build.BuildUtil;
import io.jpom.build.ReleaseManage;
import io.jpom.common.BaseServerController;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.util.LimitQueue;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Objects;

/**
 * new build info manage controller
 * ` *
 *
 * @author Hotstrip
 * @since 2021-08-23
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoManageController extends BaseServerController {


	private final BuildInfoService buildInfoService;
	private final DbBuildHistoryLogService dbBuildHistoryLogService;

	public BuildInfoManageController(BuildInfoService buildInfoService,
									 DbBuildHistoryLogService dbBuildHistoryLogService) {
		this.buildInfoService = buildInfoService;
		this.dbBuildHistoryLogService = dbBuildHistoryLogService;
	}

	/**
	 * 开始构建
	 *
	 * @param id id
	 * @return json
	 */
	@RequestMapping(value = "/build/manage/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String start(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id) {
		BuildInfoModel item = buildInfoService.getByKey(id, getRequest());
		Assert.notNull(item, "没有对应数据");
		// userModel
		UserModel userModel = getUser();
		// 执行构建
		return buildInfoService.start(item.getId(), userModel, null, 0).toString();
	}

	/**
	 * 取消构建
	 *
	 * @param id id
	 * @return json
	 */
	@RequestMapping(value = "/build/manage/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String cancel(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id) {
		BuildInfoModel item = buildInfoService.getByKey(id, getRequest());
		Objects.requireNonNull(item, "没有对应数据");
		BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, item.getStatus());
		Objects.requireNonNull(nowStatus);
		if (BuildStatus.Ing != nowStatus && BuildStatus.PubIng != nowStatus) {
			return JsonMessage.getString(501, "当前状态不在进行中");
		}
		boolean status = BuildInfoManage.cancel(item.getId());
		if (!status) {
			item.setStatus(BuildStatus.Cancel.getCode());
			buildInfoService.update(item);
		}
		return JsonMessage.getString(200, "取消成功");
	}

	/**
	 * 重新发布
	 *
	 * @param logId logId
	 * @return json
	 */
	@RequestMapping(value = "/build/manage/reRelease", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String reRelease(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) {
		BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, getRequest());
		Objects.requireNonNull(buildHistoryLog, "没有对应构建记录.");
		BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId());
		Objects.requireNonNull(item, "没有对应数据");
		String e = buildInfoService.checkStatus(item.getStatus());
		Assert.isNull(e, () -> e);
		UserModel userModel = getUser();
		ReleaseManage releaseManage = new ReleaseManage(buildHistoryLog, userModel);
		// 标记发布中
		releaseManage.updateStatus(BuildStatus.PubIng);
		ThreadUtil.execute(releaseManage::start2);
		return JsonMessage.getString(200, "重新发布中");
	}

	/**
	 * 获取构建的日志
	 *
	 * @param id      id
	 * @param buildId 构建编号
	 * @param line    需要获取的行号
	 * @return json
	 */
	@RequestMapping(value = "/build/manage/get-now-log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
							@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "没有buildId") int buildId,
							@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
		BuildInfoModel item = buildInfoService.getByKey(id, getRequest());
		Assert.notNull(item, "没有对应数据");
		Assert.state(buildId <= item.getBuildId(), "还没有对应的构建记录");

		BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
		buildHistoryLog.setBuildDataId(id);
		buildHistoryLog.setBuildNumberId(buildId);
		BuildHistoryLog queryByBean = dbBuildHistoryLogService.queryByBean(buildHistoryLog);
		Assert.notNull(queryByBean, "没有对应的构建历史");

		File file = BuildUtil.getLogFile(item.getId(), buildId);
		Assert.state(FileUtil.isFile(file), "日志文件错误");

		if (!file.exists()) {
			if (buildId == item.getBuildId()) {
				return JsonMessage.getString(201, "还没有日志文件");
			}
			return JsonMessage.getString(300, "日志文件不存在");
		}
		JSONObject data = new JSONObject();
		// 运行中
		Integer status = queryByBean.getStatus();
		data.put("run", status == BuildStatus.Ing.getCode() || status == BuildStatus.PubIng.getCode());
		// 构建中
		data.put("buildRun", status == BuildStatus.Ing.getCode());
		// 读取文件
		int linesInt = Convert.toInt(line, 1);
		LimitQueue<String> lines = new LimitQueue<>(500);
		final int[] readCount = {0};
		FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8, (LineHandler) line1 -> {
			readCount[0]++;
			if (readCount[0] < linesInt) {
				return;
			}
			lines.add(line1);
		});
		// 下次应该获取的行数
		data.put("line", readCount[0] + 1);
		data.put("getLine", linesInt);
		data.put("dataLines", lines);
		return JsonMessage.getString(200, "ok", data);
	}
}

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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSON;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.BackupInfoModel;
import io.jpom.model.enums.BackupStatusEnum;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.system.db.DbConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据库备份 controller
 *
 * @author Hotstrip
 * @date 2021-11-18
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM)
public class BackupInfoController extends BaseServerController {

	@Resource
	private BackupInfoService backupInfoService;

	/**
	 * 分页加载备份列表数据
	 *
	 * @param limit      每页条数
	 * @param page       页码
	 * @param name       备份名称
	 * @param backupType 备份类型{0: 全量, 1: 部分}
	 * @return json
	 */
	@PostMapping(value = "/system/backup/list")
	@Feature(method = MethodFeature.LOG)
	public Object loadBackupList(@ValidatorConfig(value = {@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")}, defaultVal = "10") int limit,
								 @ValidatorConfig(value = {@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")}, defaultVal = "1") int page,
								 String name, Integer backupType) {
		Page pageObj = new Page(page, limit);
		pageObj.addOrder(new Order("createTimeMillis", Direction.DESC));

		// 设置查询参数
		Entity entity = Entity.create();
		entity.setIgnoreNull("name", StrUtil.format(" like '{}%'", name));
		entity.setIgnoreNull("backupType", backupType);

		// 查询数据库
		PageResultDto<BackupInfoModel> pageResult = backupInfoService.listPage(entity, pageObj);

		return JsonMessage.getString(200, "获取成功", pageResult);
	}

	/**
	 * 删除备份数据
	 *
	 * @param id 备份 ID
	 * @return
	 */
	@PostMapping(value = "/system/backup/delete")
	@Feature(method = MethodFeature.DEL)
	@SystemPermission
	public Object deleteBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 根据 id 查询备份信息
		BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
		Objects.requireNonNull(backupInfoModel, "备份数据不存在");

		// 删除对应的文件
		FileUtil.del(backupInfoModel.getFilePath());

		// 删除备份信息
		backupInfoService.delByKey(id);
		return JsonMessage.toJson(200, "删除成功");
	}

	/**
	 * 还原备份数据
	 * 还原的时候不能异步了，只能等待备份还原成功或者失败
	 *
	 * @param id 备份 ID
	 * @return
	 */
	@PostMapping(value = "/system/backup/restore")
	@Feature(method = MethodFeature.EDIT)
	@SystemPermission
	public Object restoreBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 根据 id 查询备份信息
		BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
		Objects.requireNonNull(backupInfoModel, "备份数据不存在");

		// 检查备份文件是否存在
		File file = new File(backupInfoModel.getFilePath());
		if (!FileUtil.exist(file)) {
			return JsonMessage.toJson(400, "备份文件不存在");
		}

		// 还原备份文件
		boolean flag = backupInfoService.restoreWithSql(backupInfoModel.getFilePath());
		if (flag) {
			// 还原备份数据成功之后需要修改当前备份信息的状态（因为备份的时候该备份信息状态是备份中）
			backupInfoModel.setFileSize(FileUtil.size(file));
			backupInfoModel.setSha1Sum(SecureUtil.sha1(file));
			backupInfoModel.setStatus(BackupStatusEnum.SUCCESS.getCode());
			backupInfoService.update(backupInfoModel);
			return JsonMessage.toJson(200, "还原备份数据成功");
		}
		return JsonMessage.toJson(400, "还原备份数据失败");
	}

	/**
	 * 创建备份任务
	 *
	 * @param map 参数 map.tableNameList 选中备份的表名称
	 * @return
	 */
	@PostMapping(value = "/system/backup/create")
	@Feature(method = MethodFeature.EXECUTE)
	public Object backup(@RequestBody Map<String, Object> map) {
		List<String> tableNameList = JSON.parseArray(JSON.toJSONString(map.get("tableNameList")), String.class);
		backupInfoService.backupToSql(tableNameList);
		return JsonMessage.toJson(200, "操作成功，请稍后刷新查看备份状态");
	}

	/**
	 * 导入备份数据
	 *
	 * @return
	 */
	@PostMapping(value = "/system/backup/upload")
	@OptLog(UserOperateLogV1.OptType.UploadProjectFile)
	@Feature(method = MethodFeature.UPLOAD)
	@SystemPermission
	public Object uploadBackupFile() throws IOException {
		MultipartFileBuilder multipartFileBuilder = createMultipart()
				.addFieldName("file");
		// 备份类型
		int backupType = Integer.parseInt(getParameter("backupType"));
		// 存储目录
		File directory = FileUtil.file(DbConfig.getInstance().dbLocalPath(), Const.BACKUP_DIRECTORY_NAME);

		// 保存文件
		multipartFileBuilder.setSavePath(FileUtil.getAbsolutePath(directory))
				.setUseOriginalFilename(true);
		String backupSqlPath = multipartFileBuilder.save();

		// 记录到数据库
		final File file = new File(backupSqlPath);
		BackupInfoModel backupInfoModel = new BackupInfoModel();
		backupInfoModel.setId(IdUtil.fastSimpleUUID());
		backupInfoModel.setName(file.getName());
		backupInfoModel.setBackupType(backupType);
		backupInfoModel.setStatus(BackupStatusEnum.SUCCESS.getCode());
		backupInfoModel.setFileSize(FileUtil.size(file));
		backupInfoModel.setSha1Sum(SecureUtil.sha1(file));
		backupInfoModel.setFilePath(backupSqlPath);

		backupInfoService.insert(backupInfoModel);

		return JsonMessage.toJson(200, "操作成功，请稍后刷新查看备份状态");
	}

	/**
	 * 下载备份数据
	 *
	 * @param id 备份 ID
	 * @return
	 */
	@GetMapping(value = "/system/backup/download")
	@Feature(method = MethodFeature.DOWNLOAD)
	@SystemPermission
	public void downloadBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 根据 id 查询备份信息
		BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
		Objects.requireNonNull(backupInfoModel, "备份数据不存在");

		// 检查备份文件是否存在
		File file = new File(backupInfoModel.getFilePath());
		if (!FileUtil.exist(file)) {
			DefaultSystemLog.getLog().error("文件不存在，无法下载...backupId: {}", id);
			return;
		}

		// 下载文件
		ServletUtil.write(getResponse(), file);
	}

	/**
	 * 读取数据库表名称列表
	 *
	 * @return
	 */
	@PostMapping(value = "/system/backup/table-name-list")
	@Feature(method = MethodFeature.LIST)
	public Object loadTableNameList() {
		List<String> tableNameList = backupInfoService.h2TableNameList();
		return JsonMessage.toJson(200, "获取成功", tableNameList);
	}

}

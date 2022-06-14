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
package io.jpom.controller.system;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.BackupInfoModel;
import io.jpom.model.enums.BackupStatusEnum;
import io.jpom.model.enums.BackupTypeEnum;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.service.h2db.TableName;
import io.jpom.system.db.DbConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库备份 controller
 *
 * @author Hotstrip
 * @since 2021-11-18
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_BACKUP)
@SystemPermission
public class BackupInfoController extends BaseServerController {

	/**
	 * 存储数据库表名称和别名的变量
	 */
	private static Map<String, String> TABLE_NAME_MAP = new HashMap<>();

	private final BackupInfoService backupInfoService;

	public BackupInfoController(BackupInfoService backupInfoService) {
		this.backupInfoService = backupInfoService;
	}

	/**
	 * 分页加载备份列表数据
	 *
	 * @return json
	 */
	@PostMapping(value = "/system/backup/list")
	@Feature(method = MethodFeature.LIST)
	public Object loadBackupList() {
		// 查询数据库
		PageResultDto<BackupInfoModel> pageResult = backupInfoService.listPage(getRequest());

		return JsonMessage.getString(200, "获取成功", pageResult);
	}

	/**
	 * 删除备份数据
	 *
	 * @param id 备份 ID
	 * @return json
	 */
	@PostMapping(value = "/system/backup/delete")
	@Feature(method = MethodFeature.DEL)
	@SystemPermission(superUser = true)
	public Object deleteBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 删除备份信息
		backupInfoService.delByKey(id);
		return JsonMessage.toJson(200, "删除成功");
	}

	/**
	 * 还原备份数据
	 * 还原的时候不能异步了，只能等待备份还原成功或者失败
	 *
	 * @param id 备份 ID
	 * @return json
	 */
	@PostMapping(value = "/system/backup/restore")
	@Feature(method = MethodFeature.EXECUTE)
	public Object restoreBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 根据 id 查询备份信息
		BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
		Objects.requireNonNull(backupInfoModel, "备份数据不存在");

		// 检查备份文件是否存在
		File file = new File(backupInfoModel.getFilePath());
		if (!FileUtil.exist(file)) {
			return JsonMessage.toJson(400, "备份文件不存在");
		}
		// 清空 sql 加载记录
		DbConfig.getInstance().clearExecuteSqlLog();
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
	 * @return json
	 */
	@PostMapping(value = "/system/backup/create")
	@Feature(method = MethodFeature.EDIT)
	public Object backup(@RequestBody Map<String, Object> map) {
		List<String> tableNameList = JSON.parseArray(JSON.toJSONString(map.get("tableNameList")), String.class);
		backupInfoService.backupToSql(tableNameList);
		return JsonMessage.toJson(200, "操作成功，请稍后刷新查看备份状态");
	}

	/**
	 * 导入备份数据
	 *
	 * @return json
	 */
	@PostMapping(value = "/system/backup/upload")
	@Feature(method = MethodFeature.UPLOAD)
	@SystemPermission(superUser = true)
	public Object uploadBackupFile() throws IOException {
		MultipartFileBuilder multipartFileBuilder = createMultipart()
				.addFieldName("file");
		// 备份类型
		//int backupType = Integer.parseInt(getParameter("backupType"));
		// 存储目录
		File directory = FileUtil.file(DbConfig.getInstance().dbLocalPath(), Const.BACKUP_DIRECTORY_NAME);

		// 保存文件
		multipartFileBuilder.setSavePath(FileUtil.getAbsolutePath(directory))
				.setUseOriginalFilename(false);
		String backupSqlPath = multipartFileBuilder.save();
		// 记录到数据库
		final File file = new File(backupSqlPath);
		String sha1Sum = SecureUtil.sha1(file);
		BackupInfoModel backupInfoModel = new BackupInfoModel();
		backupInfoModel.setSha1Sum(sha1Sum);
		boolean exists = backupInfoService.exists(backupInfoModel);
		if (exists) {
			FileUtil.del(file);
			return JsonMessage.getString(400, "导入的数据已经存在啦");
		}

//		backupInfoModel.setId(IdUtil.fastSimpleUUID());
		backupInfoModel.setName(file.getName());
		backupInfoModel.setBackupType(BackupTypeEnum.IMPORT.getCode());
		backupInfoModel.setStatus(BackupStatusEnum.SUCCESS.getCode());
		backupInfoModel.setFileSize(FileUtil.size(file));

		backupInfoModel.setSha1Sum(sha1Sum);
		backupInfoModel.setFilePath(backupSqlPath);
		backupInfoService.insert(backupInfoModel);

		return JsonMessage.toJson(200, "导入成功");
	}

	/**
	 * 下载备份数据
	 *
	 * @param id 备份 ID
	 */
	@GetMapping(value = "/system/backup/download")
	@Feature(method = MethodFeature.DOWNLOAD)
	public void downloadBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 根据 id 查询备份信息
		BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
		Objects.requireNonNull(backupInfoModel, "备份数据不存在");

		// 检查备份文件是否存在
		File file = new File(backupInfoModel.getFilePath());
		if (!FileUtil.exist(file)) {
			//log.error("文件不存在，无法下载...backupId: {}", id);
			ServletUtil.write(getResponse(), JsonMessage.getString(404, "文件不存在，无法下载"), ContentType.JSON.toString());
			return;
		}

		// 下载文件
		ServletUtil.write(getResponse(), file);
	}

	/**
	 * 读取数据库表名称列表
	 *
	 * @return json
	 */
	@PostMapping(value = "/system/backup/table-name-list")
	@Feature(method = MethodFeature.LIST)
	public Object loadTableNameList() {
		// 从数据库加载表名称列表
		List<String> tableNameList = backupInfoService.h2TableNameList();
		// 扫描程序，拿到表名称和别名
		if (TABLE_NAME_MAP.isEmpty()) {
			Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("io.jpom", TableName.class);
			TABLE_NAME_MAP = CollStreamUtil.toMap(classes, aClass -> {
				TableName tableName = aClass.getAnnotation(TableName.class);
				return tableName.value();
			}, aClass -> {
				TableName tableName = aClass.getAnnotation(TableName.class);
				return tableName.name();
			});
		}
		List<JSONObject> list = tableNameList.stream().map(s -> {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("tableName", s);
			jsonObject.put("tableDesc", StrUtil.emptyToDefault(TABLE_NAME_MAP.get(s), s));
			return jsonObject;
		}).collect(Collectors.toList());
		return JsonMessage.toJson(200, "", list);
	}

}

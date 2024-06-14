/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.BackupInfoModel;
import org.dromara.jpom.model.enums.BackupStatusEnum;
import org.dromara.jpom.model.enums.BackupTypeEnum;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.dblog.BackupInfoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@ConditionalOnProperty(prefix = "jpom.db", name = "mode", havingValue = "H2", matchIfMissing = true)
@Slf4j
public class BackupInfoController extends BaseServerController {


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
    public Object loadBackupList(HttpServletRequest request) {
        // 查询数据库
        PageResultDto<BackupInfoModel> pageResult = backupInfoService.listPage(request);
        pageResult.each(backupInfoModel -> backupInfoModel.setFileExist(FileUtil.exist(backupInfoModel.getFilePath())));
        return JsonMessage.success(I18nMessageUtil.get("i18n.get_success.fb55"), pageResult);
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
    public IJsonMessage<String> deleteBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_cannot_be_empty.403b") String id) {
        // 删除备份信息
        backupInfoService.delByKey(id);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.delete_success.0007"));
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
    public IJsonMessage<String> restoreBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_cannot_be_empty.403b") String id) {
        // 根据 id 查询备份信息
        BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
        Objects.requireNonNull(backupInfoModel, I18nMessageUtil.get("i18n.backup_data_not_exist.f88c"));

        // 检查备份文件是否存在
        String filePath = backupInfoModel.getFilePath();
        File file = new File(filePath);
        if (!FileUtil.exist(file)) {
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.backup_file_not_exist.9628"));
        }
        // 清空 sql 加载记录
        StorageServiceFactory.clearExecuteSqlLog();
        // 还原备份文件
        boolean flag = backupInfoService.restoreWithSql(filePath);
        if (flag) {
            // 还原备份数据成功之后需要修改当前备份信息的状态（因为备份的时候该备份信息状态是备份中）
            this.fuzzyUpdate(SecureUtil.sha1(file));
            return new JsonMessage<>(200, I18nMessageUtil.get("i18n.restore_backup_data_success.253a"));
        }
        return new JsonMessage<>(400, I18nMessageUtil.get("i18n.restore_backup_data_failed.58af"));
    }

    /**
     * 模糊更新
     *
     * @param sha1 文件签名
     */
    private void fuzzyUpdate(String sha1) {
        BackupInfoModel where = new BackupInfoModel();
        where.setStatus(BackupStatusEnum.DEFAULT.getCode());
        List<BackupInfoModel> list = backupInfoService.listByBean(where);
        Optional.ofNullable(list).ifPresent(backupInfoModels -> {
            for (BackupInfoModel backupInfoModel : backupInfoModels) {
                String filePath = backupInfoModel.getFilePath();
                if (!FileUtil.exist(filePath)) {
                    continue;
                }
                File file = FileUtil.file(filePath);
                if (StrUtil.equals(SecureUtil.sha1(file), sha1)) {
                    // 是同一个文件
                    BackupInfoModel update = new BackupInfoModel();
                    update.setId(backupInfoModel.getId());
                    update.setFileSize(FileUtil.size(file));
                    update.setStatus(BackupStatusEnum.SUCCESS.getCode());
                    update.setSha1Sum(sha1);
                    int updateCount = backupInfoService.updateById(update);
                    log.debug(I18nMessageUtil.get("i18n.update_restore_data.1b0b"), updateCount);
                }
            }
        });
    }

    /**
     * 创建备份任务
     *
     * @param map 参数 map.tableNameList 选中备份的表名称
     * @return json
     */
    @PostMapping(value = "/system/backup/create")
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> backup(@RequestBody Map<String, Object> map) {
        List<String> tableNameList = JSON.parseArray(JSON.toJSONString(map.get("tableNameList")), String.class);
        backupInfoService.backupToSql(tableNameList);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded_refresh_backup.54a9"));
    }

    /**
     * 导入备份数据
     *
     * @return json
     */
    @PostMapping(value = "/system/backup/upload")
    @Feature(method = MethodFeature.UPLOAD)
    @SystemPermission(superUser = true)
    public IJsonMessage<String> uploadBackupFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.state(StrUtil.containsAnyIgnoreCase(extName, "sql"), I18nMessageUtil.get("i18n.file_type_not_supported2.d497") + extName);
        String saveFileName = UnicodeUtil.toUnicode(originalFilename);
        saveFileName = saveFileName.replace(StrUtil.BACKSLASH, "_");
        // 存储目录
        File directory = FileUtil.file(StorageServiceFactory.dbLocalPath(), DbExtConfig.BACKUP_DIRECTORY_NAME);
        // 生成唯一id
        String format = String.format("%s_%s", IdUtil.fastSimpleUUID(), saveFileName);
        format = StrUtil.maxLength(format, 40);
        File backupSqlFile = FileUtil.file(directory, format + "." + extName);
        FileUtil.mkParentDirs(backupSqlFile);
        file.transferTo(backupSqlFile);
        // 记录到数据库
        String sha1Sum = SecureUtil.sha1(backupSqlFile);
        BackupInfoModel backupInfoModel = new BackupInfoModel();
        backupInfoModel.setSha1Sum(sha1Sum);
        boolean exists = backupInfoService.exists(backupInfoModel);
        if (exists) {
            FileUtil.del(backupSqlFile);
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.data_already_exists.0397"));
        }

        backupInfoModel.setName(backupSqlFile.getName());
        backupInfoModel.setBackupType(BackupTypeEnum.IMPORT.getCode());
        backupInfoModel.setStatus(BackupStatusEnum.SUCCESS.getCode());
        backupInfoModel.setFileSize(FileUtil.size(backupSqlFile));

        backupInfoModel.setSha1Sum(sha1Sum);
        backupInfoModel.setFilePath(FileUtil.getAbsolutePath(backupSqlFile));
        backupInfoService.insert(backupInfoModel);

        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.import_success.b6d1"));
    }

    /**
     * 下载备份数据
     *
     * @param id 备份 ID
     */
    @GetMapping(value = "/system/backup/download")
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_cannot_be_empty.403b") String id, HttpServletResponse response) {
        // 根据 id 查询备份信息
        BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
        Objects.requireNonNull(backupInfoModel, I18nMessageUtil.get("i18n.backup_data_not_exist.f88c"));

        // 检查备份文件是否存在
        File file = new File(backupInfoModel.getFilePath());
        if (!FileUtil.exist(file)) {
            //log.error("文件不存在，无法下载...backupId: {}", id);
            ServletUtil.write(response, JsonMessage.getString(404, I18nMessageUtil.get("i18n.file_does_not_exist_for_download.8dd6")), ContentType.JSON.toString());
            return;
        }

        // 下载文件
        ServletUtil.write(response, file);
    }

    /**
     * 读取数据库表名称列表
     *
     * @return json
     */
    @PostMapping(value = "/system/backup/table-name-list")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> loadTableNameList() {
        // 从数据库加载表名称列表
        List<String> tableNameList = backupInfoService.h2TableNameList();
        // 扫描程序，拿到表名称和别名

        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("org.dromara.jpom", TableName.class);
        Map<String, String> TABLE_NAME_MAP = CollStreamUtil.toMap(classes, aClass -> {
            TableName tableName = aClass.getAnnotation(TableName.class);
            return tableName.value();
        }, aClass -> {
            TableName tableName = aClass.getAnnotation(TableName.class);
            return I18nMessageUtil.get(tableName.nameKey());
        });

        List<JSONObject> list = tableNameList.stream().map(s -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tableName", s);
            jsonObject.put("tableDesc", StrUtil.emptyToDefault(TABLE_NAME_MAP.get(s), s));
            return jsonObject;
        }).collect(Collectors.toList());
        return new JsonMessage<>(200, "", list);
    }

}

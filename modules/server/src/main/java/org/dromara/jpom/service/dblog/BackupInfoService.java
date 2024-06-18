/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.*;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.event.ISystemTask;
import cn.keepbx.jpom.plugins.IPlugin;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.model.data.BackupInfoModel;
import org.dromara.jpom.model.enums.BackupStatusEnum;
import org.dromara.jpom.model.enums.BackupTypeEnum;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 备份数据库 service
 *
 * @author Hotstrip
 * @since 2021-11-18
 **/
@Service
@Slf4j
public class BackupInfoService extends BaseDbService<BackupInfoModel> implements ISystemTask {

    private final DbExtConfig dbExtConfig;

    public BackupInfoService(DbExtConfig dbExtConfig) {
        this.dbExtConfig = dbExtConfig;
    }

    /**
     * 检查数据库备份
     */
    @Override
    public void executeTask() {
        if (dbExtConfig.getMode() != DbExtConfig.Mode.H2) {
            return;
        }
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            // 创建备份
            this.createAutoBackup();
            // 删除历史备份
            this.deleteAutoBackup();
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    /**
     * 删除历史 自动备份信息
     */
    private void deleteAutoBackup() {
        Integer autoBackupReserveDay = dbExtConfig.getAutoBackupReserveDay();
        if (autoBackupReserveDay != null && autoBackupReserveDay > 0) {
            //
            Entity entity = Entity.create();
            entity.set("backupType", 3);
            entity.set("createTimeMillis", " < " + (SystemClock.now() - TimeUnit.DAYS.toMillis(autoBackupReserveDay)));
            List<Entity> entities = super.queryList(entity);
            if (entities != null) {
                for (Entity entity1 : entities) {
                    String id = entity1.getStr("id");
                    this.delByKey(id);
                }
            }
        }
    }

    /**
     * 创建自动备份数据
     */
    private void createAutoBackup() {
        // 自动备份
        Integer autoBackupIntervalDay = dbExtConfig.getAutoBackupIntervalDay();
        if (autoBackupIntervalDay != null && autoBackupIntervalDay > 0) {
            BackupInfoModel backupInfoModel = new BackupInfoModel();
            backupInfoModel.setBackupType(3);
            List<BackupInfoModel> infoModels = super.queryList(backupInfoModel, 1, new Order("createTimeMillis", Direction.DESC));
            BackupInfoModel first = CollUtil.getFirst(infoModels);
            if (first != null) {
                Long createTimeMillis = first.getCreateTimeMillis();
                long interval = SystemClock.now() - createTimeMillis;
                if (interval < TimeUnit.DAYS.toMillis(autoBackupIntervalDay)) {
                    return;
                }
            }
            this.autoBackup();
        }
    }

    /**
     * 自动备份
     */
    public Future<BackupInfoModel> autoBackup() {
        if (dbExtConfig.getMode() != DbExtConfig.Mode.H2) {
            return null;
        }
        // 执行数据库备份
        return this.backupToSql(null, BackupTypeEnum.AUTO);
    }

    /**
     * 备份数据库 SQL 文件
     *
     * @param tableNameList 需要备份的表名称列表，如果是全库备份，则不需要
     */
    public Future<BackupInfoModel> backupToSql(final List<String> tableNameList) {
        // 判断备份类型
        BackupTypeEnum backupType = BackupTypeEnum.ALL;
        if (!CollectionUtils.isEmpty(tableNameList)) {
            backupType = BackupTypeEnum.PART;
        }
        return this.backupToSql(tableNameList, backupType);
    }

    /**
     * 备份数据库 SQL 文件
     *
     * @param tableNameList 需要备份的表名称列表，如果是全库备份，则不需要
     */
    private Future<BackupInfoModel> backupToSql(final List<String> tableNameList, BackupTypeEnum backupType) {
        final String fileName = LocalDateTimeUtil.format(LocalDateTimeUtil.now(), DatePattern.PURE_DATETIME_PATTERN);

        // 设置默认备份 SQL 的文件地址
        File file = FileUtil.file(StorageServiceFactory.dbLocalPath(), DbExtConfig.BACKUP_DIRECTORY_NAME, fileName + DbExtConfig.SQL_FILE_SUFFIX);
        final String backupSqlPath = FileUtil.getAbsolutePath(file);

        // 数据源参数
        final String url = StorageServiceFactory.get().dbUrl();

        final String user = dbExtConfig.userName();
        final String pass = dbExtConfig.userPwd();

        JpomManifest instance = JpomManifest.getInstance();
        // 先构造备份信息插入数据库
        BackupInfoModel backupInfoModel = new BackupInfoModel();
        String timeStamp = instance.getTimeStamp();
        try {
            DateTime parse = DateUtil.parse(timeStamp);
            backupInfoModel.setBaleTimeStamp(parse.getTime());
        } catch (Exception ignored) {
        }
        backupInfoModel.setName(fileName);
        backupInfoModel.setVersion(instance.getVersion());
        backupInfoModel.setBackupType(backupType.getCode());
        backupInfoModel.setFilePath(backupSqlPath);
        this.insert(backupInfoModel);
        // 开启一个子线程去执行任务，任务完成之后修改对应的数据库备份信息
        return I18nThreadUtil.execAsync(() -> {
            // 修改用的实体类
            BackupInfoModel backupInfo = new BackupInfoModel();
            backupInfo.setId(backupInfoModel.getId());
            try {
                log.debug("启动一个新线程来执行 H2 数据库备份...启动");
                StorageServiceFactory.get().backupSql(url, user, pass, backupSqlPath, tableNameList);
                // 修改备份任务执行完成
                backupInfo.setFileSize(FileUtil.size(file));
                backupInfo.setSha1Sum(SecureUtil.sha1(file));
                backupInfo.setStatus(BackupStatusEnum.SUCCESS.getCode());
                this.updateById(backupInfo);
                log.debug("启动一个新线程来执行 H2 数据库备份...成功");
            } catch (Exception e) {
                // 记录错误日志信息，修改备份任务执行失败
                log.error("备份 h2 数据库异常", e);
                backupInfo.setStatus(BackupStatusEnum.FAILED.getCode());
                this.updateById(backupInfo);
            }
            return backupInfo;
        });
    }

    /**
     * 根据 SQL 文件还原数据库
     * 还原数据库时只能同步，防止该过程中修改数据造成数据不一致
     *
     * @param backupSqlPath 备份 sql 文件地址
     */
    public boolean restoreWithSql(String backupSqlPath) {
        try {
            long startTs = System.currentTimeMillis();
            IPlugin plugin = PluginFactory.getPlugin("db-h2");
            Map<String, Object> map = new HashMap<>(10);
            map.put("backupSqlPath", backupSqlPath);
            plugin.execute("restoreBackupSql", map);
            // h2BackupService.restoreBackupSql(backupSqlPath);
            long endTs = System.currentTimeMillis();
            log.debug("restore H2 Database backup...success...cast {} ms", endTs - startTs);
            return true;
        } catch (Exception e) {
            // 记录错误日志信息，返回数据库备份还原执行失败
            log.error("restore H2 Database backup...catch exception...message: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * load table name list from h2 database
     *
     * @return list
     */
    public List<String> h2TableNameList() {
        String sql = "show tables;";
        List<Entity> list = super.query(sql);
        // 筛选字段
        return list.stream()
            .filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(ServerConst.TABLE_NAME))))
            .flatMap(entity -> Stream.of(String.valueOf(entity.get(ServerConst.TABLE_NAME))))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public int delByKey(String keyValue) {
        // 根据 id 查询备份信息
        BackupInfoModel backupInfoModel = super.getByKey(keyValue);
        Objects.requireNonNull(backupInfoModel, I18nMessageUtil.get("i18n.backup_data_not_exist.f88c"));

        // 删除对应的文件
        boolean del = FileUtil.del(backupInfoModel.getFilePath());
        Assert.state(del, I18nMessageUtil.get("i18n.delete_backup_data_file_failure.2ebf"));

        // 删除备份信息
        return super.delByKey(keyValue);
    }
}

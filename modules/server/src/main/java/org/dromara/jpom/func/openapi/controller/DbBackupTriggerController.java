package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.BackupInfoModel;
import org.dromara.jpom.model.enums.BackupTypeEnum;
import org.dromara.jpom.service.dblog.BackupInfoService;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Future;

/**
 * @author bwcx_jzy
 * @since 2024/9/29
 */
@RestController
@NotLogin
@Slf4j
public class DbBackupTriggerController {

    private final BackupInfoService backupInfoService;
    private final SystemParametersServer systemParametersServer;

    public DbBackupTriggerController(BackupInfoService backupInfoService,
                                     SystemParametersServer systemParametersServer) {
        this.backupInfoService = backupInfoService;
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 备份数据触发器
     *
     * @param token        token
     * @param reserveCount 保留份数
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.BACKUP_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> trigger(@PathVariable String token,
                                        HttpServletRequest request,
                                        String reserveCount) {
        String configToken = systemParametersServer.getConfig("backup-db-token", String.class);
        Assert.state(StrUtil.equals(configToken, token), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        //
        Future<BackupInfoModel> future = backupInfoService.triggerBackup();
        Assert.notNull(future, I18nMessageUtil.get("i18n.database_auto_backup_support.7b8f"));
        //
        int reserveCountInt = Convert.toInt(reserveCount, 0);
        if (reserveCountInt > 0) {
            while (true) {
                Entity entity = Entity.create();
                entity.set("backupType", BackupTypeEnum.TRIGGER.getCode());
                Page page = new Page(2, reserveCountInt);
                page.addOrder(new Order("createTimeMillis", Direction.DESC));

                try {
                    PageResultDto<BackupInfoModel> pageResultDto = backupInfoService.listPage(entity, page);
                    if (pageResultDto.isEmpty()) {
                        break;
                    }
                    pageResultDto.each(backupInfoModel -> backupInfoService.delByKey(backupInfoModel.getId()));
                } catch (Exception e) {
                    if (StrUtil.equals(e.getMessage(), I18nMessageUtil.get("i18n.pagination_error.6759"))) {
                        // 没有任何数据
                        break;
                    } else {
                        throw Lombok.sneakyThrow(e);
                    }
                }
            }
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.trigger_success.f9d1"));
    }
}

/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.func.files.model.FileStorageModel;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/17
 */
@RestController
@NotLogin
@Slf4j
public class FileStorageApiController extends BaseDownloadApiController {

    /**
     * 允许作为排序/过滤字段的字段名：字母开头，仅字母、数字、下划线
     * <p>
     * 排序字段会被 Hutool 直接拼接到 ORDER BY，必须先通过字符白名单校验，避免 SQL 注入。
     */
    private static final Pattern SAFE_FIELD_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /**
     * 不允许通过 filter_* 参数传入的字段（这些条件由接口自身强制设置）
     */
    private static final Set<String> FILTER_IGNORE_FIELDS = CollUtil.newHashSet("aliasCode", "triggerToken");

    private final TriggerTokenLogServer triggerTokenLogServer;
    private final FileStorageService fileStorageService;
    private final ServerConfig serverConfig;

    public FileStorageApiController(TriggerTokenLogServer triggerTokenLogServer,
                                    FileStorageService fileStorageService,
                                    ServerConfig serverConfig) {
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.fileStorageService = fileStorageService;
        this.serverConfig = serverConfig;
    }


    /**
     * 解析别名参数
     *
     * @param id      别名
     * @param token   token
     * @param sort    排序
     * @param request 请求
     * @return 数据
     */
    private FileStorageModel queryByAliasCode(String id, String token, String sort, HttpServletRequest request) {
        // 先验证 token 和 id 是否都存在（查询值已由底层 Condition 绑定，不会被解析为 SQL 条件）
        {
            FileStorageModel data = new FileStorageModel();
            data.setAliasCode(id);
            data.setTriggerToken(token);
            Assert.state(fileStorageService.exists(data), I18nMessageUtil.get("i18n.alias_or_token_error.d5c6"));
        }
        // 排序字段白名单校验，非法字段直接忽略（sort 会被拼接到 ORDER BY，不能信任请求值）
        List<Order> orders = parseOrders(sort);
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        FileStorageModel where = new FileStorageModel();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (!StrUtil.startWith(key, "filter_")) {
                continue;
            }
            String fieldName = StrUtil.removePrefix(key, "filter_");
            if (!isSafeModelField(fieldName) || FILTER_IGNORE_FIELDS.contains(fieldName)) {
                log.warn("ignore invalid filter field: {}", key);
                continue;
            }
            Field field = ReflectUtil.getField(FileStorageModel.class, fieldName);
            try {
                Object value = Convert.convert(field.getType(), entry.getValue());
                ReflectUtil.setFieldValue(where, fieldName, value);
            } catch (Exception e) {
                log.warn("ignore invalid filter value: {}={}", key, entry.getValue());
            }
        }
        // 强制使用接口自身的别名和 token 条件（避免被 filter_* 参数覆盖/绕过）
        where.setAliasCode(id);
        where.setTriggerToken(token);
        List<FileStorageModel> fileStorageModels = fileStorageService.queryList(where, 1, orders.toArray(new Order[]{}));
        FileStorageModel storageModel = CollUtil.getFirst(fileStorageModels);
        // 按别名查询可能命中同别名的多条记录，必须再次确认记录绑定的 token 与请求一致
        Assert.state(storageModel != null && StrUtil.equals(token, storageModel.getTriggerToken()),
            I18nMessageUtil.get("i18n.invalid_or_expired_token.bc43"));
        return storageModel;
    }

    /**
     * 解析排序参数（字段白名单校验，非法字段忽略）
     * <p>
     * 参数格式：{@code field:direction,field:direction}，例如 {@code createTimeMillis:desc}。
     *
     * @param sort 排序参数
     * @return 排序列表
     */
    static List<Order> parseOrders(String sort) {
        return Opt.ofBlankAble(sort)
            .map(s -> StrUtil.splitTrim(s, StrUtil.COMMA))
            .map(strings -> strings.stream()
                .map(FileStorageApiController::parseOrder)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    private static Order parseOrder(String item) {
        List<String> list = StrUtil.splitTrim(item, StrUtil.COLON);
        String field = CollUtil.getFirst(list);
        if (!isSafeModelField(field)) {
            log.warn("ignore invalid sort field: {}", item);
            return null;
        }
        Order order = new Order(field);
        String direction = CollUtil.get(list, 1);
        Direction orderDirection = null;
        try {
            orderDirection = Direction.fromString(direction);
        } catch (IllegalArgumentException ignored) {
            // 非法排序方向按默认降序处理，不把异常抛给调用方
        }
        order.setDirection(ObjectUtil.defaultIfNull(orderDirection, Direction.DESC));
        return order;
    }

    /**
     * 是否为 {@link FileStorageModel} 中可排序/可过滤的数据库字段
     * <p>
     * 仅允许字母数字下划线，且必须是模型中非 static、未标注 {@link PropIgnore} 的字段。
     *
     * @param field 字段名
     * @return true 合法
     */
    static boolean isSafeModelField(String field) {
        if (StrUtil.isEmpty(field) || !SAFE_FIELD_NAME.matcher(field).matches()) {
            return false;
        }
        Field fieldObj = ReflectUtil.getField(FileStorageModel.class, field);
        if (fieldObj == null || Modifier.isStatic(fieldObj.getModifiers())) {
            return false;
        }
        return fieldObj.getAnnotation(PropIgnore.class) == null;
    }

    @GetMapping(value = ServerOpenApi.FILE_STORAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(@PathVariable String id,
                         @PathVariable String token,
                         String sort,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        // 触发器 token 只允许字母数字（新版 32 位十六进制，兼容历史 10 位），
        // 提前校验可避免 "is not null" 一类字符串进入 Hutool 查询条件
        Assert.state(TriggerTokenLogServer.isValidToken(token), I18nMessageUtil.get("i18n.invalid_or_expired_token.bc43"));
        FileStorageModel storageModel = fileStorageService.getByKey(id);
        if (storageModel == null) {
            // 根据别名查询
            storageModel = this.queryByAliasCode(id, token, sort, request);
            Assert.notNull(storageModel, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        }
        // 无论主键还是别名分支，都校验记录绑定的 token 与请求一致
        Assert.state(StrUtil.equals(token, storageModel.getTriggerToken()), I18nMessageUtil.get("i18n.invalid_or_expired_token.bc43"));
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, fileStorageService.typeName());
        //
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.token_invalid_or_expired.cb96"));
        //
        File storageSavePath = serverConfig.fileStorageSavePath();
        File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
        // 需要考虑文件名中存在非法字符
        String name = FileUtils.safeFileName(storageModel.getName(), storageModel.getExtName(), fileStorageFile.getName());
        //    解析断点续传相关信息
        long fileSize = FileUtil.size(fileStorageFile);
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        this.download(fileStorageFile, fileSize, name, resolveRange, response);
    }
}

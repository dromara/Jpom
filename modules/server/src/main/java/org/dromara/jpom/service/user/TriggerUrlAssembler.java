/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.user;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseIdModel;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.UrlRedirectUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.service.ITriggerToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 触发器地址统一收口点
 * <p>
 * 全站「生成触发器地址」入口（构建、节点项目、节点脚本、脚本、SSH 命令、SSH 项目、环境变量、文件存储、
 * 静态文件存储、系统备份）都只经过本类的三个方法，禁止入口自己判空、自己调 {@code restToken}、
 * 自己拼 contextPath：
 * </p>
 * <ul>
 *     <li>{@link #require(Supplier, String)}：统一「查不到 → 405」，不再有 500 空指针；</li>
 *     <li>{@link #issue}：统一 token 首次生成/重置 + 审计落库（{@code TRIGGER_TOKEN_LOG}）+ 地址装配；</li>
 *     <li>{@link #issueGlobal}：没有实体 id 的全局 token（系统备份）走同一段逻辑。</li>
 * </ul>
 * <p>
 * 新增一种触发器：实现 {@link ITriggerToken}（含 {@code updateTriggerToken}）→ 控制器调 {@code issue} 一行，
 * 其余零改动。历史教训见 B-041（同类缺陷在兄弟入口重复出现三次）。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class TriggerUrlAssembler {

    private final TriggerTokenLogServer triggerTokenLogServer;

    public TriggerUrlAssembler(TriggerTokenLogServer triggerTokenLogServer) {
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 统一取数：查不到一律 405（业务异常），不再把 null 交给调用方
     *
     * @param loader          取数（内部可先做归属/隐私等业务校验）
     * @param notFoundMessage 不存在时的提示（i18n）
     * @param <T>             实体类型
     * @return 非 null 实体
     */
    public <T extends BaseIdModel> T require(Supplier<T> loader, String notFoundMessage) {
        T item = loader.get();
        Assert.notNull(item, notFoundMessage);
        return item;
    }

    /**
     * 发放 token 并装配地址（实体类触发器）
     *
     * @param service   数据服务（typeName 用于审计落库与类型登记）
     * @param item      数据实体（非 null，由 {@link #require} 取得）
     * @param rest      非空表示重置
     * @param userId    操作人（审计表记录）
     * @param urls      该类型的地址模板集合
     * @param request   请求（取代理 contextPath）
     * @return 响应体
     */
    public Map<String, String> issue(ITriggerToken service, String dataId, String oldToken, String rest, String userId,
                                     TriggerUrls urls, HttpServletRequest request) {
        return this.issue(service, dataId, oldToken, rest, userId, urls, request, null);
    }

    /**
     * 发放 token 并装配地址（带别名地址的场景，如文件存储）
     *
     * @param service    数据服务
     * @param item       数据实体
     * @param rest       非空表示重置
     * @param userId     操作人
     * @param urls       地址模板集合
     * @param request    请求
     * @param aliasValue 别名（可空；为空时 alias 类地址不输出）
     * @return 响应体
     */
    public Map<String, String> issue(ITriggerToken service, String dataId, String oldToken, String rest, String userId,
                                     TriggerUrls urls, HttpServletRequest request, String aliasValue) {
        String token = oldToken;
        if (StrUtil.isEmpty(token) || StrUtil.isNotEmpty(rest)) {
            token = triggerTokenLogServer.restToken(token, service.typeName(), dataId, userId);
            service.updateTriggerToken(dataId, token);
        }
        return this.assemble(dataId, token, aliasValue, urls, request);
    }

    /**
     * 发放 token 并装配地址（没有实体 id 的全局 token，如系统备份）
     *
     * @param oldToken 旧 token
     * @param rest     非空表示重置
     * @param saver    新 token 落库
     * @param urls     地址模板集合
     * @param request  请求
     * @return 响应体
     */
    public Map<String, String> issueGlobal(String oldToken, String rest, Consumer<String> saver,
                                           TriggerUrls urls, HttpServletRequest request) {
        String token = oldToken;
        if (StrUtil.isEmpty(token) || StrUtil.isNotEmpty(rest)) {
            token = IdUtil.fastUUID();
            saver.accept(token);
        }
        return this.assemble(null, token, null, urls, request);
    }

    /**
     * 统一的成功文案：普通查询回 ok，重置回「重置成功」
     *
     * @param rest 非空表示重置
     * @return 文案
     */
    public static String successMessage(String rest) {
        return StrUtil.isEmpty(rest) ? "ok" : I18nMessageUtil.get("i18n.reset_success.faa3");
    }

    private Map<String, String> assemble(String dataId, String token, String aliasValue, TriggerUrls urls, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        Map<String, String> map = new LinkedHashMap<>(10);
        for (TriggerUrls.Entry entry : urls.entries()) {
            String idValue = entry.isAlias() ? aliasValue : dataId;
            if (entry.isAlias() && StrUtil.isEmpty(idValue)) {
                continue;
            }
            String url = entry.getTemplate().
                replace("{id}", StrUtil.nullToEmpty(idValue)).
                replace("{token}", StrUtil.nullToEmpty(token));
            map.put(entry.getResponseKey(), FileUtil.normalize(String.format("/%s/%s", contextPath, url)));
        }
        if (urls.includeDataId()) {
            map.put("id", dataId);
        }
        map.put("token", token);
        return map;
    }
}

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
package io.jpom.common;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.data.NodeModel;
import io.jpom.model.user.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.util.StringUtil;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Jpom server 端
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
public abstract class BaseServerController extends BaseJpomController {
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();
    public static final Cache<String, String> SHARDING_IDS = CacheUtil.newLRUCache(10, TimeUnit.DAYS.toMillis(1));
    public static final String NODE_ID = "nodeId";

    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        NodeModel nodeModel = tryGetNode();
        Assert.notNull(nodeModel, "节点信息不正确,对应对节点不存在");
        return nodeModel;
    }

    protected NodeModel tryGetNode() {
        String nodeId = getParameter(NODE_ID);
        if (StrUtil.isEmpty(nodeId)) {
            return null;
        }
        return nodeService.getByKey(nodeId);
    }

    /**
     * 验证 cron 表达式, demo 账号不能开启 cron
     *
     * @param cron cron
     * @return 原样返回
     */
    protected String checkCron(String cron) {
        return StringUtil.checkCron(cron, s -> {
            UserModel user = getUser();
            Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
            return s;
        });
    }

    /**
     * 为线程设置 用户
     *
     * @param userModel 用户
     */
    public static void resetInfo(UserModel userModel) {
        UserModel userModel1 = USER_MODEL_THREAD_LOCAL.get();
        if (userModel1 != null && userModel == UserModel.EMPTY) {
            // 已经存在，更新为 empty 、跳过
            return;
        }
        USER_MODEL_THREAD_LOCAL.set(userModel);
    }

    protected UserModel getUser() {
        UserModel userByThreadLocal = getUserByThreadLocal();
        Assert.notNull(userByThreadLocal, ServerConst.AUTHORIZE_TIME_OUT_CODE + StrUtil.EMPTY);
        return userByThreadLocal;
    }

    /**
     * 从线程 缓存中获取 用户信息
     *
     * @return 用户
     */
    public static UserModel getUserByThreadLocal() {
        return Optional.ofNullable(USER_MODEL_THREAD_LOCAL.get()).orElseGet(BaseServerController::getUserModel);
//		return ;
    }

    public static void removeAll() {
        USER_MODEL_THREAD_LOCAL.remove();
    }

    /**
     * 只清理 是 empty 对象
     */
    public static void removeEmpty() {
        UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
        if (userModel == UserModel.EMPTY) {
            USER_MODEL_THREAD_LOCAL.remove();
        }
    }

    public static UserModel getUserModel() {
        ServletRequestAttributes servletRequestAttributes = tryGetRequestAttributes();
        if (servletRequestAttributes == null) {
            return null;
        }
        return (UserModel) servletRequestAttributes.getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    @Override
    public void uploadSharding(MultipartFile file, String tempPath, String sliceId, Integer totalSlice, Integer nowSlice, String fileSumMd5, String... extNames) throws IOException {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        super.uploadSharding(file, tempPath, sliceId, totalSlice, nowSlice, fileSumMd5, extNames);
    }

    @Override
    public File shardingTryMerge(String tempPath, String sliceId, Integer totalSlice, String fileSumMd5) throws IOException {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        try {
            return super.shardingTryMerge(tempPath, sliceId, totalSlice, fileSumMd5);
        } finally {
            // 判断-删除分片id
            BaseServerController.SHARDING_IDS.remove(sliceId);
        }
    }
}

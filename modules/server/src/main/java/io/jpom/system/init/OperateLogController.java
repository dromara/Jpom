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
package io.jpom.system.init;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.request.XssFilter;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.dblog.DbUserOperateLogService;
import io.jpom.system.AopLogInterface;
import io.jpom.system.WebAopLog;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 操作记录控制器
 *
 * @author jiangzeyin
 * @since 2019/4/19
 */
@PreLoadClass
@Slf4j
public class OperateLogController implements AopLogInterface {
    private static final ThreadLocal<CacheInfo> CACHE_INFO_THREAD_LOCAL = new ThreadLocal<>();

    private DbUserOperateLogService dbUserOperateLogService;

    @PreLoadMethod
    private static void init() {
        WebAopLog.setAopLogInterface(SpringUtil.getBean(OperateLogController.class));
    }

    private CacheInfo createCacheInfo(Method method) {
        Feature feature = method.getAnnotation(Feature.class);
        if (feature == null) {
            return null;
        }
        Class<?> declaringClass = method.getDeclaringClass();
        MethodFeature methodFeature = feature.method();
        if (methodFeature == MethodFeature.NULL) {
            log.error("权限分发配置错误：{}  {}", declaringClass, method.getName());
            return null;
        }
        ClassFeature classFeature = feature.cls();
        if (classFeature == ClassFeature.NULL) {
            Feature feature1 = declaringClass.getAnnotation(Feature.class);
            if (feature1 == null || feature1.cls() == ClassFeature.NULL) {
                log.error("权限分发配置错误：{}  {} class not find", declaringClass, method.getName());
                return null;
            }
            classFeature = feature1.cls();
        }
        CacheInfo cacheInfo = new CacheInfo();
        cacheInfo.setClassFeature(classFeature);
        cacheInfo.setMethodFeature(methodFeature);
        cacheInfo.setOptTime(SystemClock.now());
        cacheInfo.setResultCode(feature.resultCode());
        cacheInfo.setLogResponse(feature.logResponse());
        //
        if (dbUserOperateLogService == null) {
            dbUserOperateLogService = SpringUtil.getBean(DbUserOperateLogService.class);
        }
        return cacheInfo;
    }

    @Override
    public void before(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            Method method = methodSignature.getMethod();
            CacheInfo cacheInfo = this.createCacheInfo(method);
            if (cacheInfo == null) {
                return;
            }
            //
            ServletRequestAttributes servletRequestAttributes = BaseServerController.getRequestAttributes();
            HttpServletRequest request = servletRequestAttributes.getRequest();
            // 获取ip地址
            cacheInfo.ip = ServletUtil.getClientIP(request);
            // 获取节点
            cacheInfo.nodeModel = (NodeModel) request.getAttribute("node");
            //
            cacheInfo.userAgent = ServletUtil.getHeaderIgnoreCase(request, HttpHeaders.USER_AGENT);
            cacheInfo.workspaceId = ServletUtil.getHeaderIgnoreCase(request, Const.WORKSPACEID_REQ_HEADER);
            //
            Map<String, Object> allData = this.buildRequestParam(request);
            //
            cacheInfo.dataId = StrUtil.toStringOrNull(allData.get("id"));
            allData.put("request_url", request.getRequestURI());
            //
            cacheInfo.reqData = JSONObject.toJSONString(allData);
            //
            if (cacheInfo.methodFeature == MethodFeature.DEL) {
                // 删除数据 提前查询出操作到数据相关信息
                cacheInfo.optDataNameMap = dbUserOperateLogService.buildDataMsg(cacheInfo.classFeature, cacheInfo.dataId, cacheInfo.nodeModel == null ? null : cacheInfo.nodeModel.getId());
            }
            CACHE_INFO_THREAD_LOCAL.set(cacheInfo);
        }
    }

    private Map<String, Object> buildRequestParam(HttpServletRequest request) {
        Map<String, String> map = ServletUtil.getParamMap(request);
        // 过滤密码字段
        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            if (StrUtil.containsAnyIgnoreCase(key, XssFilter.logFilterPar)) {
                entry.setValue("***");
            }
        }
        //
        Map<String, Object> allData = new HashMap<>(30);
        String body = ServletFileUpload.isMultipartContent(request) ? null : ServletUtil.getBody(request);
        if (StrUtil.isNotEmpty(body)) {
            JSONValidator jsonValidator = JSONValidator.from(body);
            JSONValidator.Type type = jsonValidator.getType();
            if (type == null || type == JSONValidator.Type.Value) {
                allData.put("bodyData", body);
            } else if (type == JSONValidator.Type.Object) {
                JSONObject jsonObject = JSONObject.parseObject(body);
                allData.putAll(jsonObject);
                //
            } else if (type == JSONValidator.Type.Array) {
                allData.put("bodyData", JSONObject.toJSON(body));
            }
        }
        allData.putAll(map);
        return allData;
    }

    @Override
    public void afterReturning(Object value) {
        try {
            CacheInfo cacheInfo = CACHE_INFO_THREAD_LOCAL.get();
            if (cacheInfo == null || cacheInfo.methodFeature == MethodFeature.LIST) {
                return;
            }
            if (cacheInfo.classFeature == null || cacheInfo.methodFeature == null) {
                new RuntimeException("权限功能没有配置正确").printStackTrace();
                return;
            }
            UserModel userModel = BaseServerController.getUserByThreadLocal();
            userModel = userModel == null ? BaseServerController.getUserModel() : userModel;
            // 没有对应的用户
            if (userModel == null) {
                return;
            }
            this.log(userModel, value, cacheInfo);
        } finally {
            CACHE_INFO_THREAD_LOCAL.remove();
        }
    }

    /**
     * 记录操作日志
     *
     * @param userModel 用户
     * @param value     返回执行
     * @param cacheInfo 请求信息
     */
    public void log(UserModel userModel, Object value, CacheInfo cacheInfo) {
        UserOperateLogV1 userOperateLogV1 = new UserOperateLogV1();
        userOperateLogV1.setWorkspaceId(cacheInfo.workspaceId);
        userOperateLogV1.setClassFeature(cacheInfo.classFeature.name());
        userOperateLogV1.setMethodFeature(cacheInfo.methodFeature.name());
        userOperateLogV1.setDataId(cacheInfo.dataId);
        userOperateLogV1.setUserId(userModel.getId());
        userOperateLogV1.setIp(cacheInfo.ip);
        userOperateLogV1.setUserAgent(cacheInfo.userAgent);
        userOperateLogV1.setReqData(cacheInfo.reqData);
        userOperateLogV1.setOptTime(ObjectUtil.defaultIfNull(cacheInfo.optTime, SystemClock.now()));
        if (value != null) {
            // 解析结果
            if (value instanceof Throwable) {
                // 发生异常
                Throwable throwable = (Throwable) value;
                userOperateLogV1.setResultMsg(ExceptionUtil.stacktraceToString(throwable));
                userOperateLogV1.setOptStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            } else {
                String json = value.toString();
                userOperateLogV1.setResultMsg(json);
                try {
                    JsonMessage<?> jsonMessage = JSONObject.parseObject(json, JsonMessage.class);
                    int code = jsonMessage.getCode();
                    int[] resultCode = cacheInfo.getResultCode();
                    if (ArrayUtil.isNotEmpty(resultCode) && !ArrayUtil.contains(resultCode, code)) {
                        // 忽略
                        return;
                    }
                    userOperateLogV1.setOptStatus(code);
                } catch (Exception ignored) {
                }
            }
            // 判断是否记录响应日志
            Boolean logResponse = cacheInfo.getLogResponse();
            if (logResponse != null && !logResponse) {
                userOperateLogV1.setResultMsg(new cn.hutool.json.JSONObject().putOpt("hide", "*****").toString());
            }
        }
        //
        if (cacheInfo.nodeModel != null) {
            userOperateLogV1.setNodeId(cacheInfo.nodeModel.getId());
            if (StrUtil.isEmpty(cacheInfo.workspaceId)) {
                userOperateLogV1.setWorkspaceId(cacheInfo.nodeModel.getWorkspaceId());
            }
        }
        //
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            dbUserOperateLogService.insert(userOperateLogV1, cacheInfo);
        } finally {
            BaseServerController.removeEmpty();
        }
    }


    /**
     * 修改执行结果
     *
     * @param reqId 请求id
     * @param val   结果
     */
    public void updateLog(String reqId, String val) {
        DbUserOperateLogService dbUserOperateLogService = SpringUtil.getBean(DbUserOperateLogService.class);

        Entity entity = new Entity();
        entity.set("resultMsg", val);
        try {
            JsonMessage<?> jsonMessage = JSONObject.parseObject(val, JsonMessage.class);
            entity.set("optStatus", jsonMessage.getCode());
        } catch (Exception ignored) {
        }
        //
        Entity where = new Entity();
        where.set("reqId", reqId);
        dbUserOperateLogService.update(entity, where);
    }

    /**
     * 临时缓存
     */
    @Data
    public static class CacheInfo {
        private Long optTime;
        private String workspaceId;
        private ClassFeature classFeature;
        private MethodFeature methodFeature;
        private String ip;
        private NodeModel nodeModel;
        private String dataId;
        private String userAgent;
        private String reqData;
        private int[] resultCode;
        private Boolean logResponse;
        /**
         * 操作到数据到名称相关 map
         */
        private Map<String, Object> optDataNameMap;
    }
}

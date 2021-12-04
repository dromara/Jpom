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
package io.jpom.permission;

import io.jpom.common.BaseServerController;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.NodeOld1Service;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.manage.ProjectInfoService;
import io.jpom.service.node.script.ScriptServer;
import io.jpom.service.node.tomcat.TomcatService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态数据权限
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public class DynamicData {

    private static final Map<ClassFeature, DynamicData> DYNAMIC_DATA_MAP = new HashMap<>();

    /**
     * 二级数据
     */
    private static final Map<ClassFeature, Set<ClassFeature>> PARENT = new HashMap<>();

    static {
        // 节点
        put(ClassFeature.NODE, new DynamicData(NodeOld1Service.class, "id", BaseServerController.NODE_ID, MethodFeature.LIST));
        // 分发
        put(ClassFeature.OUTGIVING, new DynamicData(OutGivingServer.class, MethodFeature.LIST));
        // 项目
        put(ClassFeature.PROJECT, new DynamicData(ProjectInfoService.class, MethodFeature.LIST));
        // 脚本
        put(ClassFeature.SCRIPT, new DynamicData(ScriptServer.class, MethodFeature.LIST));
        //
//        put(ClassFeature.SSH, new DynamicData(SshServiceOld.class, MethodFeature.LIST));
        //
        put(ClassFeature.TOMCAT, new DynamicData(TomcatService.class, MethodFeature.LIST));
        //
//        put(ClassFeature.BUILD, new DynamicData(BuildInfoService.class, MethodFeature.LIST));
    }

    private static void put(ClassFeature feature, DynamicData dynamicData) {
        DYNAMIC_DATA_MAP.put(feature, dynamicData);
        if (feature.getParent() != null) {
            Set<ClassFeature> classFeatures = PARENT.computeIfAbsent(feature.getParent(), classFeature -> new HashSet<>());
            classFeatures.add(feature);
        }
    }

    /**
     * 获取一级功能
     *
     * @return 子级
     */
    public static List<ClassFeature> getRoot() {
        return DYNAMIC_DATA_MAP.keySet().stream().filter(dynamicData -> dynamicData.getParent() == null).collect(Collectors.toList());
    }

    /**
     * 获取子级功能
     *
     * @param classFeature 功能
     * @return 子级
     */
    public static Set<ClassFeature> getChildren(ClassFeature classFeature) {
        return PARENT.get(classFeature);
    }

    public static Map<ClassFeature, DynamicData> getDynamicDataMap() {
        return DYNAMIC_DATA_MAP;
    }

    public static DynamicData getDynamicData(ClassFeature classFeature) {
        return DYNAMIC_DATA_MAP.get(classFeature);
    }

    /**
     * id 请求参数
     */
    private String parameterName;

    /**
     * 数据操作的server类
     */
    private Class<? extends BaseDynamicService> baseOperService;

    /**
     * 排除验证方法
     */
    private MethodFeature[] excludeMethod;

    private String childrenParameterName;

    private DynamicData(Class<? extends BaseDynamicService> baseOperService, MethodFeature... excludeMethod) {
        this(baseOperService, "id", excludeMethod);
    }

    private DynamicData(Class<? extends BaseDynamicService> baseOperService, String parameterName, MethodFeature... excludeMethod) {
        this(baseOperService, parameterName, null, excludeMethod);
    }

    private DynamicData(Class<? extends BaseDynamicService> baseOperService, String parameterName, String childrenParameterName, MethodFeature... excludeMethod) {
        this.parameterName = parameterName;
        this.baseOperService = baseOperService;
        this.excludeMethod = excludeMethod;
        this.childrenParameterName = childrenParameterName;
    }

    public String getChildrenParameterName() {
        return childrenParameterName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public Class<? extends BaseDynamicService> getBaseOperService() {
        return baseOperService;
    }

    public MethodFeature[] getExcludeMethod() {
        return excludeMethod;
    }
}

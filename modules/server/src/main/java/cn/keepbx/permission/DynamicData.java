package cn.keepbx.permission;

import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.service.node.manage.ProjectInfoService;
import cn.keepbx.jpom.service.node.script.ScriptServer;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.jpom.service.node.tomcat.TomcatService;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        put(ClassFeature.NODE, new DynamicData(NodeService.class, MethodFeature.LIST));
        // 分发
        put(ClassFeature.OUTGIVING, new DynamicData(OutGivingServer.class, MethodFeature.LIST));
        // 项目
        put(ClassFeature.PROJECT, new DynamicData(ProjectInfoService.class, MethodFeature.LIST));
        // 脚本
        put(ClassFeature.SCRIPT, new DynamicData(ScriptServer.class, MethodFeature.LIST));
        //
        put(ClassFeature.SSH, new DynamicData(SshService.class, MethodFeature.LIST));
        //
        put(ClassFeature.TOMCAT, new DynamicData(TomcatService.class, MethodFeature.LIST));
        //
        put(ClassFeature.BUILD, new DynamicData(BuildService.class, MethodFeature.LIST));
    }

    private static void put(ClassFeature feature, DynamicData dynamicData) {
        DYNAMIC_DATA_MAP.put(feature, dynamicData);
        if (feature.getParent() != null) {
            Set<ClassFeature> classFeatures = PARENT.computeIfAbsent(feature.getParent(), classFeature -> new HashSet<>());
            classFeatures.add(feature);
        }
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

    private DynamicData(Class<? extends BaseDynamicService> baseOperService, MethodFeature... excludeMethod) {
        this(baseOperService, "id", excludeMethod);
    }

    private DynamicData(Class<? extends BaseDynamicService> baseOperService, String parameterName, MethodFeature... excludeMethod) {
        this.parameterName = parameterName;
        this.baseOperService = baseOperService;
        this.excludeMethod = excludeMethod;
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

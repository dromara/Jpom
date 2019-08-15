package cn.keepbx.permission;

import cn.keepbx.jpom.service.BaseDynamicService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据权限
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public class DynamicData {

    private static final Map<ClassFeature, DynamicData> DYNAMIC_DATA_MAP = new HashMap<>();

    static {
        DYNAMIC_DATA_MAP.put(ClassFeature.NODE, new DynamicData(NodeService.class, MethodFeature.LIST));

        DYNAMIC_DATA_MAP.put(ClassFeature.OUTGIVING, new DynamicData(OutGivingServer.class, MethodFeature.LIST));
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

    public DynamicData(Class<? extends BaseDynamicService> baseOperService, MethodFeature... excludeMethod) {
        this(baseOperService, "id", excludeMethod);
    }

    public DynamicData(Class<? extends BaseDynamicService> baseOperService, String parameterName, MethodFeature... excludeMethod) {
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

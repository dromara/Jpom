package cn.keepbx.jpom.system.init;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.controller.LoginControl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.AopLogInterface;
import cn.keepbx.jpom.system.OperateType;
import cn.keepbx.jpom.system.WebAopLog;
import com.alibaba.fastjson.JSONObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * 操作记录控制器
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@PreLoadClass
public class OperateLogController implements AopLogInterface {
    private static final ThreadLocal<CacheInfo> CACHE_INFO_THREAD_LOCAL = new ThreadLocal<>();


    @PreLoadMethod
    private static void init() {
        WebAopLog.setAopLogInterface(SpringUtil.getBean(OperateLogController.class));
    }

    @Override
    public void before(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            Method method = methodSignature.getMethod();
            UserOperateLogV1.OptType optType = null;
            OperateType operateType = method.getAnnotation(OperateType.class);
            if (operateType == null) {
                UrlPermission urlPermission = method.getAnnotation(UrlPermission.class);
                if (urlPermission != null) {
                    optType = urlPermission.optType();
                } else {
                    ProjectPermission projectPermission = method.getAnnotation(ProjectPermission.class);
                    if (projectPermission != null) {
                        optType = projectPermission.optType();
                    }
                }
            } else {
                optType = operateType.value();
            }
            if (optType != null) {
                CacheInfo cacheInfo = new CacheInfo();
                cacheInfo.optType = optType;

                ServletRequestAttributes servletRequestAttributes = BaseServerController.getRequestAttributes();
                HttpServletRequest request = servletRequestAttributes.getRequest();
                if (optType == UserOperateLogV1.OptType.Login) {
                    // 获取登录人的信息
                    String userName = request.getParameter("userName");
                    UserService userService = SpringUtil.getBean(UserService.class);
                    cacheInfo.userModel = userService.getItem(userName);
                }
                // 获取ip地址
                cacheInfo.ip = ServletUtil.getClientIP(request);
                // 获取节点
                cacheInfo.nodeModel = (NodeModel) request.getAttribute("node");
                //
                cacheInfo.dataId = request.getParameter("id");
                //
                cacheInfo.userAgent = ServletUtil.getHeaderIgnoreCase(request, HttpHeaders.USER_AGENT);
                //
                Map<String, String[]> map = request.getParameterMap();
                // 过滤密码字段
                Set<Map.Entry<String, String[]>> entries = map.entrySet();
                for (Map.Entry<String, String[]> entry : entries) {
                    String key = entry.getKey();
                    if (StrUtil.containsAnyIgnoreCase(key, "pwd", "password")) {
                        entry.setValue(new String[]{"***"});
                    }
                }
                cacheInfo.setReqData(JSONObject.toJSONString(map));
                CACHE_INFO_THREAD_LOCAL.set(cacheInfo);
            }
        }
    }

    @Override
    public void afterReturning(Object value) {
        CacheInfo cacheInfo = CACHE_INFO_THREAD_LOCAL.get();
        if (cacheInfo == null) {
            return;
        }
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            userModel = cacheInfo.userModel;
        }
        CACHE_INFO_THREAD_LOCAL.remove();
        // 没有对应的用户
        if (userModel == null) {
            return;
        }
        this.log(userModel, value, cacheInfo);
    }

    public void log(String reqId, UserModel userModel,
                    Object value, CacheInfo cacheInfo) {
        String ip = cacheInfo.ip;
        UserOperateLogV1.OptType optType = cacheInfo.optType;
        NodeModel nodeModel = cacheInfo.nodeModel;
        String dataId = cacheInfo.dataId;
        UserOperateLogV1 userOperateLogV1 = new UserOperateLogV1(reqId);
        //
        userOperateLogV1.setUserId(UserModel.getOptUserName(userModel));
        userOperateLogV1.setIp(ip);
        userOperateLogV1.setUserAgent(cacheInfo.userAgent);
        userOperateLogV1.setReqData(cacheInfo.reqData);
        if (value != null) {
            // 解析结果
            String json = value.toString();
            userOperateLogV1.setResultMsg(json);
            try {
                JsonMessage jsonMessage = JSONObject.parseObject(json, JsonMessage.class);
                // 没有输入验证码不记录日志
                if (optType == UserOperateLogV1.OptType.Login && jsonMessage.getCode() == LoginControl.INPUT_CODE) {
                    return;
                }
                userOperateLogV1.setOptStatus(jsonMessage.getCode());
            } catch (Exception ignored) {
            }
        }
        userOperateLogV1.setOptTime(DateUtil.current(false));
        userOperateLogV1.setOptType(optType.getCode());
        //
        if (nodeModel != null) {
            userOperateLogV1.setNodeId(nodeModel.getId());
        }
        //
        userOperateLogV1.setDataId(dataId);
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(UserOperateLogV1.TABLE_NAME);
            entity.parseBean(userOperateLogV1);
            db.insert(entity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void log(UserModel userModel, Object value, CacheInfo cacheInfo) {
        this.log(null, userModel, value, cacheInfo);
    }

    /**
     * 修改执行结果
     *
     * @param reqId 请求id
     * @param val   结果
     */
    public void updateLog(String reqId, String val) {
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(UserOperateLogV1.TABLE_NAME);
            entity.set("resultMsg", val);
            try {
                JsonMessage jsonMessage = JSONObject.parseObject(val, JsonMessage.class);
                entity.set("optStatus", jsonMessage.getCode());
            } catch (Exception ignored) {
            }
            //
            Entity where = new Entity(UserOperateLogV1.TABLE_NAME);
            where.set("reqId", reqId);
            db.update(entity, where);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 临时缓存
     */
    public static class CacheInfo {
        private UserOperateLogV1.OptType optType;
        private UserModel userModel;
        private String ip;
        private NodeModel nodeModel;
        private String dataId;
        private String userAgent;
        private String reqData;

        public void setReqData(String reqData) {
            this.reqData = reqData;
        }

        public void setOptType(UserOperateLogV1.OptType optType) {
            this.optType = optType;
        }

        public void setUserModel(UserModel userModel) {
            this.userModel = userModel;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public void setNodeModel(NodeModel nodeModel) {
            this.nodeModel = nodeModel;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }
}

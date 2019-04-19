package cn.keepbx.jpom.system.init;

import cn.hutool.core.date.DateUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
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
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.SQLException;

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
        UserOperateLogV1.OptType optType = cacheInfo.optType;
        String ip = cacheInfo.ip;
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            userModel = cacheInfo.userModel;
        }
        CACHE_INFO_THREAD_LOCAL.remove();
        // 没有对应的用户
        if (userModel == null) {
            return;
        }
        this.log(userModel, value, ip, optType, cacheInfo.nodeModel);
    }

    public void log(String reqId, UserModel userModel, Object value, String ip, UserOperateLogV1.OptType optType, NodeModel nodeModel) {
        UserOperateLogV1 userOperateLogV1 = new UserOperateLogV1(reqId);
        //
        userOperateLogV1.setUserId(userModel.getId());
        userOperateLogV1.setIp(ip);
        if (value != null) {
            // 解析结果
            String json = value.toString();
            userOperateLogV1.setResultMsg(json);
            try {
                JsonMessage jsonMessage = JSONObject.parseObject(json, JsonMessage.class);
                if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    userOperateLogV1.setOptStatus(UserOperateLogV1.Status.Success.getCode());
                } else {
                    // 没有输入验证码不记录日志
                    if (optType == UserOperateLogV1.OptType.Login && jsonMessage.getCode() == LoginControl.INPUT_CODE) {
                        return;
                    }
                    userOperateLogV1.setOptStatus(jsonMessage.getCode());
                }
            } catch (Exception ignored) {

            }
        }
        userOperateLogV1.setOptTime(DateUtil.current(false));
        userOperateLogV1.setOptType(optType.getCode());
        //
        if (nodeModel != null) {
            userOperateLogV1.setNodeId(nodeModel.getId());
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(UserOperateLogV1.class.getSimpleName().toUpperCase());
            entity.parseBean(userOperateLogV1);
            db.insert(entity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void log(UserModel userModel, Object value, String ip, UserOperateLogV1.OptType optType, NodeModel nodeModel) {
        this.log(null, userModel, value, ip, optType, nodeModel);
    }

    public void updateLog(String reqId, String val) {
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(UserOperateLogV1.class.getSimpleName().toUpperCase());
            entity.set("resultMsg", val);

            Entity where = new Entity(UserOperateLogV1.class.getSimpleName().toUpperCase());
            where.set("reqId", reqId);
            db.update(entity, where);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 临时缓存
     */
    private static class CacheInfo {
        private UserOperateLogV1.OptType optType;
        private UserModel userModel;
        private String ip;
        private NodeModel nodeModel;
    }
}

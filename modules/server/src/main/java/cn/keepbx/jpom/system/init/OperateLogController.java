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
import cn.keepbx.jpom.controller.LoginControl;
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
import java.sql.SQLException;

/**
 * 操作记录控制器
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@PreLoadClass
public class OperateLogController implements AopLogInterface {
    private static final ThreadLocal<OperateType> OPERATE_TYPE_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> IP = new ThreadLocal<>();

    @PreLoadMethod
    private static void init() {
        WebAopLog.setAopLogInterface(SpringUtil.getBean(OperateLogController.class));
    }

    @Override
    public void before(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            OperateType operateType = methodSignature.getMethod().getAnnotation(OperateType.class);
            if (operateType != null) {
                OPERATE_TYPE_THREAD_LOCAL.set(operateType);
                ServletRequestAttributes servletRequestAttributes = BaseServerController.getRequestAttributes();
                HttpServletRequest request = servletRequestAttributes.getRequest();
                if (operateType.value() == UserOperateLogV1.Type.Login) {
                    // 获取登录人的信息
                    String userName = request.getParameter("userName");
                    UserService userService = SpringUtil.getBean(UserService.class);
                    UserModel userModel = userService.getItem(userName);
                    USER_MODEL_THREAD_LOCAL.set(userModel);
                }
                // 获取ip地址
                String ip = ServletUtil.getClientIP(request);
                IP.set(ip);
            }

        }
    }

    @Override
    public void afterReturning(Object value) {
        OperateType operateType = OPERATE_TYPE_THREAD_LOCAL.get();
        if (operateType == null) {
            return;
        }
        String ip = IP.get();
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            userModel = USER_MODEL_THREAD_LOCAL.get();
        }
        OPERATE_TYPE_THREAD_LOCAL.remove();
        USER_MODEL_THREAD_LOCAL.remove();
        IP.remove();
        // 没有对应的用户
        if (userModel == null) {
            return;
        }
        UserOperateLogV1 userOperateLogV1 = new UserOperateLogV1();
        if (userModel.isSystemUser()) {
            userOperateLogV1.setUserId(UserModel.SYSTEM_OCCUPY_NAME);
        } else {
            userOperateLogV1.setUserId(userModel.getId());
        }
        userOperateLogV1.setIp(ip);
        if (value != null) {
            // 解析结果
            String json = value.toString();
            userOperateLogV1.setResultMsg(json);
            JsonMessage jsonMessage = JSONObject.parseObject(json, JsonMessage.class);
            if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                userOperateLogV1.setOptStatus(UserOperateLogV1.Status.Success.getCode());
            } else {
                if (operateType.value() == UserOperateLogV1.Type.Login && jsonMessage.getCode() == LoginControl.INPUT_CODE) {
                    return;
                }
                userOperateLogV1.setOptStatus(jsonMessage.getCode());
            }
        }
        userOperateLogV1.setOptTime(DateUtil.current(false));
        userOperateLogV1.setOptType(operateType.value().getCode());
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
}

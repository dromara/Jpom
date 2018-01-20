package cn.jiangzeyin.system.log.aop;


import cn.jiangzeyin.common.DefaultSystemLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Administrator on 2017/5/11.
 */
@Aspect
@Component
public class WebLog {
    private static final ThreadLocal<Boolean> isLog = new ThreadLocal<>();

    @Pointcut("execution(public * cn.jiangzeyin.controller..*.*(..))")
    public void webLog() {
    }

    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        // 接收到请求，记录请求内容
        isLog.set(true);
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            ResponseBody responseBody = methodSignature.getMethod().getAnnotation(ResponseBody.class);
            if (responseBody == null) {
                RestController restController = joinPoint.getTarget().getClass().getAnnotation(RestController.class);
                if (restController == null)
                    isLog.set(false);
            }
        }
    }

    @AfterReturning(returning = "ret", pointcut = "webLog()")
    public void doAfterReturning(Object ret) throws Throwable {
        // 处理完请求，返回内容
        Boolean isLog_ = isLog.get();
        if (isLog_ != null && !isLog_)
            return;
        if (ret == null)
            return;
        //BaseInterceptor.getNowUserName() +
        DefaultSystemLog.LOG().info(" :" + ret.toString());
    }
}

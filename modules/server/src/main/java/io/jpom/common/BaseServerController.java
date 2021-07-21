package io.jpom.common;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.system.JpomRuntimeException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Jpom server 端
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseServerController extends BaseJpomController {
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

    public static final String NODE_ID = "nodeId";

    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        NodeModel nodeModel = tryGetNode();
        if (nodeModel == null) {
            throw new JpomRuntimeException("节点信息不正确");
        }
        return nodeModel;
    }

    protected NodeModel tryGetNode() {
        String nodeId = getParameter(NODE_ID);
        if (StrUtil.isEmpty(nodeId)) {
            return null;
        }
        return nodeService.getItem(nodeId);
    }

    @Override
    public void resetInfo() {
        USER_MODEL_THREAD_LOCAL.set(getUserModel());
    }

    protected UserModel getUser() {
        UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
        Objects.requireNonNull(userModel);
        return userModel;
    }

    public static void remove() {
        USER_MODEL_THREAD_LOCAL.remove();
    }

    public static UserModel getUserModel() {
        ServletRequestAttributes servletRequestAttributes = tryGetRequestAttributes();
        if (servletRequestAttributes == null) {
            return null;
        }
        return (UserModel) servletRequestAttributes.getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * 处理分页的时间字段
     *
     * @param page    分页
     * @param entity  条件
     * @param colName 字段名称
     */
    protected void doPage(Page page, Entity entity, String colName) {
        String time = getParameter("time");
        colName = colName.toUpperCase();
        page.addOrder(new Order(colName, Direction.DESC));
        // 时间
        if (StrUtil.isNotEmpty(time)) {
            String[] val = StrUtil.splitToArray(time, "~");
            if (val.length == 2) {
                DateTime startDateTime = DateUtil.parse(val[0], DatePattern.NORM_DATETIME_FORMAT);
                entity.set(colName, ">= " + startDateTime.getTime());

                DateTime endDateTime = DateUtil.parse(val[1], DatePattern.NORM_DATETIME_FORMAT);
                if (startDateTime.equals(endDateTime)) {
                    endDateTime = DateUtil.endOfDay(endDateTime);
                }
                // 防止字段重复
                entity.set(colName + " ", "<= " + endDateTime.getTime());
            }
        }
    }
}

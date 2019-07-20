package cn.keepbx.jpom.common;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.JpomRuntimeException;
import org.springframework.web.context.request.RequestAttributes;

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

    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        String nodeId = getParameter("nodeId");
        NodeModel nodeModel = nodeService.getItem(nodeId);
        if (nodeModel == null) {
            throw new JpomRuntimeException("节点信息不正确");
        }
        return nodeModel;
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
        return (UserModel) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
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
            String[] val = StrUtil.split(time, "~");
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

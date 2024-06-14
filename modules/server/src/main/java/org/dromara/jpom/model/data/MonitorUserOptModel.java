/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import com.alibaba.fastjson2.JSON;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控用户操作实体
 *
 * @author Arno
 */
@TableName(value = "MONITOR_USER_OPT",
    nameKey = "i18n.monitoring_user_actions.f2d5")
public class MonitorUserOptModel extends BaseWorkspaceModel {
    /**
     *
     */
    private String name;
    /**
     * 监控的人员
     */
    private String monitorUser;
    /**
     * 监控的功能
     *
     * @see ClassFeature
     */
    private String monitorFeature;
    /**
     * 监控的操作
     *
     * @see MethodFeature
     */
    private String monitorOpt;
    /**
     * 报警联系人
     */
    private String notifyUser;

    /**
     * 监控开启状态
     */
    private Boolean status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMonitorUser(String monitorUser) {
        this.monitorUser = monitorUser;
    }

    public String getMonitorFeature() {
        List<ClassFeature> object = monitorFeature();
        return object == null ? null : JSON.toJSONString(object);
    }

    public List<ClassFeature> monitorFeature() {
        return StringUtil.jsonConvertArray(monitorFeature, ClassFeature.class);
    }

    public void setMonitorFeature(String monitorFeature) {
        this.monitorFeature = monitorFeature;
    }

    public void monitorFeature(List<ClassFeature> monitorFeature) {
        if (monitorFeature == null) {
            this.monitorFeature = null;
        } else {
            this.monitorFeature = JSON.toJSONString(monitorFeature.stream().map(Enum::name).collect(Collectors.toList()));
        }
    }

    public String getMonitorOpt() {
        List<MethodFeature> object = monitorOpt();
        return object == null ? null : JSON.toJSONString(object);
    }


    public List<MethodFeature> monitorOpt() {
        return StringUtil.jsonConvertArray(monitorOpt, MethodFeature.class);
    }

    public void setMonitorOpt(String monitorOpt) {
        this.monitorOpt = monitorOpt;
    }

    public void monitorOpt(List<MethodFeature> monitorOpt) {
        if (monitorOpt == null) {
            this.monitorOpt = null;
        } else {
            this.monitorOpt = JSON.toJSONString(monitorOpt.stream().map(Enum::name).collect(Collectors.toList()));
        }
    }

    public void setNotifyUser(String notifyUser) {
        this.notifyUser = notifyUser;
    }

    public Boolean getStatus() {
        return status;
    }

    public String getMonitorUser() {
        List<String> object = monitorUser();
        return object == null ? null : JSON.toJSONString(object);
    }

    public String getNotifyUser() {
        List<String> object = notifyUser();
        return object == null ? null : JSON.toJSONString(object);

    }

    public List<String> monitorUser() {
        return StringUtil.jsonConvertArray(monitorUser, String.class);
    }

    public void monitorUser(List<String> monitorUser) {
        if (monitorUser == null) {
            this.monitorUser = null;
        } else {
            this.monitorUser = JSON.toJSONString(monitorUser);
        }
    }

    public List<String> notifyUser() {
        return StringUtil.jsonConvertArray(notifyUser, String.class);
    }

    public void notifyUser(List<String> notifyUser) {
        if (notifyUser == null) {
            this.notifyUser = null;
        } else {
            this.notifyUser = JSON.toJSONString(notifyUser);
        }
    }


    public boolean isStatus() {
        return status != null && status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}

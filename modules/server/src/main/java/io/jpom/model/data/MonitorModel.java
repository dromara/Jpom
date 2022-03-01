/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseJsonModel;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

/**
 * 监控管理实体
 *
 * @author Arno
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MONITOR_INFO", name = "监控信息")
@Data
public class MonitorModel extends BaseWorkspaceModel {

    private String name;
    /**
     * 监控的项目
     */
    private String projects;
    /**
     * 报警联系人
     */
    private String notifyUser;
    /**
     * 异常后是否自动重启
     */
    private Boolean autoRestart;
    /**
     * 监控周期
     *
     * @see io.jpom.model.Cycle
     */
    @Deprecated
    private Integer cycle;
    /**
     * 监控定时周期
     */
    private String execCron;
    /**
     * 监控开启状态
     */
    private Boolean status;
    /**
     * 报警状态
     */
    private Boolean alarm;
    /**
     * webhook
     */
    private String webhook;

    public String getExecCron() {
        if (execCron == null) {
            // 兼容旧版本
            if (cycle != null) {
                return String.format("0 0/%s * * * ?", cycle);
            }
        }
        return execCron;
    }

    public boolean autoRestart() {
        return autoRestart != null && autoRestart;
    }

    /**
     * 开启状态
     *
     * @return true 启用
     */
    public boolean status() {
        return status != null && status && StrUtil.isNotEmpty(getExecCron());
    }

    public List<NodeProject> projects() {
        return StringUtil.jsonConvertArray(projects, NodeProject.class);
    }


    public String getProjects() {
        List<NodeProject> projects = projects();
        return projects == null ? null : JSON.toJSONString(projects);
    }

    public void projects(List<NodeProject> projects) {
        if (projects == null) {
            this.projects = null;
        } else {
            this.projects = JSON.toJSONString(projects);
        }
    }

    public List<String> notifyUser() {
        return StringUtil.jsonConvertArray(notifyUser, String.class);
    }

    public String getNotifyUser() {
        List<String> object = notifyUser();
        return object == null ? null : JSON.toJSONString(object);
    }

    public void notifyUser(List<String> notifyUser) {
        if (notifyUser == null) {
            this.notifyUser = null;
        } else {
            this.notifyUser = JSON.toJSONString(notifyUser);
        }
    }

    public boolean checkNodeProject(String nodeId, String projectId) {
        List<NodeProject> projects = projects();
        if (projects == null) {
            return false;
        }
        for (NodeProject project : projects) {
            if (project.getNode().equals(nodeId)) {
                List<String> projects1 = project.getProjects();
                if (projects1 == null) {
                    return false;
                }
                for (String s : projects1) {
                    if (projectId.equals(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Getter
    public enum NotifyType implements BaseEnum {
        /**
         * 通知方式
         */
        dingding(0, "钉钉"),
        mail(1, "邮箱"),
        workWx(2, "企业微信"),
        webhook(3, "webhook"),
        ;

        private final int code;
        private final String desc;

        NotifyType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 通知
     */
    public static class Notify extends BaseJsonModel {
        private int style;
        private String value;

        public Notify() {
        }

        public Notify(NotifyType style, String value) {
            this.style = style.getCode();
            this.value = value;
        }

        public int getStyle() {
            return style;
        }

        public void setStyle(int style) {
            this.style = style;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class NodeProject extends BaseJsonModel {
        /**
         * 节点 ID
         */
        private String node;
        /**
         * 被监控的项目ID
         */
        private List<String> projects;

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public List<String> getProjects() {
            return projects;
        }

        public void setProjects(List<String> projects) {
            this.projects = projects;
        }
    }
}

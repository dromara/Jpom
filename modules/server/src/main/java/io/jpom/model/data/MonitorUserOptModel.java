/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

import io.jpom.model.BaseModel;
import io.jpom.model.log.UserOperateLogV1;

import java.util.List;

/**
 * 监控用户操作实体
 *
 * @author Arno
 */
public class MonitorUserOptModel extends BaseModel {
    /**
     * 监控的人员
     */
    private List<String> monitorUser;

    /**
     * 监控的操作
     */
    private List<UserOperateLogV1.OptType> monitorOpt;
    /**
     * 报警联系人
     */
    private List<String> notifyUser;

    /**
     * 创建人
     */
    private String parent;
    /**
     * 最后修改时间
     */
    private long modifyTime;
    /**
     * 监控开启状态
     */
    private boolean status;

    public List<String> getMonitorUser() {
        return monitorUser;
    }

    public void setMonitorUser(List<String> monitorUser) {
        this.monitorUser = monitorUser;
    }

    public List<UserOperateLogV1.OptType> getMonitorOpt() {
        return monitorOpt;
    }

    public void setMonitorOpt(List<UserOperateLogV1.OptType> monitorOpt) {
        this.monitorOpt = monitorOpt;
    }

    public List<String> getNotifyUser() {
        return notifyUser;
    }

    public void setNotifyUser(List<String> notifyUser) {
        this.notifyUser = notifyUser;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}

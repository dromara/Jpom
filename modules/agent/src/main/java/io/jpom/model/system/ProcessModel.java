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
package io.jpom.model.system;

import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.BaseJsonModel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 进程信息实体
 *
 * @author jiangzeyin
 * @since 2019/4/15
 */
@Slf4j
public class ProcessModel extends BaseJsonModel {
    /**
     * 进程id
     */
    private int pid;
    /**
     * 进程名
     */
    private String command;
    /**
     * 运行状态
     */
    private String status;
    /**
     * 进程仍然在使用的，没被交换出物理内存部分的大小
     */
    private String res;
    /**
     * 所有者
     */
    private String user;
    /**
     * 时间总计
     */
    private String time;
    /**
     * 优先级
     */
    private String pr = StrUtil.DASHED;
    /**
     * nice值
     */
    private String ni = StrUtil.DASHED;
    /**
     * 虚拟内存
     */
    private String virt = StrUtil.DASHED;
    /**
     * 共享内存大小
     */
    private String shr = StrUtil.DASHED;
    /**
     * 内存比重
     */
    private String mem = StrUtil.DASHED;
    /**
     * cpu比重
     */
    private String cpu = StrUtil.DASHED;
    /**
     * 端口
     */
    private String port = StrUtil.DASHED;
    /**
     * Jpom 项目名称
     */
    private String jpomName = StrUtil.DASHED;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getJpomName() {
        return jpomName;
    }

    public void setJpomName(String jpomName) {
        this.jpomName = jpomName;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
        if (pid > 0) {
            String port = AbstractProjectCommander.getInstance().getMainPort(pid);
            this.setPort(port);
            //
            try {
                String jpomName = AbstractProjectCommander.getInstance().getJpomNameByPid(pid);
                this.setJpomName(jpomName);
            } catch (IOException e) {
                log.error("解析进程失败", e);
            }
        }
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPr() {
        return pr;
    }

    public void setPr(String pr) {
        this.pr = pr;
    }

    public String getNi() {
        return ni;
    }

    public void setNi(String ni) {
        this.ni = ni;
    }

    public String getVirt() {
        return virt;
    }

    public void setVirt(String virt) {
        this.virt = virt;
    }

    public String getShr() {
        return shr;
    }

    public void setShr(String shr) {
        this.shr = shr;
    }

    public String getMem() {
        return mem;
    }

    public void setMem(String mem) {
        this.mem = mem;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }
}

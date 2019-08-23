package io.jpom.model.system;

import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseJsonModel;

/**
 * 网络端口信息实体
 *
 * @author jiangzeyin
 * @date 2019/4/10
 */
public class NetstatModel extends BaseJsonModel {
    private String protocol;
    private String receive = StrUtil.DASHED;
    private String send = StrUtil.DASHED;
    private String local;
    private String foreign;
    private String status;
    private String name;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getReceive() {
        return receive;
    }

    public void setReceive(String receive) {
        this.receive = receive;
    }

    public String getSend() {
        return send;
    }

    public void setSend(String send) {
        this.send = send;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getForeign() {
        return foreign;
    }

    public void setForeign(String foreign) {
        this.foreign = foreign;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

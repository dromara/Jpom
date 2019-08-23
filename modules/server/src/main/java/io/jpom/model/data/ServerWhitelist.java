package io.jpom.model.data;

import io.jpom.model.BaseJsonModel;

import java.util.List;

/**
 * 节点分发白名单
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
public class ServerWhitelist extends BaseJsonModel {
    private List<String> outGiving;

    public List<String> getOutGiving() {
        return outGiving;
    }

    public void setOutGiving(List<String> outGiving) {
        this.outGiving = outGiving;
    }
}

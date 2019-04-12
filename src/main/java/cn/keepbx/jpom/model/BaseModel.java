package cn.keepbx.jpom.model;

import com.alibaba.fastjson.JSON;

/**
 * @author jiangzeyin
 * @date 2019/3/14
 */
public abstract class BaseModel {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

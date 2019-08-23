package io.jpom.model;

/**
 * 基础实体（带id）
 *
 * @author jiangzeyin
 * @date 2019/3/14
 */
public abstract class BaseModel extends BaseJsonModel {
    private String id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

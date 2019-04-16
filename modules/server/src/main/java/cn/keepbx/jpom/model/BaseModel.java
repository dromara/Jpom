package cn.keepbx.jpom.model;

/**
 * 基础实体（带id）
 *
 * @author jiangzeyin
 * @date 2019/3/14
 */
public abstract class BaseModel extends BaseJsonModel {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

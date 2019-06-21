package cn.keepbx.jpom.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 基础实体（带id）
 *
 * @author jiangzeyin
 * @date 2019/3/14
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public abstract class BaseModel extends BaseJsonModel {
    private String id;
    private String name;


}

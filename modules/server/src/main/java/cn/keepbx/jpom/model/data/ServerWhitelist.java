package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 节点分发白名单
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ServerWhitelist extends BaseJsonModel {
    private List<String> outGiving;
}

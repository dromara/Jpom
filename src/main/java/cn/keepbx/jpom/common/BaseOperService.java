package cn.keepbx.jpom.common;

import java.io.IOException;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/3/14
 */
public abstract class BaseOperService<T> extends BaseDataService {
    /**
     * 获取所有数据
     *
     * @return list
     * @throws IOException IO
     */
    public abstract List<T> list() throws IOException;
}

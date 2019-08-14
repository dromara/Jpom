package cn.keepbx.jpom.model.vo;

import cn.keepbx.build.BuildUtil;
import cn.keepbx.jpom.model.data.BuildModel;

import java.io.File;

/**
 * vo
 *
 * @author bwcx_jzy
 * @date 2019/8/14
 */
public class BuildModelVo extends BuildModel {

    /**
     * 代码是否存在
     */
    private boolean sourceExist;

    public boolean isSourceExist() {
        File source = BuildUtil.getSource(this);
        return source.exists();
    }

    public void setSourceExist(boolean sourceExist) {
        this.sourceExist = sourceExist;
    }
}

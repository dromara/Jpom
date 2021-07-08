package io.jpom.model.vo;

import io.jpom.build.BuildUtil;
import io.jpom.model.data.BuildModel;

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
        sourceExist = source.exists();
        return sourceExist;
    }

    public void setSourceExist(boolean sourceExist) {
        this.sourceExist = sourceExist;
    }
}

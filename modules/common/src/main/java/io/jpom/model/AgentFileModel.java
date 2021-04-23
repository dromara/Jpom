package io.jpom.model;

/**
 * @author lf
 */
public class AgentFileModel extends BaseModel {

    /**
     * 文件大小
     */
    private long size = 0;

    /**
     * 保存路径
     */
    private String savePath;

    private String version;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

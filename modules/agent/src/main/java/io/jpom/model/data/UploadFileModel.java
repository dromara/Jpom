package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import io.jpom.model.BaseModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author lf
 */
public class UploadFileModel extends BaseModel {
    private long size = 0;
    private long completeSize = 0;
    private String savePath;
    private String version;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
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

    public void save(byte[] data) {
        this.completeSize += data.length;
        File file = new File(this.getFilePath());
        FileUtil.mkParentDirs(file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(data);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFilePath() {
        return savePath + "/" + getName();
    }

    public void remove() {
        FileUtil.del(this.getFilePath());
    }
}

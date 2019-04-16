package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseJsonModel;

import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class Whitelist extends BaseJsonModel {
    private List<String> project;
    private List<String> certificate;
    private List<String> nginx;

    public List<String> getProject() {
        return project;
    }

    public void setProject(List<String> project) {
        this.project = project;
    }

    public List<String> getCertificate() {
        return certificate;
    }

    public void setCertificate(List<String> certificate) {
        this.certificate = certificate;
    }

    public List<String> getNginx() {
        return nginx;
    }

    public void setNginx(List<String> nginx) {
        this.nginx = nginx;
    }
}

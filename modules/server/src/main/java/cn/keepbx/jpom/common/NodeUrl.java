package cn.keepbx.jpom.common;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public enum NodeUrl {
    /**
     *
     */
    GetTop("/getTop");

    private String url;

    public String getUrl() {
        return url;
    }

    NodeUrl(String url) {
        this.url = url;
    }
}

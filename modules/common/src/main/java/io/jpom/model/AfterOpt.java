package io.jpom.model;

/**
 * @author bwcx_jzy
 * @date 2020/3/21
 */
public enum AfterOpt implements BaseEnum {
    /**
     * 操作
     */
    No(0, "不做任何操作"),
    /**
     * 并发执行项目分发
     */
    Restart(1, "并发重启"),
    /**
     * 顺序执行项目分发
     */
    Order_Must_Restart(2, "完整顺序重启(有重启失败将结束本次)"),
    /**
     * 顺序执行项目分发
     */
    Order_Restart(3, "顺序重启(有重启失败将继续)"),
    ;
    private int code;
    private String desc;

    AfterOpt(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}

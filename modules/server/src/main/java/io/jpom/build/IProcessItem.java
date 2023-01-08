package io.jpom.build;

/**
 * @author bwcx_jzy
 * @since 2023/1/8
 */
public interface IProcessItem {

    /**
     * 流程名称
     *
     * @return 名称
     */
    String name();

    /**
     * 执行流程
     *
     * @return 执行结果，false 不再继续执行后续流程
     */
    boolean execute();
}

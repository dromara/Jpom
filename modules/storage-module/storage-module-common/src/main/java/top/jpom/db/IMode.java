package top.jpom.db;

/**
 * @author bwcx_jzy
 * @since 2023/1/5
 */
public interface IMode {

    /**
     * 当前模式
     *
     * @return 当前运行模式
     */
    DbExtConfig.Mode mode();
}

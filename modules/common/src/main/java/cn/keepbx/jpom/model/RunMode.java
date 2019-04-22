package cn.keepbx.jpom.model;

/**
 * 项目的运行方式
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
public enum RunMode {
    /**
     * java -classpath
     */
    ClassPath,
    /**
     * java -jar
     */
    Jar,
}

package io.jpom.model;

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
    /**
     * java -jar  Springboot war
     */
    JarWar,
    /**
     * java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
     */
    JavaExtDirsCp,
    /**
     * 纯文件管理
     */
    File,
}

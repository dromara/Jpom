/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

/**
 * 项目的运行方式
 *
 * @author bwcx_jzy
 * @since 2019/4/22
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
    /**
     * 自定义项目管理
     */
    Dsl,
    /**
     * 软链
     */
    Link,
}

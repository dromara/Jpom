/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.entity;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/11/19
 */
public class NodeProjectInfo extends ProjectInfo {

    private String nodeId;

    /**
     * 副本集
     */
    private List<JavaCopy> javaCopys;

    public List<JavaCopy> getJavaCopys() {
        return javaCopys;
    }

    public void setJavaCopys(List<JavaCopy> javaCopys) {
        this.javaCopys = javaCopys;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public NodeProjectInfo(ProjectInfo projectInfo) {
        this.copy(projectInfo);
    }

    public void copy(ProjectInfo projectInfo) {
        if (super.getId() == null) {
            super.setId(projectInfo.getId());
        }
        if (super.getName() == null) {
            super.setName(projectInfo.getName());
        }
        if (getGroup() == null) {
            super.setGroup(projectInfo.getGroup());
        }
        if (getArgs() == null) {
            super.setArgs(projectInfo.getArgs());
        }
        if (getWhitelistDirectory() == null) {
            super.setWhitelistDirectory(projectInfo.getWhitelistDirectory());
        }
        if (getPath() == null) {
            super.setPath(projectInfo.getPath());
        }
        if (getJvm() == null) {
            super.setJvm(projectInfo.getJvm());
        }
        if (getMainClass() == null) {
            super.setMainClass(projectInfo.getMainClass());
        }
        if (getRunMode() == null) {
            super.setRunMode(projectInfo.getRunMode());
        }
        if (getWebHook() == null) {
            super.setWebHook(projectInfo.getWebHook());
        }
    }

    public NodeProjectInfo() {
    }

    public static class JavaCopy {
        private String id;

        private String jvm;

        private String args;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getJvm() {
            return jvm;
        }

        public void setJvm(String jvm) {
            this.jvm = jvm;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }
    }
}

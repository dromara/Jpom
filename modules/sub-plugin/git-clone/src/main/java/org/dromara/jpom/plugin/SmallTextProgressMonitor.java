/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import org.eclipse.jgit.lib.TextProgressMonitor;

import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * 少量的输出，只在进度 x%5=0 的时候输出
 *
 * @author bwcx_jzy
 * @see 2022/11/28
 */
public class SmallTextProgressMonitor extends TextProgressMonitor {

    private final Set<Integer> progressRangeList;
    private final int reduceProgressRatio;

    /**
     * @param out                 输出流
     * @param reduceProgressRatio 压缩折叠显示进度比例 范围 1-100
     */
    public SmallTextProgressMonitor(Writer out, int reduceProgressRatio) {
        super(out);
        this.reduceProgressRatio = reduceProgressRatio;
        this.progressRangeList = new HashSet<>((int) Math.floor((float) 100 / reduceProgressRatio));
    }

    @Override
    protected void onUpdate(String taskName, int cmp, int totalWork, int pcnt) {
        int progressRange = (int) Math.floor((float) pcnt / this.reduceProgressRatio);
        if (progressRangeList.add(progressRange)) {
            super.onUpdate(taskName, cmp, totalWork, pcnt);
        }
    }
}

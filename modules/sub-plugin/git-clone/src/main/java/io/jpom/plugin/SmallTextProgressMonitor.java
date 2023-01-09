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
package io.jpom.plugin;

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

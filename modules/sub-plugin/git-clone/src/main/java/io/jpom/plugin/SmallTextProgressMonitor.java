package io.jpom.plugin;

import org.eclipse.jgit.lib.TextProgressMonitor;

import java.io.Writer;

/**
 * 少量的输出，只在进度 x%5=0 的时候输出
 *
 * @author bwcx_jzy
 * @see 2022/11/28
 */
public class SmallTextProgressMonitor extends TextProgressMonitor {

    public SmallTextProgressMonitor(Writer out) {
        super(out);
    }

    @Override
    protected void onUpdate(String taskName, int cmp, int totalWork, int pcnt) {
        if (pcnt % 5 == 0) {
            super.onUpdate(taskName, cmp, totalWork, pcnt);
        }
    }
}

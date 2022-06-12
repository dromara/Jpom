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
package io.jpom.system.init;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.JvmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据目录权限检查
 *
 * @author jiangzeyin
 * @since 2019/3/26
 */
@PreLoadClass(value = Integer.MIN_VALUE)
@Slf4j
public class CheckPath {

    /**
     * 判断是否重复运行
     */
    @PreLoadMethod(2)
    private static void checkDuplicateRun() {
        try {
            Class<?> appClass = JpomApplication.getAppClass();
            String pid = String.valueOf(JpomManifest.getInstance().getPid());
            Integer mainClassPid = JvmUtil.findMainClassPid(appClass.getName());
            if (mainClassPid == null || pid.equals(ObjectUtil.toString(mainClassPid))) {
                return;
            }
            log.warn("The Jpom program recommends that only one corresponding program be run on a machine：" + JpomApplication.getAppType() + "  pid:" + mainClassPid);
        } catch (Exception e) {
            log.error("检查异常", e);
        }
    }

    @PreLoadMethod(3)
    private static void reqXssLog() {
        if (!ExtConfigBean.getInstance().isConsoleLogReqXss()) {
            // 不在控制台记录请求日志信息
            DefaultSystemLog.setLogCallback(new DefaultSystemLog.LogCallback() {
                @Override
                public void log(DefaultSystemLog.LogType type, Object... log) {
                    //
                    if (type == DefaultSystemLog.LogType.REQUEST_ERROR) {
                        CheckPath.log.info(Arrays.toString(log));
                    }
                }

                @Override
                public void logStart(HttpServletRequest request, String id, String url, HttpMethod httpMethod, String ip, Map<String, String> parameters, Map<String, String> header) {

                }

                @Override
                public void logError(String id, int status) {

                }

                @Override
                public void logTimeOut(String id, long time) {

                }
            });
        }
    }

    @PreLoadMethod(4)
    private static void clearTemp() {
        File file = ConfigBean.getInstance().getTempPath();
        /**
         * @author Hotstrip
         * use Hutool's FileUtil.del method just put file as param not file's path
         * or else,  may be return Accessdenied exception
         */
        try {
            FileUtil.del(file);
        } catch (Exception e) {
            // Try again  jzy 2021-07-31
            log.warn("Attempt to delete temporary folder failed, try to handle read-only permission：{}", e.getMessage());
            List<File> files = FileUtil.loopFiles(file);
            long count = files.stream().map(file12 -> file12.setWritable(true)).filter(aBoolean -> aBoolean).count();
            log.warn("Cumulative number of files in temporary folder: {}, number of successful processing：{}", CollUtil.size(files), count);
            try {
                FileUtil.del(file.toPath());
            } catch (Exception e1) {
                e1.addSuppressed(e);
                boolean causedBy = ExceptionUtil.isCausedBy(e1, AccessDeniedException.class);
                if (causedBy) {
                    log.error("清除临时文件失败,请手动清理：" + FileUtil.getAbsolutePath(file), e);
                    return;
                }
                log.error("清除临时文件失败,请检查目录：" + FileUtil.getAbsolutePath(file), e);
            }
        }
    }
}

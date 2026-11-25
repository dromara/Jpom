/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.core.release;

import org.dromara.jpom.build.DockerImageReleaseTarget;
import org.dromara.jpom.build.FileStorageReleaseTarget;
import org.dromara.jpom.build.FtpReleaseTarget;
import org.dromara.jpom.build.LocalCommandReleaseTarget;
import org.dromara.jpom.build.NoopReleaseTarget;
import org.dromara.jpom.build.OutgivingReleaseTarget;
import org.dromara.jpom.build.ProjectReleaseTarget;
import org.dromara.jpom.build.SshProjectReleaseTarget;
import org.dromara.jpom.build.SshReleaseTarget;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发布目标注册表
 * <p>
 * 新增一种发布方式：实现 {@link ReleaseTarget} 并在静态块注册一行；调用方零改动。
 * 重复注册同一码值直接抛错（启动期暴露，而不是运行时静默覆盖）。
 *
 * @author jpom
 * @since 2.11.16
 */
public final class ReleaseTargetRegistry {

    private static final Map<Integer, ReleaseTarget> TARGETS = new LinkedHashMap<>();

    static {
        register(new NoopReleaseTarget());
        register(new OutgivingReleaseTarget());
        register(new ProjectReleaseTarget());
        register(new SshReleaseTarget());
        register(new LocalCommandReleaseTarget());
        register(new DockerImageReleaseTarget());
        register(new FtpReleaseTarget());
        register(new SshProjectReleaseTarget());
        register(new FileStorageReleaseTarget());
    }

    private ReleaseTargetRegistry() {
    }

    /**
     * 注册实现
     *
     * @param target 实现
     */
    public static synchronized void register(ReleaseTarget target) {
        ReleaseTarget exists = TARGETS.put(target.method(), target);
        if (exists != null) {
            throw new IllegalStateException("发布方式重复注册：" + target.method());
        }
    }

    /**
     * 按发布方式取实现
     *
     * @param method 发布方式码
     * @return 实现；未注册返回 null（调用方按「未实现的发布方式」报错）
     */
    public static ReleaseTarget get(int method) {
        return TARGETS.get(method);
    }

    /**
     * @return 已注册的发布方式（只读，诊断/测试用）
     */
    public static Map<Integer, ReleaseTarget> all() {
        return Collections.unmodifiableMap(TARGETS);
    }
}
/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.GlobalHeaders;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgentInfo;
import cn.hutool.system.JavaInfo;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.Type;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.TransportUserAgent;

/**
 * Jpom 出站请求的 User-Agent 定义（唯一产生点）。
 * <p>所有出站 {@code User-Agent} 都由本类生成，不再散落到各调用点；hutool 通道通过
 * {@link #applyGlobal(Type, String)} 写入 {@link GlobalHeaders}，非 hutool 通道（原生
 * {@code HttpURLConnection}、WebSocket 握手）显式取 {@link #of(Type, String)} 写入请求头。</p>
 * <p>格式与 Go 插件端 {@code internal/agentid} 必须逐字一致，两端一致性由共享向量
 * {@code jpom-parent/contract/user-agent.tsv} 与 {@code preflight-check.sh --ua} 门禁保证。</p>
 *
 * @author jpom
 * @since 2026/11/26
 */
public final class JpomUserAgent {

    /**
     * 请求头名称（唯一字面量在传输层常量 {@link TransportServer#USER_AGENT}）
     */
    public static final String HEADER_NAME = TransportServer.USER_AGENT;

    /**
     * UA 模板：{@code {}} 依次为 osName、osVersion、arch、runtime、type、version。
     * <p>与 Go 插件端 {@code agentid.Template}（{@code %s} 顺序相同）逐字一致。</p>
     */
    public static final String TEMPLATE = "Mozilla/5.0 ({} {}; {}; {}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.0000.000 Safari/537.36 Jpom {}/{}";

    /**
     * 无法识别的取值（与 hutool {@link UserAgentInfo#NameUnknown} 一致）
     */
    public static final String UNKNOWN = UserAgentInfo.NameUnknown;

    /**
     * 版本为空时的回退值（开发运行时 {@link JpomManifest#getVersion()} 为 dev）
     */
    public static final String UNKNOWN_VERSION = "dev";

    /**
     * 已注入的 UA（供非 hutool 通道直接取用，避免重复计算）
     */
    private static volatile String applied;

    private JpomUserAgent() {
    }

    /**
     * 当前进程的 UA（未注入时回退到运行环境计算）
     *
     * @return UA
     */
    public static String value() {
        String cached = applied;
        if (cached != null) {
            return cached;
        }
        return of(JpomApplication.getAppType(), JpomManifest.getInstance().getVersion());
    }

    /**
     * 按当前运行环境生成 UA
     *
     * @param type    程序类型
     * @param version 程序版本
     * @return UA
     */
    public static String of(Type type, String version) {
        OsInfo osInfo = SystemUtil.getOsInfo();
        JavaInfo javaInfo = SystemUtil.getJavaInfo();
        // 容器内运行时 os 名称统一为 docker（与历史行为一致）
        boolean inDocker = StrUtil.isNotEmpty(SystemUtil.get("JPOM_PKG"));
        return of(type == null ? null : type.name(), version,
            inDocker ? "docker" : osInfo.getName(),
            osInfo.getVersion(),
            osInfo.getArch(),
            javaInfo == null ? null : javaInfo.getVersion());
    }

    /**
     * 按显式平台信息生成 UA（契约向量测试走这里，避免依赖宿主环境）
     *
     * @param typeName       程序类型名（{@code Agent}/{@code Server}）
     * @param version        程序版本
     * @param osName         系统名称
     * @param osVersion      系统版本
     * @param arch           架构
     * @param runtimeVersion 运行时版本（Java 为 JDK 版本，Go 为 go 版本）
     * @return UA
     */
    public static String of(String typeName, String version, String osName, String osVersion, String arch, String runtimeVersion) {
        return StrUtil.format(TEMPLATE,
            StrUtil.blankToDefault(osName, UNKNOWN),
            StrUtil.blankToDefault(osVersion, "0"),
            StrUtil.blankToDefault(arch, UNKNOWN),
            StrUtil.blankToDefault(runtimeVersion, UNKNOWN),
            StrUtil.blankToDefault(typeName, UNKNOWN),
            StrUtil.blankToDefault(version, UNKNOWN_VERSION));
    }

    /**
     * 把 UA 写入 hutool 全局请求头（hutool 通道覆盖点）
     *
     * @param type    程序类型
     * @param version 程序版本
     */
    public static void applyGlobal(Type type, String version) {
        String userAgent = of(type, version);
        applied = userAgent;
        GlobalHeaders.INSTANCE.header(Header.USER_AGENT, userAgent, true);
        // 传输层（隧道握手、回环 ws、服务端到插件端 ws）由装配点统一取用
        TransportUserAgent.set(() -> userAgent);
    }

    /**
     * 确保 UA 已注入全局请求头。
     * <p>注入发生在 {@link JpomManifest#getInstance()} 首次调用时（延迟初始化），
     * 启动阶段就要发出站请求的入口（如插件端自动注册推送）必须先调用本方法。</p>
     */
    public static void ensureApplied() {
        JpomManifest.getInstance();
    }
}

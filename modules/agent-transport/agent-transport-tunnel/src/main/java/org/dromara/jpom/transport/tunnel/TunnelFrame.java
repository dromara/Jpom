/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel;

import java.util.Map;

/**
 * 隧道控制帧（JSON 文本帧）。
 * <p>
 * 一个类承载全部控制帧字段，序列化时按需取用（fastjson2 默认不输出 null 字段），
 * 避免为每种帧维护独立的类与映射表。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelFrame {

    /**
     * 帧类型
     *
     * @see TunnelFrameType
     */
    private String type;
    /**
     * 逻辑请求 / 流标识
     */
    private String sid;

    // ---------- HELLO ----------
    private String machineId;
    private String key;
    private String agentVersion;
    private String os;
    private String arch;
    private Long ts;
    private String nonce;
    private String sign;

    // ---------- HELLO_ACK ----------
    private String sessionId;
    private Integer heartbeat;
    private Integer maxConcurrency;
    private String serverVersion;

    // ---------- REJECT / ABORT / CLOSE ----------
    private Integer code;
    private String msg;

    // ---------- REQ ----------
    private String method;
    private String path;
    private Map<String, String> headers;
    private String contentType;
    private Long len;
    private Integer timeout;

    // ---------- OPEN ----------
    private Map<String, String> query;

    // ---------- OPEN_ACK ----------
    private Boolean ok;

    // ---------- RES ----------
    private Integer status;

    // ---------- ACK ----------
    private Long n;

    public TunnelFrame() {
    }

    public TunnelFrame(TunnelFrameType type) {
        this.type = type == null ? null : type.name();
    }

    public TunnelFrameType frameType() {
        return TunnelFrameType.of(this.type);
    }

    public static TunnelFrame of(TunnelFrameType type, String sid) {
        TunnelFrame frame = new TunnelFrame(type);
        frame.setSid(sid);
        return frame;
    }

    public static TunnelFrame ping() {
        return new TunnelFrame(TunnelFrameType.PING);
    }

    public static TunnelFrame pong() {
        return new TunnelFrame(TunnelFrameType.PONG);
    }

    public static TunnelFrame end(String sid) {
        return of(TunnelFrameType.END, sid);
    }

    public static TunnelFrame abort(String sid, int code, String msg) {
        TunnelFrame frame = of(TunnelFrameType.ABORT, sid);
        frame.setCode(code);
        frame.setMsg(msg);
        return frame;
    }

    public static TunnelFrame close(String sid, int code, String msg) {
        TunnelFrame frame = of(TunnelFrameType.CLOSE, sid);
        frame.setCode(code);
        frame.setMsg(msg);
        return frame;
    }

    public static TunnelFrame reject(int code, String msg) {
        TunnelFrame frame = new TunnelFrame(TunnelFrameType.REJECT);
        frame.setCode(code);
        frame.setMsg(msg);
        return frame;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Integer heartbeat) {
        this.heartbeat = heartbeat;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getLen() {
        return len;
    }

    public void setLen(Long len) {
        this.len = len;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public void setQuery(Map<String, String> query) {
        this.query = query;
    }

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getN() {
        return n;
    }

    public void setN(Long n) {
        this.n = n;
    }

    @Override
    public String toString() {
        return "TunnelFrame{" + type + ", sid=" + sid + ", code=" + code + ", msg=" + msg + "}";
    }
}

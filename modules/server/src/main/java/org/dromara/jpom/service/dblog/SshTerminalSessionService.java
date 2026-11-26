/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import cn.hutool.core.io.FileUtil;
import cn.hutool.db.Entity;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.log.SshTerminalSessionModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.socket.handler.record.AsciicastTerminalRecorder;
import org.dromara.jpom.socket.handler.record.TerminalRecordContext;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.common.ILoadEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH 终端会话（录制 / 回看）
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
@Slf4j
@Service
public class SshTerminalSessionService extends BaseWorkspaceService<SshTerminalSessionModel> implements ILoadEvent {

    /**
     * 在数据初始化之后执行（ILoadEvent 由 ApplicationReadyEvent 按 order 依次调用；
     * 用 @PostConstruct 会早于数据库初始化，导致清理永远失败）
     *
     * @return 顺序
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) {
        this.cleanStaleRecording();
    }

    /**
     * 启动时把上次进程异常退出（重启/崩溃）遗留的「录制中」记录标记为异常中断，避免列表长期停在录制中
     */
    public void cleanStaleRecording() {
        try {
            Entity where = Entity.create(this.getTableName());
            where.set("status", SshTerminalSessionModel.STATUS_RECORDING);
            Entity entity = Entity.create(this.getTableName());
            entity.set("status", SshTerminalSessionModel.STATUS_ABORTED);
            int count = this.update(entity, where);
            if (count > 0) {
                log.info("ssh terminal session: mark {} stale recording session(s) as aborted", count);
            }
        } catch (Exception e) {
            log.warn("ssh terminal session clean stale failed: {}", e.getMessage());
        }
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"startTime"};
    }

    /**
     * 录制文件根目录
     *
     * @return 目录
     */
    public static File recordRoot() {
        return FileUtil.file(JpomApplication.getInstance().getDataPath(), AsciicastTerminalRecorder.ROOT_DIR);
    }

    /**
     * 开始录制：先落一行「录制中」
     *
     * @param context       上下文
     * @param castFile      相对路径
     * @param recordVersion 录制器版本
     */
    public void open(TerminalRecordContext context, String castFile, int recordVersion) {
        SshTerminalSessionModel model = new SshTerminalSessionModel();
        model.setId(context.getSessionId());
        model.setWorkspaceId(context.getWorkspaceId());
        model.setUserId(context.getUserId());
        model.setSshId(context.getSshId());
        model.setSshName(context.getSshName());
        model.setMachineSshId(context.getMachineSshId());
        model.setMachineSshName(context.getMachineSshName());
        model.setCols(context.getCols());
        model.setRows(context.getRows());
        model.setStartTime(System.currentTimeMillis());
        model.setCommandCount(0);
        model.setBytes(0L);
        model.setStatus(SshTerminalSessionModel.STATUS_RECORDING);
        model.setIp(context.getIp());
        model.setUserAgent(context.getUserAgent());
        model.setCastFile(castFile);
        model.setRecordVersion(recordVersion);
        UserModel userModel = context.getUserModel();
        try {
            BaseServerController.resetInfo(userModel == null ? UserModel.EMPTY : userModel);
            this.insert(model);
        } finally {
            BaseServerController.removeAll();
        }
    }

    /**
     * 录制结束：回写统计与状态
     *
     * @param id           会话 id
     * @param endTime      结束时间
     * @param duration     时长
     * @param bytes        文件大小
     * @param commandCount 命令数
     * @param status       状态
     */
    public void finish(String id, long endTime, long duration, long bytes, int commandCount, int status) {
        if (StrUtil.isEmpty(id)) {
            return;
        }
        SshTerminalSessionModel model = new SshTerminalSessionModel();
        model.setId(id);
        model.setEndTime(endTime);
        model.setDuration(Math.max(duration, 0));
        model.setBytes(Math.max(bytes, 0));
        model.setCommandCount(commandCount);
        model.setStatus(status);
        this.updateById(model);
    }

    /**
     * 解析录制文件路径（限定在录制根目录内，防目录穿越）
     *
     * @param model 会话
     * @return 文件
     */
    public File castFile(SshTerminalSessionModel model) {
        Assert.notNull(model, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        String castFile = model.getCastFile();
        Assert.hasText(castFile, I18nMessageUtil.get("i18n.file_not_found.d952"));
        File root = recordRoot();
        File file = FileUtil.file(root, castFile);
        Assert.state(FileUtil.isSub(root, file), I18nMessageUtil.get("i18n.file_not_found.d952"));
        return file;
    }

    /**
     * 读取录制文件里的命令索引（m 事件）
     *
     * @param model 会话
     * @return 命令列表
     */
    public List<JSONObject> listCommands(SshTerminalSessionModel model) {
        List<JSONObject> result = new ArrayList<>();
        File file = this.castFile(model);
        if (!FileUtil.isFile(file)) {
            return result;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (StrUtil.isEmpty(line) || !StrUtil.startWith(line, "[")) {
                    continue;
                }
                JSONArray array = JSON.parseArray(line);
                if (array == null || array.size() < 3) {
                    continue;
                }
                if (!StrUtil.equals("m", array.getString(1))) {
                    continue;
                }
                String label = array.getString(2);
                if (StrUtil.isEmpty(label) || StrUtil.equals("truncated", label)) {
                    continue;
                }
                JSONObject marker = JSON.parseObject(label);
                if (marker == null) {
                    continue;
                }
                JSONObject item = new JSONObject();
                item.put("seq", marker.getInteger("seq"));
                item.put("time", array.getDouble(0));
                item.put("command", marker.getString("cmd"));
                item.put("exitCode", marker.getInteger("exit"));
                item.put("source", marker.getString("source"));
                result.add(item);
            }
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
        }
        return result;
    }

    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("startTime", h2DbLogStorageCount, null, model -> {
            try {
                CommandUtil.systemFastDel(this.castFile(model));
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
            }
            return true;
        });
    }
}

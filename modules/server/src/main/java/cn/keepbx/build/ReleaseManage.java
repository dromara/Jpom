package cn.keepbx.build;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.BuildHistoryLog;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.outgiving.OutGivingRun;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 发布管理
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class ReleaseManage extends BaseBuild {

    private UserModel userModel;
    private int buildId;
    private BaseBuildModule baseBuildModule;
    private File resultFile;
    private BaseBuild baseBuild;

    ReleaseManage(BuildModel buildModel, UserModel userModel, BaseBuild baseBuild) {
        super(BuildUtil.getLogFile(buildModel.getId(), buildModel.getBuildId()),
                buildModel.getId());
        this.baseBuildModule = buildModel;
        this.buildId = buildModel.getBuildId();
        this.userModel = userModel;
        this.baseBuild = baseBuild;
        this.init();
    }

    /**
     * 重新发布
     *
     * @param buildHistoryLog 构建历史
     * @param userModel       用户
     */
    public ReleaseManage(BuildHistoryLog buildHistoryLog, UserModel userModel) {
        super(BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId()),
                buildHistoryLog.getBuildDataId());
        this.baseBuildModule = buildHistoryLog;
        this.buildId = buildHistoryLog.getBuildNumberId();
        this.userModel = userModel;
        this.init();
    }


    @Override
    public boolean updateStatus(BuildModel.Status status) {
        if (baseBuild == null) {
            return super.updateStatus(status);
        } else {
            return baseBuild.updateStatus(status);
        }
    }

    private void init() {
        this.resultFile = BuildUtil.getHistoryPackageFile(this.buildModelId, this.buildId, this.baseBuildModule.getResultDirFile());
    }

    /**
     * 不修改为发布中状态
     */
    public void start2() {
        this.log("start release");
        if (!this.resultFile.exists()) {
            this.log("不存在构建产物");
            updateStatus(BuildModel.Status.PubError);
            return;
        }
        if (this.baseBuildModule.getReleaseMethod() == BuildModel.ReleaseMethod.Outgiving.getCode()) {
            //
            try {
                this.doOutGiving();
            } catch (Exception e) {
                this.pubLog("发布分发包异常", e);
                return;
            }
        } else if (this.baseBuildModule.getReleaseMethod() == BuildModel.ReleaseMethod.Project.getCode()) {
            BuildModel.AfterOpt afterOpt = BaseEnum.getEnum(BuildModel.AfterOpt.class, this.baseBuildModule.getAfterOpt());
            if (afterOpt == null) {
                afterOpt = BuildModel.AfterOpt.No;
            }
            try {
                this.doProject(afterOpt);
            } catch (Exception e) {
                this.pubLog("发布包异常", e);
                return;
            }
        }
        this.log("release end");
        updateStatus(BuildModel.Status.PubSuccess);
    }

    /**
     * 修改为发布中状态
     */
    public void start() {
        updateStatus(BuildModel.Status.PubIng);
        this.start2();
    }

    /**
     * 发布项目
     *
     * @param afterOpt 后续操作
     */
    private void doProject(BuildModel.AfterOpt afterOpt) {
        String releaseMethodDataId = this.baseBuildModule.getReleaseMethodDataId();
        String[] strings = StrUtil.split(releaseMethodDataId, ":");
        if (strings == null || strings.length != 2) {
            throw new JpomRuntimeException(releaseMethodDataId + " error");
        }
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        NodeModel nodeModel = nodeService.getItem(strings[0]);
        Objects.requireNonNull(nodeModel, "节点不存在");

        File zipFile = BuildUtil.isDirPackage(this.resultFile);
        boolean unZip = true;
        if (zipFile == null) {
            zipFile = this.resultFile;
            unZip = false;
        }
        JsonMessage jsonMessage = OutGivingRun.fileUpload(zipFile,
                strings[1],
                unZip,
                afterOpt != BuildModel.AfterOpt.No,
                nodeModel, this.userModel);
        if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
            this.log("发布项目包成功：" + jsonMessage.toString());
        } else {
            throw new JpomRuntimeException("发布项目包失败：" + jsonMessage.toString());
        }
    }

    /**
     * 分发包
     *
     * @throws IOException IO
     */
    private void doOutGiving() throws IOException {
        String releaseMethodDataId = this.baseBuildModule.getReleaseMethodDataId();
        File zipFile = BuildUtil.isDirPackage(this.resultFile);
        boolean unZip = true;
        if (zipFile == null) {
            zipFile = this.resultFile;
            unZip = false;
        }
        OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip);
        this.log("开始执行分发包啦,请到分发中查看当前状态");
    }


    /**
     * 发布异常日志
     *
     * @param title     描述
     * @param throwable 异常
     */
    private void pubLog(String title, Throwable throwable) {
        log(title, throwable, BuildModel.Status.PubError);
    }
}

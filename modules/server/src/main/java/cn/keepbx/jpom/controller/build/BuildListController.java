package cn.keepbx.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.build.BuildManage;
import cn.keepbx.build.BuildUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.*;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.build.BuildHistoryService;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.util.GitUtil;
import com.alibaba.fastjson.JSONArray;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * 构建列表
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 */
@Controller
@RequestMapping(value = "/build")
public class BuildListController extends BaseServerController {

    @Resource
    private BuildService buildService;
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private BuildHistoryService buildHistoryService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        //通知方式
        JSONArray jsonArray = BaseEnum.toJSONArray(BuildModel.Status.class);
        setAttribute("statusArray", jsonArray);
        return "build/list";
    }


    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getMonitorList() throws IOException {
        List<BuildModel> list = buildService.list();
        return JsonMessage.getString(200, "", list);
    }

    @RequestMapping(value = "updateBuild", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.EditBuild)
    public String updateMonitor(String id,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空")) String name,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.URL, msg = "仓库地址不正确")) String gitUrl,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录账号")) String userName,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录密码")) String password,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "请选择分支")) String branchName,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空")) String resultDirFile,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空")) String script,
                                @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "发布方法不正确") int releaseMethod,
                                String afterOpt) throws Exception {
        List<String> list = getBranchList(gitUrl, userName, password);
        if (!list.contains(branchName)) {
            return JsonMessage.getString(405, "没有找到对应分支：" + branchName);
        }
        BuildModel buildModel = buildService.getItem(id);
        if (buildModel == null) {
            buildModel = new BuildModel();
            buildModel.setId(IdUtil.fastSimpleUUID());
        }
        buildModel.setName(name);
        buildModel.setGitUrl(gitUrl);
        buildModel.setBranchName(branchName);
        buildModel.setPassword(password);
        buildModel.setUserName(userName);
        buildModel.setResultDirFile(resultDirFile);
        buildModel.setScript(script);
        //
        buildModel.setModifyUser(UserModel.getOptUserName(getUser()));
        //
        BuildModel.ReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildModel.ReleaseMethod.class, releaseMethod);
        if (releaseMethod1 == null) {
            return JsonMessage.getString(405, "发布方法不正确");
        }
        buildModel.setReleaseMethod(releaseMethod1.getCode());
        if (releaseMethod1 == BuildModel.ReleaseMethod.Outgiving) {
            String releaseMethodDataId = getParameter("releaseMethodDataId_1");
            if (StrUtil.isEmpty(releaseMethodDataId)) {
                return JsonMessage.getString(405, "请选择分发项目");
            }
            buildModel.setReleaseMethodDataId(releaseMethodDataId);
        } else if (releaseMethod1 == BuildModel.ReleaseMethod.Project) {
            String releaseMethodDataId2Node = getParameter("releaseMethodDataId_2_node");
            String releaseMethodDataId2Project = getParameter("releaseMethodDataId_2_project");
            if (StrUtil.isEmpty(releaseMethodDataId2Node) || StrUtil.isEmpty(releaseMethodDataId2Project)) {
                return JsonMessage.getString(405, "请选择节点和项目");
            }
            buildModel.setReleaseMethodDataId(String.format("%s:%s", releaseMethodDataId2Node, releaseMethodDataId2Project));
            //
            BuildModel.AfterOpt afterOpt1 = BaseEnum.getEnum(BuildModel.AfterOpt.class, Convert.toInt(afterOpt, 0));
            if (afterOpt1 == null) {
                return JsonMessage.getString(400, "请选择打包后的操作");
            }
            buildModel.setAfterOpt(afterOpt1.getCode());
        } else {
            buildModel.setReleaseMethodDataId(null);
        }
        if (StrUtil.isEmpty(id)) {
            buildService.addItem(buildModel);
            return JsonMessage.getString(200, "添加成功");
        }
        buildService.updateItem(buildModel);
        return JsonMessage.getString(200, "修改成功");
    }

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) throws IOException {
        BuildModel buildModel = null;
        if (StrUtil.isNotEmpty(id)) {
            buildModel = buildService.getItem(id);
        }
        setAttribute("model", buildModel);
        //
        JSONArray releaseMethods = BaseEnum.toJSONArray(BuildModel.ReleaseMethod.class);
        setAttribute("releaseMethods", releaseMethods);
        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        setAttribute("outGivingModels", outGivingModels);

        //
        List<NodeModel> nodeModels = nodeService.listAndProject();
        setAttribute("nodeModels", nodeModels);
        //
        JSONArray jsonArray = BaseEnum.toJSONArray(BuildModel.AfterOpt.class);
        setAttribute("afterOpt", jsonArray);
        //
        JSONArray outAfterOpt = BaseEnum.toJSONArray(OutGivingModel.AfterOpt.class);
        setAttribute("outAfterOpt", outAfterOpt);
        return "build/edit";
    }

    @RequestMapping(value = "branchList.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String branchList(
            @ValidatorConfig(@ValidatorItem(value = ValidatorRule.URL, msg = "仓库地址不正确")) String url,
            @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录账号")) String userName,
            @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录密码")) String userPwd) throws GitAPIException, IOException {
        List<String> list = getBranchList(url, userName, userPwd);
        return JsonMessage.getString(200, "ok", list);
    }

    private List<String> getBranchList(String url, String userName, String userPwd) throws GitAPIException, IOException {
        //  生成临时路径
        String tempId = SecureUtil.md5(url);
        File file = ConfigBean.getInstance().getTempPath();
        File gitFile = FileUtil.file(file, "gitTemp", tempId);
        List<String> list = GitUtil.branchList(url, gitFile, new UsernamePasswordCredentialsProvider(userName, userPwd));
        if (list == null || list.isEmpty()) {
            throw new JpomRuntimeException("该仓库还没有任何分支");
        }
        return list;
    }


    @RequestMapping(value = "delete.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.DelBuild)
    public String delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) throws IOException, SQLException {
        BuildModel buildModel = buildService.getItem(id);
        Objects.requireNonNull(buildModel, "没有对应数据");
        buildHistoryService.delByBuildId(buildModel.getId());
        //
        File file = BuildUtil.getBuildDataFile(buildModel.getId());
        if (!FileUtil.del(file)) {
            return JsonMessage.getString(500, "清理历史构建产物失败");
        }
        buildService.deleteItem(buildModel.getId());
        return JsonMessage.getString(200, "清理成功");
    }

}

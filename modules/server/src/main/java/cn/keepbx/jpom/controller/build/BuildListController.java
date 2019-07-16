package cn.keepbx.jpom.controller.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.build.BuildService;
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
import java.util.List;

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
    public String updateMonitor(String id,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空")) String name,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.URL, msg = "仓库地址不正确")) String gitUrl,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录账号")) String userName,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录密码")) String password,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "请选择分支")) String branchName,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空")) String resultDirFile,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空")) String script) throws Exception {
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

}

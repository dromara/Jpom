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
package org.dromara.jpom.controller.user;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.UserConfig;
import org.dromara.jpom.func.system.model.ClusterInfoModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.func.user.model.UserLoginLogModel;
import org.dromara.jpom.func.user.server.UserLoginLogServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.MailAccountModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.log.UserOperateLogV1;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.monitor.EmailUtil;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.dblog.DbUserOperateLogService;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.TwoFactorAuthUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2019/8/10
 */
@RestController
@RequestMapping(value = "/user")
@Slf4j
public class UserBasicInfoController extends BaseServerController {

    private static final TimedCache<String, Integer> CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(30));

    private final SystemParametersServer systemParametersServer;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final UserService userService;
    private final UserConfig userConfig;
    private final UserLoginLogServer userLoginLogServer;
    private final DbUserOperateLogService dbUserOperateLogService;
    private final ClusterInfoService clusterInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;

    public UserBasicInfoController(SystemParametersServer systemParametersServer,
                                   UserBindWorkspaceService userBindWorkspaceService,
                                   UserService userService,
                                   ServerConfig serverConfig,
                                   UserLoginLogServer userLoginLogServer,
                                   DbUserOperateLogService dbUserOperateLogService,
                                   ClusterInfoService clusterInfoService,
                                   DbBuildHistoryLogService dbBuildHistoryLogService) {
        this.systemParametersServer = systemParametersServer;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userService = userService;
        this.userConfig = serverConfig.getUser();
        this.userLoginLogServer = userLoginLogServer;
        this.dbUserOperateLogService = dbUserOperateLogService;
        this.clusterInfoService = clusterInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
    }


    /**
     * get user basic info
     * 获取管理员基本信息接口
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "user-basic-info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Map<String, Object>> getUserBasicInfo() {
        UserModel userModel = getUser();
        userModel = userService.getByKey(userModel.getId(), false);
        // return basic info
        Map<String, Object> map = new HashMap<>(10);
        map.put("id", userModel.getId());
        map.put("name", userModel.getName());
        map.put("systemUser", userModel.isSystemUser());
        map.put("superSystemUser", userModel.isSuperSystemUser());
        map.put("demoUser", userModel.isDemoUser());
        map.put("email", userModel.getEmail());
        map.put("dingDing", userModel.getDingDing());
        map.put("workWx", userModel.getWorkWx());
        map.put("md5Token", userModel.getPassword());
        boolean bindMfa = userService.hasBindMfa(userModel.getId());
        map.put("bindMfa", bindMfa);
        map.put("forceMfa", userConfig.isForceMfa());
        return JsonMessage.success("success", map);
    }

    @RequestMapping(value = "save_basicInfo.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> saveBasicInfo(String email,
                                              String dingDing, String workWx, String code,
                                              @ValidatorItem(value = ValidatorRule.NOT_BLANK, range = "2:10", msg = "昵称长度只能是2-10") String name) {
        UserModel user = getUser();
        UserModel userModel = userService.getByKey(user.getId());
        UserModel updateModel = new UserModel(user.getId());
        // 判断是否一样
        if (StrUtil.isNotEmpty(email) && !StrUtil.equals(email, userModel.getEmail())) {
            Validator.validateEmail(email, "邮箱格式不正确");
            Integer cacheCode = CACHE.get(email);
            if (cacheCode == null || !Objects.equals(cacheCode.toString(), code)) {
                return new JsonMessage<>(405, "请输入正确验证码");
            }
            updateModel.setEmail(email);
        }

        updateModel.setName(name);
        //
        if (StrUtil.isNotEmpty(dingDing) && !Validator.isUrl(dingDing)) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, dingDing, "请输入正确钉钉地址");
        }
        updateModel.setDingDing(dingDing);
        if (StrUtil.isNotEmpty(workWx)) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, workWx, "请输入正确企业微信地址");
        }
        updateModel.setWorkWx(workWx);
        userService.updateById(updateModel);
        return JsonMessage.success("修改成功");
    }

    /**
     * 发送邮箱验证
     *
     * @param email 邮箱
     * @return msg
     */
    @RequestMapping(value = "sendCode.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> sendCode(@ValidatorItem(value = ValidatorRule.EMAIL, msg = "邮箱格式不正确") String email) {
        MailAccountModel config = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
        Assert.notNull(config, "管理员还没有配置系统邮箱,请联系管理配置发件信息");
        int randomInt = RandomUtil.randomInt(1000, 9999);
        try {
            EmailUtil.send(email, "Jpom 验证码", "验证码是：" + randomInt);
        } catch (Exception e) {
            log.error("发送失败", e);
            return new JsonMessage<>(500, "发送邮件失败：" + e.getMessage());
        }
        CACHE.put(email, randomInt);
        return JsonMessage.success("发送成功");
    }

    /**
     * 查询用户自己的工作空间
     *
     * @return msg
     */
    @GetMapping(value = "my-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<UserWorkspaceModel>> myWorkspace() {
        UserModel user = getUser();
        List<WorkspaceModel> models = userBindWorkspaceService.listUserWorkspaceInfo(user);
        Assert.notEmpty(models, "当前账号没有绑定任何工作空间，请联系管理员处理");
        JSONObject parametersServerConfig = systemParametersServer.getConfig("user-my-workspace-" + user.getId(), JSONObject.class);
        List<UserWorkspaceModel> userWorkspaceModels = models.stream()
            .map(workspaceModel -> {
                UserWorkspaceModel userWorkspaceModel = new UserWorkspaceModel();
                userWorkspaceModel.setId(workspaceModel.getId());
                userWorkspaceModel.setName(workspaceModel.getName());
                userWorkspaceModel.setGroup(workspaceModel.getGroup());
                userWorkspaceModel.setOriginalName(workspaceModel.getName());
                userWorkspaceModel.setClusterInfoId(workspaceModel.getClusterInfoId());
                Long createTimeMillis = workspaceModel.getCreateTimeMillis();
                userWorkspaceModel.setSort((int) (ObjectUtil.defaultIfNull(createTimeMillis, 0L) / 1000L));
                return userWorkspaceModel;
            })
            .peek(userWorkspaceModel -> {
                if (parametersServerConfig == null) {
                    return;
                }
                UserWorkspaceModel userConfig = parametersServerConfig.getObject(userWorkspaceModel.getId(), UserWorkspaceModel.class);
                if (userConfig == null) {
                    return;
                }
                userWorkspaceModel.setName(StrUtil.emptyToDefault(userConfig.getName(), userWorkspaceModel.getName()));
                userWorkspaceModel.setSort(ObjectUtil.defaultIfNull(userConfig.getSort(), userWorkspaceModel.getSort()));
            })
            .sorted((o1, o2) -> CompareUtil.compare(o1.getSort(), o2.getSort()))
            .collect(Collectors.toList());
        return JsonMessage.success("", userWorkspaceModels);
    }

    /**
     * 保存用户自己的工作空间
     *
     * @return msg
     */
    @PostMapping(value = "save-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> saveWorkspace(@RequestBody List<UserWorkspaceModel> workspaceModels) {
        Assert.notEmpty(workspaceModels, "没有选择任何工作空间");
        List<UserWorkspaceModel> collect = workspaceModels.stream()
            .filter(workspaceModel -> StrUtil.isNotEmpty(workspaceModel.getId()))
            .peek(userWorkspaceModel -> userWorkspaceModel.setOriginalName(null))
            .collect(Collectors.toList());
        UserModel user = getUser();
        Map<String, UserWorkspaceModel> map = CollStreamUtil.toMap(collect, UserWorkspaceModel::getId, workspaceModel -> workspaceModel);
        systemParametersServer.upsert("user-my-workspace-" + user.getId(), map, "用户自定义工作空间");
        return JsonMessage.success("保存成功");
    }

    /**
     * 关闭自己到 mfa 相关信息
     *
     * @return json
     */
    @GetMapping(value = "close_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> closeMfa(@ValidatorItem String code) {
        UserModel user = getUser();
        boolean mfaCode = userService.verifyMfaCode(user.getId(), code);
        Assert.state(mfaCode, "验证码不正确");
        UserModel userModel = new UserModel(user.getId());
        userModel.setTwoFactorAuthKey(StrUtil.EMPTY);
        userService.updateById(userModel);
        return JsonMessage.success("关闭成功");
    }

    @GetMapping(value = "generate_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> generateMfa() {
        UserModel user = getUser();
        JSONObject jsonObject = new JSONObject();
        String tfaKey = TwoFactorAuthUtils.generateTFAKey();
        jsonObject.put("mfaKey", tfaKey);
        jsonObject.put("url", TwoFactorAuthUtils.generateOtpAuthUrl(user.getId(), tfaKey));
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 绑定 mfa
     *
     * @param mfa     mfa key
     * @param twoCode 验证码
     * @return json
     */
    @GetMapping(value = "bind_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> bindMfa(String mfa, String twoCode) {
        //
        UserModel user = getUser();
        boolean bindMfa = userService.hasBindMfa(user.getId());
        Assert.state(!bindMfa, "当前账号已经绑定 mfa 啦");
        // demo
        Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
        //
        boolean tfaCode = TwoFactorAuthUtils.validateTFACode(mfa, twoCode);
        Assert.state(tfaCode, " mfa 验证码不正确");
        userService.bindMfa(user.getId(), mfa);
        return JsonMessage.success("绑定成功");
    }

    /**
     * 登录日志列表
     *
     * @return json
     */
    @RequestMapping(value = "list-login-log-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<PageResultDto<UserLoginLogModel>> listLoginLogData(HttpServletRequest request) {
        UserModel user = getUser();
        PageResultDto<UserLoginLogModel> pageResult = userLoginLogServer.listPageByUserId(request, user.getId());
        return JsonMessage.success("", pageResult);
    }

    /**
     * 操作日志
     *
     * @return json
     */
    @RequestMapping(value = "list-operate-log-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<PageResultDto<UserOperateLogV1>> listOperateLogData(HttpServletRequest request) {
        UserModel user = getUser();
        PageResultDto<UserOperateLogV1> pageResult = dbUserOperateLogService.listPageByUserId(request, user.getId());
        return JsonMessage.success("", pageResult);
    }

    @RequestMapping(value = "recent-log-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> recentData(HttpServletRequest request) {
        UserModel user = getUser();
        JSONObject jsonObject = new JSONObject();
        {
            Entity entity = Entity.create();
            entity.set("userId", user.getId());
            List<UserOperateLogV1> operateLog = dbUserOperateLogService.queryList(entity, 10, new Order("createTimeMillis", Direction.DESC));
            jsonObject.put("operateLog", operateLog);
        }
        {
            Entity entity = Entity.create();
            entity.set("modifyUser", user.getId());
            List<UserLoginLogModel> loginLog = userLoginLogServer.queryList(entity, 10, new Order("createTimeMillis", Direction.DESC));

            jsonObject.put("loginLog", loginLog);
        }
        {
            String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(request);
            Entity entity = Entity.create();
            entity.set("workspaceId", workspaceId);
            entity.set("modifyUser", user.getId());
            List<BuildHistoryLog> loginLog = dbBuildHistoryLogService.queryList(entity, 10, new Order("createTimeMillis", Direction.DESC));
            jsonObject.put("buildLog", loginLog);
        }
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 查询集群列表
     *
     * @return json
     */
    @GetMapping(value = "cluster-list")
    public IJsonMessage<JSONObject> clusterList() {
        List<ClusterInfoModel> list = clusterInfoService.list();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("list", list);
        jsonObject.put("currentId", JpomManifest.getInstance().getInstallId());
        return JsonMessage.success("", jsonObject);
    }
}

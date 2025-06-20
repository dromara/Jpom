package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpMode;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.AssetsConfig;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.func.BaseGroupNameController;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.FtpModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.node.ftp.FtpService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author bwcx_jzy
 * @since 2024/8/31
 */
@RestController
@RequestMapping(value = "/system/assets/ftp")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_SSH)
@SystemPermission
@Slf4j
public class MachineFtpController extends BaseGroupNameController {

    private final MachineFtpServer machineFtpServer;
    private final AssetsConfig.FtpConfig ftpConfig;
    private final WorkspaceService workspaceService;
    private final FtpService ftpService;
    private final ServerConfig serverConfig;


    public MachineFtpController(MachineFtpServer machineFtpServer, AssetsConfig assetsConfig, WorkspaceService workspaceService, FtpService ftpService, ServerConfig serverConfig) {
        super(machineFtpServer);
        this.machineFtpServer = machineFtpServer;
        this.ftpConfig = assetsConfig.getFtp();
        this.workspaceService = workspaceService;
        this.ftpService = ftpService;
        this.serverConfig = serverConfig;

    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<MachineFtpModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineFtpModel> pageResultDto = machineFtpServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    @Override
    @GetMapping(value = "list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Collection<String>> listGroup() {
        Collection<String> list = dbService.listGroupName();
        // 合并配置禁用分组
        List<String> monitorGroupName = ftpConfig.getDisableMonitorGroupName();
        if (monitorGroupName != null) {
            list.addAll(monitorGroupName);
            //
            list.remove("*");
            list = new HashSet<>(list);
        }
        return JsonMessage.success("", list);
    }

    /**
     * 编辑
     *
     * @param name               名称
     * @param host               主机
     * @param user               用户名
     * @param password           密码
     * @param serverLanguageCode 服务器语言
     * @param systemKey          服务器系统关键词
     * @param port               端口
     * @param charset            编码格式
     * @param id                 ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_ssh_name_cannot_be_empty.ff4f") String name,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.host_cannot_be_empty.644a") String host,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_user_cannot_be_empty.9239") String user,
                                     String password,
                                     @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.parameter_error_port_error.810d") int port,
                                     String charset,
                                     String id,
                                     Integer timeout,
                                     String allowEditSuffix,
                                     String serverLanguageCode,
                                     String systemKey,
                                     String mode,
                                     String groupName) {
        boolean add = StrUtil.isEmpty(id);
        if (add) {
            Assert.hasText(password, I18nMessageUtil.get("i18n.login_password_required.9605"));
        } else {
            boolean exists = machineFtpServer.exists(new MachineFtpModel(id));
            Assert.state(exists, I18nMessageUtil.get("i18n.ftp_not_exist.f9b3"));
        }

        MachineFtpModel model = new MachineFtpModel();
        model.setId(id);
        model.setGroupName(groupName);
        model.setHost(host);
        model.setServerLanguageCode(serverLanguageCode);
        model.setMode(mode);
        model.setSystemKey(systemKey);
        // 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
        Opt.ofBlankAble(password).ifPresent(model::setPassword);

        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, I18nMessageUtil.get("i18n.suffix_cannot_be_empty.ec72"));
        model.allowEditSuffix(allowEditSuffixList);
        model.setPort(port);
        model.setUser(user);
        model.setName(name);
        model.setTimeout(timeout);
        try {
            Charset.forName(charset);
            model.setCharset(charset);
        } catch (Exception e) {
            return new JsonMessage<>(405, I18nMessageUtil.get("i18n.correct_encoding_format_required.1f7f") + e.getMessage());
        }
        // 判断重复
        Entity entity = Entity.create();
        entity.set("host", model.getHost());
        entity.set("port", model.getPort());
        entity.set(DialectUtil.wrapField("user"), model.getUser());
        Opt.ofBlankAble(id).ifPresent(s -> entity.set("id", StrUtil.format(" <> {}", s)));
        boolean exists = machineFtpServer.exists(entity);
        Assert.state(!exists, I18nMessageUtil.get("i18n.ftp_already_exists.d66b"));


        MachineFtpModel byKey = machineFtpServer.getByKey(id,false);
        Optional.ofNullable(byKey).ifPresent(item -> {
            model.setPassword(StrUtil.emptyToDefault(model.getPassword(), item.getPassword()));
        });

        // 测试连接
        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(model),
            EnumUtil.fromString(FtpMode.class, mode, FtpMode.Active)))  {
            ftp.pwd();
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.ftp_connection_failed.1f2f"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.ftp_connection_failed_message.bd99") + e.getMessage());
        }

        model.setStatus(1);
        int i = add ? machineFtpServer.insert(model) : machineFtpServer.updateById(model);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    @GetMapping(value = "list-workspace-ftp", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<FtpModel>> listWorkspaceFtp(@ValidatorItem String id) {
        MachineFtpModel machineFtpModel = machineFtpServer.getByKey(id);
        Assert.notNull(machineFtpModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
        FtpModel ftpModel = new FtpModel();
        ftpModel.setMachineFtpId(id);
        List<FtpModel> modelList = ftpService.listByBean(ftpModel);
        modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
        for (FtpModel model : modelList) {
            model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
        }
        return JsonMessage.success("", modelList);
    }

    /**
     * 将 ftp 分配到指定工作空间
     *
     * @param ids         ftp id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String id : list) {
            MachineFtpModel machineFtpModel = machineFtpServer.getByKey(id);
            Assert.notNull(machineFtpModel, I18nMessageUtil.get("i18n.no_ftp_correspondence.23c4"));
            boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
            Assert.state(exists, I18nMessageUtil.get("i18n.workspace_not_exist.a6fd"));
            if (!ftpService.existsFtp2(workspaceId, id)) {
                ftpService.insert(machineFtpModel, workspaceId);
            }
        }

        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }


    /**
     * 保存工作空间配置
     *
     * @param fileDirs 文件夹
     * @param id       ID
     * @return json
     */
    @PostMapping(value = "save-workspace-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> saveWorkspaceConfig(
        String fileDirs,
        @ValidatorItem String id,
        String allowEditSuffix) {
        FtpModel ftpModel = new FtpModel(id);
        // 目录
        if (StrUtil.isEmpty(fileDirs)) {
            ftpModel.fileDirs(null);
        } else {
            List<String> list = StrSplitter.splitTrim(fileDirs, StrUtil.LF, true);
            UserModel userModel = getUser();
            Assert.state(!userModel.isDemoUser(), PermissionInterceptor.DEMO_TIP);
            ftpModel.fileDirs(list);
        }
        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, I18nMessageUtil.get("i18n.suffix_cannot_be_empty.ec72"));
        ftpModel.allowEditSuffix(allowEditSuffixList);
        ftpService.updateById(ftpModel);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * edit
     *
     * @param id ssh id
     * @return json
     */
    @PostMapping(value = "rest-hide-field", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> restHideField(@ValidatorItem String id) {
        MachineFtpModel machineFtpModel = new MachineFtpModel();
        machineFtpModel.setId(id);
        machineFtpModel.setPassword(StrUtil.EMPTY);
        machineFtpServer.updateById(machineFtpModel);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 下载导入模板
     */
    @GetMapping(value = "import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String prefix = I18nMessageUtil.get("i18n.ftp_import_template.8fa3");
        String fileName = prefix + ".csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("name", "groupName", "host", "port", "user", "password", "serverLanguageCode", "systemKey", "charset", "mode", "timeout");
        writer.flush();
    }

    /**
     * 导出数据
     */
    @GetMapping(value = "export-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void exportData(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String prefix = I18nMessageUtil.get("i18n.exported_ftp_data.2b54");
        String fileName = prefix + DateTime.now().toString(DatePattern.NORM_DATE_FORMAT) + ".csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        int pageInt = 0;
        writer.writeLine("name", "groupName", "host", "port", "user", "password", "serverLanguageCode", "systemKey", "charset", "mode", "timeout");
        while (true) {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            paramMap.remove("workspaceId");
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<MachineFtpModel> listPage = machineFtpServer.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<MachineFtpModel, List<Object>>) machineSshModel -> CollUtil.newArrayList(
                    machineSshModel.getName(),
                    machineSshModel.getGroupName(),
                    machineSshModel.getHost(),
                    machineSshModel.getPort(),
                    machineSshModel.getUser(),
                    machineSshModel.getPassword(),
                    machineSshModel.getCharset(),
                    machineSshModel.getMode(),
                    machineSshModel.getTimeout()
                ))
                .map(objects -> objects.stream().map(StrUtil::toStringOrNull).toArray(String[]::new))
                .forEach(writer::writeLine);
            if (ObjectUtil.equal(listPage.getPage(), listPage.getTotalPage())) {
                // 最后一页
                break;
            }
        }
        writer.flush();
    }

    /**
     * 导入数据
     *
     * @return json
     */
    @PostMapping(value = "import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> importData(MultipartFile file) throws IOException {
        Assert.notNull(file, I18nMessageUtil.get("i18n.no_uploaded_file.07ef"));
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean csv = StrUtil.endWithIgnoreCase(extName, "csv");
        Assert.state(csv, I18nMessageUtil.get("i18n.disallowed_file_format.d6e4"));
        assert originalFilename != null;
        File csvFile = FileUtil.file(serverConfig.getUserTempPath(), originalFilename);
        int addCount = 0, updateCount = 0;
        Charset fileCharset;
        try {
            file.transferTo(csvFile);
            fileCharset = CharsetDetector.detect(csvFile);
            Reader bomReader = FileUtil.getReader(csvFile, fileCharset);
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
            CsvData csvData;
            try {
                csvData = reader.read();
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.parse_csv_exception.885e"), e);
                return new JsonMessage<>(405, I18nMessageUtil.get("i18n.parse_file_exception.374d") + e.getMessage());
            } finally {
                IoUtil.close(reader);
            }
            List<CsvRow> rows = csvData.getRows();
            Assert.notEmpty(rows, I18nMessageUtil.get("i18n.no_data.55a2"));

            for (int i = 0; i < rows.size(); i++) {
                CsvRow csvRow = rows.get(i);
                String name = csvRow.getByName("name");
                int finalI = i;
                Assert.hasText(name, () -> StrUtil.format(I18nMessageUtil.get("i18n.name_field_required.e0c5"), finalI + 1));
                String groupName = csvRow.getByName("groupName");
                String host = csvRow.getByName("host");
                Assert.hasText(host, () -> StrUtil.format(I18nMessageUtil.get("i18n.host_field_required.5c36"), finalI + 1));
                Integer port = Convert.toInt(csvRow.getByName("port"));
                Assert.state(port != null && NetUtil.isValidPort(port), () -> StrUtil.format(I18nMessageUtil.get("i18n.port_field_required_or_incorrect.8426"), finalI + 1));
                String user = csvRow.getByName("user");
                Assert.hasText(host, () -> StrUtil.format(I18nMessageUtil.get("i18n.user_field_required.8732"), finalI + 1));
                String password = csvRow.getByName("password");
                String charset = csvRow.getByName("charset");
                //
                String mode = csvRow.getByName("mode");
                FtpMode ftpMode = EnumUtil.fromString(FtpMode.class, mode, FtpMode.Active);
                mode = ftpMode.name();
                String serverLanguageCode = csvRow.getByName("serverLanguageCode");
                String systemKey = csvRow.getByName("systemKey");

                Integer timeout = Convert.toInt(csvRow.getByName("timeout"));
                //
                MachineFtpModel where = new MachineFtpModel();
                where.setHost(host);
                where.setUser(user);
                where.setPort(port);
                where.setMode(mode);
                MachineFtpModel machineFtpModel = machineFtpServer.queryByBean(where);
                if (machineFtpModel == null) {
                    // 添加
                    where.setName(name);
                    where.setGroupName(groupName);
                    where.setPassword(password);
                    where.setMode(mode);
                    where.setTimeout(timeout);
                    where.setCharset(charset);
                    where.setServerLanguageCode(serverLanguageCode);
                    where.setSystemKey(systemKey);
                    machineFtpServer.insert(where);
                    addCount++;
                } else {
                    MachineFtpModel update = new MachineFtpModel();
                    update.setId(machineFtpModel.getId());
                    update.setName(name);
                    update.setGroupName(groupName);
                    update.setPassword(password);
                    update.setMode(mode);
                    update.setTimeout(timeout);
                    update.setCharset(charset);
                    where.setServerLanguageCode(serverLanguageCode);
                    where.setSystemKey(systemKey);
                    machineFtpServer.updateById(update);
                    updateCount++;
                }
            }
        } finally {
            FileUtil.del(csvFile);
        }
        String fileCharsetStr = Optional.ofNullable(fileCharset).map(Charset::name).orElse(StrUtil.EMPTY);
        return JsonMessage.success(I18nMessageUtil.get("i18n.import_success_with_details.a4a0"), fileCharsetStr, addCount, updateCount);
    }


}

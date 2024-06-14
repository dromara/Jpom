/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.cert.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.cert.model.CertificateInfoModel;
import org.dromara.jpom.func.cert.service.CertificateInfoService;
import org.dromara.jpom.func.files.service.FileReleaseTaskService;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.*;

/**
 * @author bwcx_jzy
 * @since 2023/3/22
 */
@RestController
@RequestMapping(value = "/certificate/")
@Feature(cls = ClassFeature.CERTIFICATE_INFO)
@Slf4j
public class CertificateInfoController extends BaseServerController {


    private final ServerConfig serverConfig;
    private final MachineDockerServer machineDockerServer;
    private final CertificateInfoService certificateInfoService;
    private final FileReleaseTaskService fileReleaseTaskService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final FileStorageService fileStorageService;

    public CertificateInfoController(ServerConfig serverConfig,
                                     MachineDockerServer machineDockerServer,
                                     CertificateInfoService certificateInfoService,
                                     FileReleaseTaskService fileReleaseTaskService,
                                     OutGivingWhitelistService outGivingWhitelistService,
                                     FileStorageService fileStorageService) {
        this.serverConfig = serverConfig;
        this.machineDockerServer = machineDockerServer;
        this.certificateInfoService = certificateInfoService;
        this.fileReleaseTaskService = fileReleaseTaskService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<CertificateInfoModel>> list(HttpServletRequest request) {
        //
        PageResultDto<CertificateInfoModel> listPage = certificateInfoService.listPage(request);
        listPage.each(certificateInfoModel -> {
            File file = certificateInfoService.getFilePath(certificateInfoModel);
            certificateInfoModel.setFileExists(!FileUtil.isEmpty(file));
        });
        return JsonMessage.success("", listPage);
    }

    /**
     * 查询所有分页列表
     *
     * @return json
     */
    @PostMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    @SystemPermission
    public IJsonMessage<PageResultDto<CertificateInfoModel>> listAll(HttpServletRequest request) {
        //
        PageResultDto<CertificateInfoModel> listPage = certificateInfoService.listPageAll(request);
        listPage.each(certificateInfoModel -> {
            File file = certificateInfoService.getFilePath(certificateInfoModel);
            certificateInfoModel.setFileExists(!FileUtil.isEmpty(file));
        });
        return JsonMessage.success("", listPage);
    }

    @PostMapping(value = "import-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> importFile(MultipartFile file, @ValidatorItem String type, String password) {
        Assert.notNull(file, I18nMessageUtil.get("i18n.no_uploaded_file.07ef"));
        String filename = file.getOriginalFilename();
        Assert.notNull(filename, I18nMessageUtil.get("i18n.file_name_not_found.b0ed"));
        File tempPath = FileUtil.file(serverConfig.getUserTempPath(), "cert", IdUtil.fastSimpleUUID());
        CertificateInfoModel certificateInfoModel;
        try {
            switch (type) {
                case KeyUtil.KEY_TYPE_PKCS12:
                case KeyUtil.KEY_TYPE_JKS:
                    certificateInfoModel = this.resolvePkcs12OrJks(file, password, tempPath, type);
                    break;
                case KeyUtil.CERT_TYPE_X509:
                    certificateInfoModel = this.resolveX509(file, tempPath);
                    break;
                default:
                    throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_mode_with_colon.c6de") + type);
            }
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        } finally {
            FileUtil.file(tempPath);
        }
        certificateInfoService.insert(certificateInfoModel);
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }


    /**
     * 解析 x509 证书
     *
     * @param multipartFile 上传的文件
     * @param tempPath      临时保存目录
     * @return 证书对象
     * @throws IOException io
     */
    private CertificateInfoModel resolveX509(MultipartFile multipartFile,
                                             File tempPath) throws IOException {
        FileUtil.mkdir(tempPath);
        String filename = multipartFile.getOriginalFilename();
        String extName = FileUtil.extName(filename);
        Assert.state(StrUtil.containsIgnoreCase(extName, "zip"), I18nMessageUtil.get("i18n.invalid_file_type.7246"));
        File saveFile = FileUtil.file(tempPath, filename);
        multipartFile.transferTo(saveFile);
        ZipUtil.unzip(saveFile, tempPath);
        return certificateInfoService.resolveX509(tempPath, true);
    }

    /**
     * 解析 pfx /jks 证书
     *
     * @param multipartFile 上传的文件
     * @param password      密码
     * @param tempPath      临时保存目录
     * @return 证书对象
     * @throws IOException io
     */
    private CertificateInfoModel resolvePkcs12OrJks(MultipartFile multipartFile,
                                                    String password, File tempPath,
                                                    String type) throws IOException {
        String suffix;
        switch (type) {
            case KeyUtil.KEY_TYPE_JKS:
                suffix = "jks";
                break;
            case KeyUtil.KEY_TYPE_PKCS12:
            default:
                suffix = "pfx";
                break;
        }
        FileUtil.mkdir(tempPath);
        String filename = multipartFile.getOriginalFilename();
        String extName = FileUtil.extName(filename);
        File saveFile = FileUtil.file(tempPath, filename);
        File pfxFile = null;
        String newPassword = password;
        FileUtil.del(saveFile);
        if (StrUtil.equalsIgnoreCase(extName, suffix)) {
            multipartFile.transferTo(saveFile);
            pfxFile = saveFile;
        } else if (StrUtil.equalsIgnoreCase(extName, "zip")) {
            multipartFile.transferTo(saveFile);
            ZipUtil.unzip(saveFile, tempPath);
            // 找到 suffix
            File[] files = tempPath.listFiles();
            Assert.notEmpty(files, I18nMessageUtil.get("i18n.no_files_in_zip.1af6"));
            for (File file1 : files) {
                String extName2 = FileUtil.extName(file1);
                if (pfxFile == null && StrUtil.equalsIgnoreCase(extName2, suffix)) {
                    pfxFile = file1;
                }
                if (StrUtil.isEmpty(newPassword) && StrUtil.equalsIgnoreCase(extName2, "txt")) {
                    newPassword = FileUtil.readString(file1, CharsetUtil.CHARSET_UTF_8);
                }
            }
        } else {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.file_format_not_supported.eac4"));
        }
        Assert.notNull(pfxFile, StrUtil.format(I18nMessageUtil.get("i18n.no_file_found.6f1b"), suffix));
        try {
            char[] passwordChars = StrUtil.emptyToDefault(newPassword, StrUtil.EMPTY).toCharArray();
            KeyStore keyStore = StrUtil.equals(suffix, "jks") ? KeyUtil.readJKSKeyStore(pfxFile, passwordChars) : KeyUtil.readPKCS12KeyStore(pfxFile, passwordChars);
            Enumeration<String> aliases = keyStore.aliases();
            // we are readin just one certificate.
            if (aliases.hasMoreElements()) {
                //
                String keyAlias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(keyAlias);
                PrivateKey prikey = (PrivateKey) keyStore.getKey(keyAlias, passwordChars);
                PublicKey pubkey = certificate.getPublicKey();
                certificateInfoService.testKey(pubkey, prikey);
                //
                X509Certificate cert = X509Certificate.getInstance(certificate.getEncoded());
                // 填充
                CertificateInfoModel certificateInfoModel = certificateInfoService.filling(cert);
                certificateInfoModel.setKeyType(keyStore.getType());
                certificateInfoModel.setKeyAlias(keyAlias);
                // 判断是否存在
                Assert.state(!certificateInfoService.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType()),
                    I18nMessageUtil.get("i18n.certificate_already_exists.adf9"));
                //certificateInfoService.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType());

                certificateInfoModel.setCertPassword(newPassword);
                //
                File file1 = certificateInfoService.getFilePath(certificateInfoModel);
                FileUtil.mkdir(file1);
                // 避免文件夹已经存在
                FileUtil.clean(file1);
                FileUtil.move(pfxFile, file1, true);
                return certificateInfoModel;
            } else {
                throw new IllegalStateException(I18nMessageUtil.get("i18n.certificate_has_no_aliases.3a2f"));
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw Lombok.sneakyThrow(e);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.parse_certificate_exception.3b6c"), e);
            throw new IllegalStateException(I18nMessageUtil.get("i18n.parse_certificate_unknown_error.c43c") + e.getMessage());
        }
    }


    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> del(@ValidatorItem String id, HttpServletRequest request) throws IOException {
        CertificateInfoModel model = certificateInfoService.getByKeyAndGlobal(id, request);
        // 判断是否被 docker 使用
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setCertInfo(model.getSerialNumberStr() + StrUtil.COLON + model.getKeyType());
        long count = machineDockerServer.count(machineDockerModel);
        Assert.state(count == 0, I18nMessageUtil.get("i18n.certificate_in_use_by_docker.dd63"));
        //
        File file = certificateInfoService.getFilePath(model);
        FileUtil.del(file);
        if (FileUtil.isEmpty(file.getParentFile())) {
            // 一并删除避免保留空文件夹
            FileUtil.del(file.getParentFile());
        }
        //
        certificateInfoService.delByKey(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> edit(@ValidatorItem String id,
                                     String description,
                                     HttpServletRequest request) throws IOException {
        // 验证权限
        certificateInfoService.getByKeyAndGlobal(id, request);

        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setId(id);
        certificateInfoModel.setDescription(description);
        //
        certificateInfoModel.setWorkspaceId(certificateInfoService.covertGlobalWorkspace(request));
        certificateInfoService.updateById(certificateInfoModel);
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

    /**
     * 导出证书
     *
     * @param id 项目id
     */
    @GetMapping(value = "export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void export(@ValidatorItem String id, HttpServletRequest request, HttpServletResponse response) {
        CertificateInfoModel model = certificateInfoService.getByKeyAndGlobal(id, request);
        File file = certificateInfoService.getFilePath(model);
        Assert.state(!FileUtil.isEmpty(file), I18nMessageUtil.get("i18n.certificate_file_missing.c663"));

        File userTempPath = serverConfig.getUserTempPath();
        File tempSave = FileUtil.file(userTempPath, IdUtil.fastSimpleUUID());
        try {
            FileUtil.mkdir(tempSave);
            String absolutePath = FileUtil.file(tempSave, model.getSerialNumberStr() + ".zip").getAbsolutePath();
            File zip = ZipUtil.zip(file.getAbsolutePath(), absolutePath, false);
            ServletUtil.write(response, zip);
        } finally {
            FileUtil.del(tempSave);
        }
    }

    @PostMapping(value = "deploy", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> addTask(@ValidatorItem String id,
                                        @ValidatorItem String name,
                                        @ValidatorItem(value = ValidatorRule.NUMBERS) int taskType,
                                        @ValidatorItem String taskDataIds,
                                        @ValidatorItem String releasePathParent,
                                        @ValidatorItem String releasePathSecondary,
                                        String beforeScript,
                                        String afterScript,
                                        HttpServletRequest request) {
        // 判断参数
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, releasePathParent), I18nMessageUtil.get("i18n.select_correct_project_path_or_no_auth_configured.366a"));
        Assert.hasText(releasePathSecondary, I18nMessageUtil.get("i18n.publish_file_second_level_directory_required.2f65"));
        // 判断证书是否存在
        CertificateInfoModel model = certificateInfoService.getByKeyAndGlobal(id, request);
        File file = certificateInfoService.getFilePath(model);
        Assert.state(!FileUtil.isEmpty(file), I18nMessageUtil.get("i18n.certificate_file_missing.c663"));
        File userTempPath = serverConfig.getUserTempPath();
        File tempSave = FileUtil.file(userTempPath, IdUtil.fastSimpleUUID());
        try {
            // 压缩成 zip
            FileUtil.mkdir(tempSave);
            String absolutePath = FileUtil.file(tempSave, model.getSerialNumberStr() + ".zip").getAbsolutePath();
            File zip = ZipUtil.zip(file.getAbsolutePath(), absolutePath, false);
            // 添加到文件中心
            String description = model.getSerialNumberStr() + Optional.ofNullable(model.getDescription()).map(s -> "," + s).orElse(StrUtil.EMPTY);
            String fileId = fileStorageService.addFile(zip, 3, certificateInfoService.getCheckUserWorkspace(request), description, null, 1);
            String releasePath = FileUtil.normalize(releasePathParent + StrUtil.SLASH + releasePathSecondary);
            // 创建发布任务
            Map<String, String> env = new HashMap<>();
            env.put("CERT_SERIAL_NUMBER_STR", model.getSerialNumberStr());
            return fileReleaseTaskService.addTask(fileId, 1, name, taskType, taskDataIds, releasePath, beforeScript, afterScript, env, request);
        } finally {
            FileUtil.del(tempSave);
        }
    }
}

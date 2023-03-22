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
package io.jpom.func.cert.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.GlobalBouncyCastleProvider;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.func.cert.model.CertificateInfoModel;
import io.jpom.func.cert.service.CertificateInfoService;
import io.jpom.model.user.UserModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.system.ServerConfig;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.jpom.model.PageResultDto;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/22
 */
@RestController
@RequestMapping(value = "/certificate/")
@Feature(cls = ClassFeature.CERTIFICATE_INFO)
@Slf4j
public class CertificateInfoController extends BaseServerController {

    static {
        GlobalBouncyCastleProvider.setUseBouncyCastle(false);
    }

    private final ServerConfig serverConfig;
    private final JpomApplication jpomApplication;
    private final CertificateInfoService certificateInfoService;

    public CertificateInfoController(ServerConfig serverConfig,
                                     JpomApplication jpomApplication,
                                     CertificateInfoService certificateInfoService) {
        this.serverConfig = serverConfig;
        this.jpomApplication = jpomApplication;
        this.certificateInfoService = certificateInfoService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<CertificateInfoModel>> list(HttpServletRequest request) {
        File certificatePath = FileUtil.file(jpomApplication.getDataPath(), "certificate");
        //
        PageResultDto<CertificateInfoModel> listPage = certificateInfoService.listPage(request);
        listPage.each(certificateInfoModel -> {

        });
        return JsonMessage.success("", listPage);
    }

    @PostMapping(value = "import-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public JsonMessage<String> importFile(MultipartFile file, @ValidatorItem String type, String password) {
        Assert.notNull(file, "没有上传文件");
        String filename = file.getOriginalFilename();
        Assert.notNull(filename, "没有文件名");
        File tempPath = FileUtil.file(serverConfig.getUserTempPath(), "cert", IdUtil.fastSimpleUUID());
        File certificatePath = FileUtil.file(jpomApplication.getDataPath(), "certificate");
        CertificateInfoModel certificateInfoModel;
        try {
            switch (type) {
                case KeyUtil.KEY_TYPE_PKCS12:
                case KeyUtil.KEY_TYPE_JKS:
                    certificateInfoModel = this.resolvePkcs12OrJks(file, password, tempPath, certificatePath, type);
                    break;
                case KeyUtil.CERT_TYPE_X509:
                    certificateInfoModel = this.resolveX509(file, tempPath, certificatePath);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的模式：" + type);
            }
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        } finally {
            FileUtil.file(tempPath);
        }
        certificateInfoService.insert(certificateInfoModel);
        return JsonMessage.success("上传成功");
    }

    /**
     * 判断证书是否存在
     *
     * @param serialNumber 证书编号
     * @param type         证书类型
     */
    private void checkRepeat(String serialNumber, String type) {
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setSerialNumberStr(serialNumber);
        certificateInfoModel.setKeyType(type);
        Assert.state(!certificateInfoService.exists(certificateInfoModel), "当前证书已经存在啦(系统全局范围内)");
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
                                             File tempPath,
                                             File certificatePath) throws IOException {
        FileUtil.mkdir(tempPath);
        String filename = multipartFile.getOriginalFilename();
        String extName = FileUtil.extName(filename);
        Assert.state(StrUtil.containsIgnoreCase(extName, "zip"), "上传的文件不是 zip");
        File saveFile = FileUtil.file(tempPath, filename);
        multipartFile.transferTo(saveFile);
        ZipUtil.unzip(saveFile, tempPath);
        String[] keyNameSuffixes = new String[]{"key.pem", ".key"};
        String[] pemNameSuffixes = new String[]{".crt", ".cer", ".pem"};
        // 找到 对应的文件
        File[] files = tempPath.listFiles();
        Assert.notNull(files, "压缩包里没有任何文件");
        File keyFile = Arrays.stream(files).filter(file -> StrUtil.endWithAnyIgnoreCase(file.getName(), keyNameSuffixes)).findAny().orElse(null);
        Assert.notNull(keyFile, "没有包里没有找到私钥文件");
        //
        try {
            List<File> fileList = Arrays.stream(files)
                    .filter(file -> !FileUtil.equals(file, keyFile))
                    .filter(file -> StrUtil.endWithAnyIgnoreCase(file.getName(), pemNameSuffixes))
                    .collect(Collectors.toList());
            Assert.notEmpty(fileList, "没有找到任何证书文件");
            Assert.state(fileList.size() <= 2, "找到 2 个以上的证书文件");
            //
            List<Certificate> certificates = fileList.stream()
                    .map(file -> {
                        try (BufferedInputStream inputStream = FileUtil.getInputStream(file)) {
                            return KeyUtil.readX509Certificate(inputStream);
                        } catch (Exception e) {
                            throw Lombok.sneakyThrow(e);
                        }
                    })
                    .collect(Collectors.toList());
            Certificate certificate0 = certificates.get(0);
            Certificate certificate1 = CollUtil.get(certificates, 1);
            X509Certificate x509Certificate0 = X509Certificate.getInstance(certificate0.getEncoded());
            X509Certificate x509Certificate1 = certificate1 != null ? X509Certificate.getInstance(certificate1.getEncoded()) : null;
            Principal issuerDN = x509Certificate0.getIssuerDN();
            Principal subjectDN = x509Certificate0.getSubjectDN();
            Assert.state(issuerDN != null && subjectDN != null, "证书信息出现错误,未找到 issuerDN 或者 subjectDN");
            int rootIndex = StrUtil.equals(issuerDN.getName(), subjectDN.getName()) ? 0 : 1;
            //
            PrivateKey privateKey;
            try (BufferedInputStream inputStream = FileUtil.getInputStream(keyFile)) {
                privateKey = PemUtil.readPemPrivateKey(inputStream);
            }
            // 验证证书公钥和私钥
            PublicKey publicKey = Optional.ofNullable(rootIndex == 0 ? certificate1 : certificate0)
                    .map(Certificate::getPublicKey)
                    .orElse(null);
            this.testKey(publicKey, privateKey);
            //
            X509Certificate pubCert = (rootIndex == 0 ? x509Certificate1 : x509Certificate0);
            if (certificate1 != null) {
                // 验证证书链
                pubCert.verify((rootIndex == 0 ? certificate0 : certificate1).getPublicKey());
            }
            // 填充
            CertificateInfoModel certificateInfoModel = this.filling(pubCert);
            // 类型已经确定
            certificateInfoModel.setKeyType(certificate0.getType());
            // 判断是否存在
            this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType());
            // 保存文件
            File file1 = this.getFilePath(certificateInfoModel);
            FileUtil.mkdir(file1);
            // 避免文件夹已经存在
            FileUtil.clean(file1);
            FileUtil.move(keyFile, file1, true);
            for (File file : fileList) {
                FileUtil.move(file, file1, true);
            }
            return certificateInfoModel;
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw Lombok.sneakyThrow(e);
        } catch (Exception e) {
            log.error("解析证书异常", e);
            throw new IllegalStateException("解析证书发生未知错误：" + e.getMessage());
        }

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
                                                    File certificatePath,
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
            Assert.notEmpty(files, "压缩包里没有任何文件");
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
            throw new IllegalArgumentException("不支持的文件格式");
        }
        Assert.notNull(pfxFile, "没有找到 " + suffix + " 文件");
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
                this.testKey(pubkey, prikey);
                //
                X509Certificate cert = X509Certificate.getInstance(certificate.getEncoded());
                // 填充
                CertificateInfoModel certificateInfoModel = this.filling(cert);
                certificateInfoModel.setKeyType(keyStore.getType());
                certificateInfoModel.setKeyAlias(keyAlias);
                // 判断是否存在
                this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType());

                certificateInfoModel.setCertPassword(newPassword);
                //
                File file1 = this.getFilePath(certificateInfoModel);
                FileUtil.mkdir(file1);
                // 避免文件夹已经存在
                FileUtil.clean(file1);
                FileUtil.move(pfxFile, file1, true);
                return certificateInfoModel;
            } else {
                throw new IllegalStateException("证书没有任何：aliases");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw Lombok.sneakyThrow(e);
        } catch (Exception e) {
            log.error("解析证书异常", e);
            throw new IllegalStateException("解析证书发生未知错误：" + e.getMessage());
        }
    }

    /**
     * 获取证书信息
     *
     * @param cert 证书公钥
     * @return data
     */
    private CertificateInfoModel filling(X509Certificate cert) {
        
        String algorithm = cert.getPublicKey().getAlgorithm();
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        Date notBefore = cert.getNotBefore();
        Date notAfter = cert.getNotAfter();
        Optional.ofNullable(notAfter).ifPresent(date -> certificateInfoModel.setExpirationTime(date.getTime()));
        Optional.ofNullable(notBefore).ifPresent(date -> certificateInfoModel.setEffectiveTime(date.getTime()));
        BigInteger serialNumber = cert.getSerialNumber();
        // 使用 16 进制
        certificateInfoModel.setSerialNumberStr(serialNumber.toString(16));
        //
        int version = cert.getVersion();
        certificateInfoModel.setCertVersion(version);
        Optional.ofNullable(cert.getSubjectDN()).ifPresent(principal -> certificateInfoModel.setSubjectDnName(principal.getName()));
        Optional.ofNullable(cert.getIssuerDN()).ifPresent(principal -> certificateInfoModel.setIssuerDnName(principal.getName()));
        String sigAlgOID = cert.getSigAlgOID();
        String sigAlgName = cert.getSigAlgName();
        certificateInfoModel.setSigAlgName(sigAlgName);
        certificateInfoModel.setSigAlgOid(sigAlgOID);
        return certificateInfoModel;
    }

    /**
     * 验证 公钥和私钥是否匹配
     *
     * @param pubkey     公钥
     * @param privateKey 私钥
     */
    private void testKey(PublicKey pubkey, PrivateKey privateKey) {
        Assert.state(pubkey != null && privateKey != null, "公钥或者私钥不存在");
        RSA rsa = new RSA(privateKey, pubkey);
        // 测试字符串
        String str = "您好，Jpom";
        String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
        String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
        Assert.state(!StrUtil.equals(encryptStr, decryptStr), "公钥和私钥不匹配");
    }

    private File getFilePath(CertificateInfoModel model) {
        File certificatePath = FileUtil.file(jpomApplication.getDataPath(), "certificate");
        return FileUtil.file(certificatePath, model.getSerialNumberStr(), model.getKeyType());
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> del(@ValidatorItem String id, HttpServletRequest request) throws IOException {
        CertificateInfoModel model = certificateInfoService.getByKey(id, request);
        Assert.notNull(model, "不存在对应的证书");
        UserModel user = getUser();
        if (!user.isSystemUser()) {
            // 不是管理员，需要验证是自己上传的文件
            Assert.state(StrUtil.equals(model.getCreateUser(), user.getId()), "当前证书创建人不是您,不能删除证书信息");
        }
        //
        File file = this.getFilePath(model);
        FileUtil.del(file);
        if (FileUtil.isEmpty(file.getParentFile())) {
            // 一并删除避免保留空文件夹
            FileUtil.del(file.getParentFile());
        }
        //
        certificateInfoService.delByKey(id);
        return JsonMessage.success("删除成功");
    }

    /**
     * 导出证书
     *
     * @param id 项目id
     */
    @RequestMapping(value = "export", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void export(@ValidatorItem String id, HttpServletRequest request, HttpServletResponse response) {
        CertificateInfoModel model = certificateInfoService.getByKey(id, request);
        Assert.notNull(model, "不存在对应的证书");
        File file = this.getFilePath(model);
        Assert.state(!FileUtil.isEmpty(file), "证书文件丢失");

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
}

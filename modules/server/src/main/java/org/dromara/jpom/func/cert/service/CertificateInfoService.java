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
package org.dromara.jpom.func.cert.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.GlobalBouncyCastleProvider;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.cert.model.CertificateInfoModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseGlobalOrWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.StandardCopyOption;
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
@Service
@Slf4j
public class CertificateInfoService extends BaseGlobalOrWorkspaceService<CertificateInfoModel> implements IStatusRecover {

    static {
        GlobalBouncyCastleProvider.setUseBouncyCastle(false);
    }

    private final JpomApplication jpomApplication;
    private final MachineDockerServer machineDockerServer;

    public CertificateInfoService(JpomApplication jpomApplication,
                                  MachineDockerServer machineDockerServer) {
        this.jpomApplication = jpomApplication;
        this.machineDockerServer = machineDockerServer;
    }

    @Override
    public int statusRecover() {
        Entity entity = Entity.create();
        entity.set("tlsVerify", true);
        entity.set("certInfo", null);
        List<MachineDockerModel> dockerModels = machineDockerServer.listByEntity(entity);
        if (CollUtil.isEmpty(dockerModels)) {
            return 0;
        }
        for (MachineDockerModel dockerModel : dockerModels) {
            try {
                String generateCertPath = dockerModel.generateCertPath();
                File file = FileUtil.file(generateCertPath);
                CertificateInfoModel certificateInfoModel = this.resolveX509(file, false);
                if (!this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType())) {
                    certificateInfoModel.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
                    certificateInfoModel.setCreateUser(UserModel.SYSTEM_ADMIN);
                    certificateInfoModel.setModifyUser(UserModel.SYSTEM_ADMIN);
                    certificateInfoModel.setDescription("docker[" + dockerModel.getName() + "] 资产导入");
                    this.insert(certificateInfoModel);
                }
                // 更新
                MachineDockerModel update = new MachineDockerModel();
                update.setId(dockerModel.getId());
                update.setCertInfo(certificateInfoModel.getSerialNumberStr() + StrUtil.COLON + certificateInfoModel.getKeyType());
                machineDockerServer.updateById(update);
                log.info("docker[{}] 证书成功迁移到证书管理中", dockerModel.getName());
            } catch (Exception e) {
                log.error("迁移 docker[{}] 证书发生异常", dockerModel.getName(), e);
            }
        }
        return CollUtil.size(dockerModels);
    }

    /**
     * 解析 x509 证书
     *
     * @param dir         证书目录
     * @param checkRepeat 是否判断重复
     * @return 证书对象
     */
    public CertificateInfoModel resolveX509(File dir, boolean checkRepeat) {
        String[] keyNameSuffixes = new String[]{"key.pem", ".key"};
        String[] pemNameSuffixes = new String[]{".crt", ".cer", ".pem"};
        // 找到 对应的文件
        File[] files = dir.listFiles();
        Assert.notNull(files, "压缩包里没有任何文件");
        File keyFile = Arrays.stream(files).filter(file -> StrUtil.endWithAnyIgnoreCase(file.getName(), keyNameSuffixes)).findAny().orElse(null);
        Assert.notNull(keyFile, "压缩包里没有找到私钥文件");
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
            if (checkRepeat) {
                //                this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType());
                Assert.state(!this.checkRepeat(certificateInfoModel.getSerialNumberStr(), certificateInfoModel.getKeyType()),
                    "当前证书已经存在啦(系统全局范围内)");
            }
            // 保存文件
            File file1 = this.getFilePath(certificateInfoModel);
            FileUtil.mkdir(file1);
            // 避免文件夹已经存在
            FileUtil.clean(file1);
            FileUtil.copyFile(keyFile, file1, StandardCopyOption.REPLACE_EXISTING);
            for (File file : fileList) {
                FileUtil.copyFile(file, file1, StandardCopyOption.REPLACE_EXISTING);
            }
            return certificateInfoModel;
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw Lombok.sneakyThrow(e);
        } catch (Exception e) {
            log.error("解析证书异常", e);
            throw new IllegalStateException("解析证书发生未知错误：" + e.getMessage());
        }
    }

    public File getFilePath(CertificateInfoModel model) {
        File certificatePath = FileUtil.file(jpomApplication.getDataPath(), "certificate");
        return FileUtil.file(certificatePath, model.getSerialNumberStr(), model.getKeyType());
    }

    public File getFilePath(String certTag) {
        CertificateInfoModel byCertTag = this.getByCertTag(certTag);
        if (byCertTag == null) {
            return null;
        }
        return this.getFilePath(byCertTag);
    }

    /**
     * 判断证书是否存在
     *
     * @param serialNumber 证书编号
     * @param type         证书类型
     */
    public boolean checkRepeat(String serialNumber, String type) {
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setSerialNumberStr(serialNumber);
        certificateInfoModel.setKeyType(type);
        return this.exists(certificateInfoModel);
    }

    /**
     * 查询证书
     *
     * @param certTag 证书标记
     */
    public CertificateInfoModel getByCertTag(String certTag) {
        if (StrUtil.isEmpty(certTag)) {
            return null;
        }
        List<String> list = StrUtil.splitTrim(certTag, StrUtil.COLON);
        String serialNumberStr = CollUtil.get(list, 0);
        String keyType = CollUtil.get(list, 1);
        Assert.hasText(serialNumberStr, "没有证书序列号");
        Assert.hasText(keyType, "没有证书类型");
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        certificateInfoModel.setSerialNumberStr(serialNumberStr);
        certificateInfoModel.setKeyType(keyType);
        List<CertificateInfoModel> infoModels = this.listByBean(certificateInfoModel);
        return CollUtil.getFirst(infoModels);
    }

    /**
     * 验证 公钥和私钥是否匹配
     *
     * @param pubkey     公钥
     * @param privateKey 私钥
     */
    public void testKey(PublicKey pubkey, PrivateKey privateKey) {
        Assert.state(pubkey != null && privateKey != null, "公钥或者私钥不存在");
        RSA rsa = new RSA(privateKey, pubkey);
        // 测试字符串
        String str = "您好，Jpom";
        String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
        String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
        Assert.state(!StrUtil.equals(encryptStr, decryptStr), "公钥和私钥不匹配");
    }

    /**
     * 获取证书信息
     *
     * @param cert 证书公钥
     * @return data
     */
    public CertificateInfoModel filling(X509Certificate cert) throws CertificateEncodingException {

        //String algorithm = cert.getPublicKey().getAlgorithm();
        CertificateInfoModel certificateInfoModel = new CertificateInfoModel();
        Date notBefore = cert.getNotBefore();
        Date notAfter = cert.getNotAfter();
        Optional.ofNullable(notAfter).ifPresent(date -> certificateInfoModel.setExpirationTime(date.getTime()));
        Optional.ofNullable(notBefore).ifPresent(date -> certificateInfoModel.setEffectiveTime(date.getTime()));
        BigInteger serialNumber = cert.getSerialNumber();
        // 使用 16 进制
        certificateInfoModel.setSerialNumberStr(serialNumber.toString(16));
        byte[] encoded = cert.getEncoded();
        certificateInfoModel.setFingerprint(SecureUtil.sha1().digestHex(encoded));
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

    public PageResultDto<CertificateInfoModel> listPageAll(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        paramMap.remove("workspaceId");
        return super.listPage(paramMap);
    }
}

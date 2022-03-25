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
package io.jpom.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.*;

import java.lang.reflect.Method;

/**
 * hutool 默认封装的 SSH 工具，仅支持传入 SSL Private Key File，不支持  Private Key Content。
 * <p>
 * 本实现用于直接采用 Private Key Content 登录。
 *
 * <pre>
 *
 * Created by zhenqin.
 * User: zhenqin
 * Date: 2022/3/25
 * Time: 下午2:56
 * Email: zhzhenqin@163.com
 *
 * </pre>
 *
 * @author zhenqin
 */
public class JschUtils {


    public final static String HEADER = "-----BEGIN RSA PRIVATE KEY-----";

    public final static String FOOTER = "-----END RSA PRIVATE KEY-----";

    /**
     * GETKEYTYPENAME_METHOD getKeyTypeName 是私有的
     *
     * @see KeyPair#getKeyTypeName
     */
    private static final Method GET_KEY_TYPE_NAME_METHOD = ReflectUtil.getMethod(KeyPair.class, "getKeyTypeName");

    static class ContentIdentity implements Identity {

        private KeyPair kpair;
        private final String identity;


        static ContentIdentity newInstance(byte[] prvContent, byte[] pubContent, String username, JSch jsch) throws Exception {
            KeyPair kpair = KeyPair.load(jsch, prvContent, pubContent);
            return new ContentIdentity(username, kpair);
        }

        private ContentIdentity(String name, KeyPair kpair) {
            this.identity = name;
            this.kpair = kpair;
        }


        /**
         * Decrypts this identity with the specified pass-phrase.
         *
         * @param passphrase the pass-phrase for this identity.
         * @return <tt>true</tt> if the decryption is succeeded
         * or this identity is not cyphered.
         */
        @Override
        public boolean setPassphrase(byte[] passphrase) {
            return kpair.decrypt(passphrase);
        }

        /**
         * Returns the public-key blob.
         *
         * @return the public-key blob
         */
        @Override
        public byte[] getPublicKeyBlob() {
            return kpair.getPublicKeyBlob();
        }

        /**
         * Signs on data with this identity, and returns the result.
         *
         * @param data data to be signed
         * @return the signature
         */
        @Override
        public byte[] getSignature(byte[] data) {
            return kpair.getSignature(data);
        }

        /**
         * @see #setPassphrase(byte[] passphrase)
         * @deprecated This method should not be invoked.
         */
        @Override
        public boolean decrypt() {
            throw new RuntimeException("not implemented");
        }

        /**
         * Returns the name of the key algorithm.
         *
         * @return "ssh-rsa" or "ssh-dss"
         */
        @Override
        public String getAlgName() {
            try {
                byte[] name = ReflectUtil.invoke(kpair, GET_KEY_TYPE_NAME_METHOD);
                return new String(name, CharsetUtil.CHARSET_UTF_8);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Returns the name of this identity.
         * It will be useful to identify this object in the {@link IdentityRepository}.
         */
        @Override
        public String getName() {
            return identity;
        }

        /**
         * Returns <tt>true</tt> if this identity is cyphered.
         *
         * @return <tt>true</tt> if this identity is cyphered.
         */
        @Override
        public boolean isEncrypted() {
            return kpair.isEncrypted();
        }

        /**
         * Disposes internally allocated data, like byte array for the private key.
         */
        @Override
        public void clear() {
            kpair.dispose();
            kpair = null;
        }

        /**
         * Returns an instance of {@link KeyPair} used in this {@link Identity}.
         *
         * @return an instance of {@link KeyPair} used in this {@link Identity}.
         */
        public KeyPair getKeyPair() {
            return kpair;
        }
    }


    /**
     * 通过私钥获取 Session
     *
     * @param sshHost    ssh 目标节点IP、域名、机器名
     * @param sshPort    ssh 服务端口号，一般为 22
     * @param sshUser    ssh 目标节点登录用户
     * @param privateKey 采用的私钥，一般由 ssh-keygen 生成。必须包含完整的前后缀：-----BEGIN RSA PRIVATE KEY-----
     * @param passphrase 密码
     * @return Session
     */
    public static Session createSession(String sshHost, int sshPort, String sshUser, String privateKey, byte[] passphrase) {
        final JSch jsch = new JSch();
        try {
            byte[] privateKeyByte = StrUtil.bytes(privateKey);
            Identity identity = ContentIdentity.newInstance(privateKeyByte, null, sshUser, jsch);
            jsch.addIdentity(identity, passphrase);
        } catch (Exception e) {
            throw new JschRuntimeException(e);
        }
        return JschUtil.createSession(jsch, sshHost, sshPort, sshUser);
    }
}

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
package com.jinhill.pki;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

import java.io.InputStream;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertUtil {
	static {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	public static String getSubjectDN(InputStream bIn) {
		String dn = "";
		try {

//            BouncyCastleProvider provider = new BouncyCastleProvider();
//            CertificateFactory cf = CertificateFactory.getInstance("X509",
//                    provider);
			CertificateFactory cf = CertificateFactory.getInstance("X.509",
					"SUN");
			//android 需采用bcprov
//            CertificateFactory cf = CertificateFactory.getInstance("X.509",
//                    "BC");
			X509Certificate cert = (X509Certificate) cf
					.generateCertificate(bIn);
			dn = cert.getSubjectDN().getName();
			bIn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dn;
	}

	public static String parseCertDN(String dn, String type) {
		type = type + "=";
		String[] split = dn.split(StrUtil.COMMA);
		for (String x : split) {
			if (x.contains(type)) {
				x = x.trim();
				return x.substring(type.length());
			}
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		InputStream inputStream = ResourceUtil.getStream("D:\\jpom\\agent\\data\\temp\\系统管理员\\sdfasdf\\example.com.csr");
		getSubjectDN(inputStream);
	}
}

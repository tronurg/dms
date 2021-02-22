package com.ogya.dms.passgen;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class DmsPassGen {

	private static final char[] PASSWORD = "dms".toCharArray();

	public static void main(String[] args) {

		try (OutputStream outputStream = Files.newOutputStream(Paths.get("./dms.p12"))) {

			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(null, null);

			// Secret key
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			Key secretKey = keyGen.generateKey();
			keyStore.setKeyEntry("secret", secretKey, PASSWORD, null);
			//

			// Private key and certificate
			CertAndKeyGen certAndKeyGen = new CertAndKeyGen("RSA", "SHA256WithRSA");
			certAndKeyGen.generate(1024);
			Key privateKey = certAndKeyGen.getPrivateKey();
			X509Certificate cert = certAndKeyGen.getSelfCertificate(new X500Name("CN=ROOT"),
					Long.MAX_VALUE /* unlimited certificate */);
			X509Certificate[] chain = new X509Certificate[] { cert };
			keyStore.setKeyEntry("private", privateKey, PASSWORD, chain);
			//

			keyStore.store(outputStream, PASSWORD);

			outputStream.flush();

		} catch (Exception e) {

		}

	}

}

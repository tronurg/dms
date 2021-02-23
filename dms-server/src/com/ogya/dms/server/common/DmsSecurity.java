package com.ogya.dms.server.common;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

public class DmsSecurity {

	private static final char[] PASSWORD = "dms".toCharArray();
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static KeyStore keyStore;

	private static SSLContext sslContext;

	public static byte[] encrypt(byte[] data) throws Exception {

		byte[] iv = new byte[12];
		new SecureRandom().nextBytes(iv);

		Cipher encryptor = Cipher.getInstance(TRANSFORMATION);
		encryptor.init(Cipher.ENCRYPT_MODE, getKeyStore().getKey("secret", PASSWORD), new GCMParameterSpec(128, iv));

		byte[] ciphertext = encryptor.doFinal(data);

		return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();

	}

	public static byte[] decrypt(byte[] data, int length) throws Exception {

		byte[] iv = new byte[12];
		byte[] ciphertext = new byte[length - 12];

		ByteBuffer.wrap(data).get(iv).get(ciphertext);

		Cipher decryptor = Cipher.getInstance(TRANSFORMATION);
		decryptor.init(Cipher.DECRYPT_MODE, getKeyStore().getKey("secret", PASSWORD), new GCMParameterSpec(128, iv));

		return decryptor.doFinal(ciphertext);

	}

	public static ServerSocket newSecureServerSocket(int port) throws Exception {

		return getSSLContext().getServerSocketFactory().createServerSocket(port);

	}

	public static Socket newSecureSocket(InetAddress serverIp, int serverPort, InetAddress localIp, int localPort)
			throws Exception {

		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(serverIp, serverPort, localIp,
				localPort);
		sslSocket.startHandshake();

		return sslSocket;

	}

	private synchronized static KeyStore getKeyStore() throws Exception {

		if (keyStore == null) {

			try (InputStream inputStream = Files.newInputStream(Paths.get("./sec/dms.p12"))) {

				keyStore = KeyStore.getInstance("PKCS12");
				keyStore.load(inputStream, PASSWORD);

			} catch (Exception e) {

				keyStore = null;

				throw e;

			}

		}

		return keyStore;

	}

	private synchronized static SSLContext getSSLContext() throws Exception {

		if (sslContext == null) {

			try {

				KeyStore keyStore = getKeyStore();

				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
				keyManagerFactory.init(keyStore, PASSWORD);

				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
				trustManagerFactory.init(keyStore);

				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			} catch (Exception e) {

				sslContext = null;

				throw e;

			}

		}

		return sslContext;

	}

}

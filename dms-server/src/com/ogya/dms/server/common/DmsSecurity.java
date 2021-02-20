package com.ogya.dms.server.common;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

public class DmsSecurity {

	private static final char[] PASSWORD = "dms".toCharArray();
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static KeyStore keyStore;

	private static Cipher encryptor;
	private static Cipher decryptor;

	private static SSLContext sslContext;

	public static byte[] encrypt(byte[] data) throws Exception {

		return getEncryptor().doFinal(data);

	}

	public static byte[] decrypt(byte[] data, int length) throws Exception {

		return getDecryptor().doFinal(data, 0, length);

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

	private synchronized static Cipher getEncryptor() throws Exception {

		if (encryptor == null) {

			try {

				encryptor = Cipher.getInstance(TRANSFORMATION);
				encryptor.init(Cipher.ENCRYPT_MODE, getKeyStore().getKey("secret", PASSWORD));

			} catch (Exception e) {

				encryptor = null;

				throw e;

			}

		}

		return encryptor;

	}

	private synchronized static Cipher getDecryptor() throws Exception {

		if (decryptor == null) {

			try {

				decryptor = Cipher.getInstance(TRANSFORMATION);
				decryptor.init(Cipher.DECRYPT_MODE, getKeyStore().getKey("secret", PASSWORD));

			} catch (Exception e) {

				decryptor = null;

				throw e;

			}

		}

		return decryptor;

	}

	private synchronized static KeyStore getKeyStore() throws Exception {

		if (keyStore == null) {

			try (InputStream inputStream = Files.newInputStream(Paths.get("./sec/dms.p12"))) {

				KeyStore keyStore = KeyStore.getInstance("PKCS12");
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

				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			} catch (Exception e) {

				sslContext = null;

				throw e;

			}

		}

		return sslContext;

	}

}

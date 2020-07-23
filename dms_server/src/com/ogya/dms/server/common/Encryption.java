package com.ogya.dms.server.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Base64;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.config.TinkConfig;

public class Encryption {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	private static Aead aead;

	public static String encryptToString(String dataStr) throws GeneralSecurityException, IOException {

		return Base64.getEncoder().encodeToString(encrypt(dataStr));

	}

	public static byte[] encrypt(String dataStr) throws GeneralSecurityException, IOException {

		return getAead().encrypt(dataStr.getBytes(CHARSET), null);

	}

	public static String decryptFromString(String encryptedDataStr) throws GeneralSecurityException, IOException {

		return decrypt(Base64.getDecoder().decode(encryptedDataStr));

	}

	public static String decrypt(byte[] encryptedData) throws GeneralSecurityException, IOException {

		return new String(getAead().decrypt(encryptedData, null), CHARSET);

	}

	private static Aead getAead() throws GeneralSecurityException, IOException {

		if (aead == null) {

			TinkConfig.register();

			KeysetHandle keysetHandle = CleartextKeysetHandle
					.read(JsonKeysetReader.withFile(new File("./sec/dms.key")));

			aead = keysetHandle.getPrimitive(Aead.class);

		}

		return aead;

	}

}

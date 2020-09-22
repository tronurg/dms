package com.ogya.dms.server.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import com.github.luben.zstd.Zstd;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.config.TinkConfig;

public class Encryption {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static Aead aead;

	public static String compressAndEncryptToString(String dataStr) throws GeneralSecurityException, IOException {

		return Base64.getEncoder().encodeToString(encryptBytes(Zstd.compress(dataStr.getBytes(CHARSET))));

	}

	public static byte[] encrypt(String dataStr) throws GeneralSecurityException, IOException {

		return encryptBytes(dataStr.getBytes(CHARSET));

	}

	public static String decryptAndDecompressFromString(String encryptedDataStr)
			throws GeneralSecurityException, IOException {

		byte[] compressedData = decryptBytes(Base64.getDecoder().decode(encryptedDataStr));

		return new String(Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData)), CHARSET);

	}

	public static String decrypt(byte[] encryptedData) throws GeneralSecurityException, IOException {

		return new String(decryptBytes(encryptedData), CHARSET);

	}

	private synchronized static byte[] encryptBytes(byte[] data) throws GeneralSecurityException, IOException {

		return getAead().encrypt(data, null);

	}

	private synchronized static byte[] decryptBytes(byte[] encryptedData) throws GeneralSecurityException, IOException {

		return getAead().decrypt(encryptedData, null);

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

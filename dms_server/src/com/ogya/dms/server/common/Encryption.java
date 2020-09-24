package com.ogya.dms.server.common;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

	public static String compressAndEncryptToString(String dataStr) throws Exception {

		return Base64.getEncoder().encodeToString(encryptBytes(Zstd.compress(dataStr.getBytes(CHARSET))));

	}

	public static byte[] encrypt(String dataStr) throws Exception {

		return encryptBytes(dataStr.getBytes(CHARSET));

	}

	public static String decryptAndDecompressFromString(String encryptedDataStr) throws Exception {

		byte[] compressedData = decryptBytes(Base64.getDecoder().decode(encryptedDataStr));

		return new String(Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData)), CHARSET);

	}

	public static String decrypt(byte[] encryptedData) throws Exception {

		return new String(decryptBytes(encryptedData), CHARSET);

	}

	private synchronized static byte[] encryptBytes(byte[] data) throws Exception {

		return getAead().encrypt(data, null);

	}

	private synchronized static byte[] decryptBytes(byte[] encryptedData) throws Exception {

		return getAead().decrypt(encryptedData, null);

	}

	private static Aead getAead() throws Exception {

		if (aead == null) {

			TinkConfig.register();

			KeysetHandle keysetHandle = CleartextKeysetHandle
					.read(JsonKeysetReader.withFile(new File("./sec/dms.key")));

			aead = keysetHandle.getPrimitive(Aead.class);

		}

		return aead;

	}

}

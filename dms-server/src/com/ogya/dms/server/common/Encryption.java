package com.ogya.dms.server.common;

import java.io.File;

import com.github.luben.zstd.Zstd;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.config.TinkConfig;

public class Encryption {

	private static KeysetHandle keysetHandle;
	private static Aead aead;
	private static StreamingAead streamingAead;

	public static byte[] compressAndEncrypt(byte[] data) throws Exception {

		return encrypt(Zstd.compress(data));

	}

	public static byte[] encrypt(byte[] data) throws Exception {

		return getAead().encrypt(data, null);

	}

	public static byte[] decryptAndDecompress(byte[] encryptedData) throws Exception {

		byte[] compressedData = decrypt(encryptedData);

		return Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData));

	}

	public static byte[] decrypt(byte[] encryptedData) throws Exception {

		return getAead().decrypt(encryptedData, null);

	}

	private static Aead getAead() throws Exception {

		if (aead == null) {

			TinkConfig.register();

			aead = getKeysetHandle().getPrimitive(Aead.class);

		}

		return aead;

	}

	private static StreamingAead getStreamingAead() throws Exception {

		if (streamingAead == null) {

			TinkConfig.register();

			streamingAead = getKeysetHandle().getPrimitive(StreamingAead.class);

		}

		return streamingAead;

	}

	private static KeysetHandle getKeysetHandle() throws Exception {

		if (keysetHandle == null) {

			keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(new File("./sec/dms.key")));

		}

		return keysetHandle;

	}

}

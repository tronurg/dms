package com.ogya.dms.server.common;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.config.TinkConfig;

public class Encryption {

	private static Aead aead;

	public static byte[] encrypt(byte[] data) throws GeneralSecurityException, IOException {

		return getAead().encrypt(data, null);

	}

	public static byte[] decrypt(byte[] encryptedData) throws GeneralSecurityException, IOException {

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

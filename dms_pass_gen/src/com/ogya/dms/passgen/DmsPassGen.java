package com.ogya.dms.passgen;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.config.TinkConfig;

public class DmsPassGen {

	public static void main(String[] args) {

		try {

			TinkConfig.register();

			KeysetHandle keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);

			CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(new File("dms.key")));

		} catch (GeneralSecurityException | IOException e) {

		}

	}

}

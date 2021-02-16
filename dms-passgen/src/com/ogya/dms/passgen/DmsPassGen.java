package com.ogya.dms.passgen;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.streamingaead.AesCtrHmacStreamingKeyManager;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;

public class DmsPassGen {

	public static void main(String[] args) {

		try {

			StreamingAeadConfig.register();

			KeysetHandle keysetHandle = KeysetHandle
					.generateNew(AesCtrHmacStreamingKeyManager.aes128CtrHmacSha2564KBTemplate());

			CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(new File("dms.key")));

		} catch (GeneralSecurityException | IOException e) {

		}

	}

}

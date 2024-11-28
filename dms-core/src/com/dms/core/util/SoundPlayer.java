package com.dms.core.util;

import java.io.BufferedInputStream;
import java.util.concurrent.ExecutorService;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import com.dms.core.factory.DmsFactory;

public class SoundPlayer {

	private Clip duoToneClip;
	private Clip triToneClip;

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutor();

	public void playDuoTone() {

		taskQueue.execute(() -> {

			Clip duoToneClip = getDuoToneClip();
			Clip triToneClip = getTriToneClip();

			if (triToneClip != null) {
				triToneClip.stop();
			}

			if (duoToneClip != null) {
				duoToneClip.setFramePosition(0);
				duoToneClip.start();
			}

		});

	}

	public void playTriTone() {

		taskQueue.execute(() -> {

			Clip duoToneClip = getDuoToneClip();
			Clip triToneClip = getTriToneClip();

			if (duoToneClip != null) {
				duoToneClip.stop();
			}

			if (triToneClip != null) {
				triToneClip.setFramePosition(0);
				triToneClip.start();
			}

		});

	}

	public void close() {
		taskQueue.shutdown();
	}

	private Clip getDuoToneClip() {

		if (duoToneClip == null || !duoToneClip.isOpen()) {

			try {

				duoToneClip = AudioSystem.getClip();
				duoToneClip.open(AudioSystem.getAudioInputStream(
						new BufferedInputStream(getClass().getResourceAsStream("/resources/sound/duotone.wav"))));

			} catch (Exception e) {

				e.printStackTrace();

				duoToneClip = null;

			}

		}

		return duoToneClip;

	}

	private Clip getTriToneClip() {

		if (triToneClip == null || !triToneClip.isOpen()) {

			try {

				triToneClip = AudioSystem.getClip();
				triToneClip.open(AudioSystem.getAudioInputStream(
						new BufferedInputStream(getClass().getResourceAsStream("/resources/sound/tritone.wav"))));

			} catch (Exception e) {

				e.printStackTrace();

				triToneClip = null;

			}

		}

		return triToneClip;

	}

}

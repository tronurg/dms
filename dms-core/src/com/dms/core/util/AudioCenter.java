package com.dms.core.util;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFileFormat.Type;

import com.dms.core.database.tables.EntityId;
import com.dms.core.factory.DmsFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioCenter {

	private final AudioCenterListener listener;

	private final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 8000, 8, 1, 1, 8000,
			true);

	private final AtomicReference<TargetDataLine> targetLineRef = new AtomicReference<TargetDataLine>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutor();

	public AudioCenter(AudioCenterListener listener) {
		this.listener = listener;
	}

	public void prepareRecording() throws Exception {

		stopRecording();

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

		if (!AudioSystem.isLineSupported(info)) {
			throw new Exception("Capture device not found!");
		}

		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(audioFormat);

		targetLineRef.set(line);

	}

	public void startRecording(final EntityId entityId, final Path path, final Long refId) throws Exception {

		final TargetDataLine line = targetLineRef.get();

		if (line == null) {
			throw new Exception("Audio line not initialized!");
		}

		taskQueue.execute(() -> {

			line.start();

			try (AudioInputStream audioInputStream = new AudioInputStream(line)) {

				AudioSystem.write(audioInputStream, Type.WAVE, path.toFile());

			} catch (Exception e) {

				stopRecording();

				e.printStackTrace();

			}

			listener.recordingStopped(entityId, path, refId);

		});

	}

	public void stopRecording() {

		TargetDataLine line = targetLineRef.getAndSet(null);

		if (line == null) {
			return;
		}

		line.stop();
		line.drain();
		line.close();

	}

	public void close() {
		taskQueue.shutdown();
	}

	public static interface AudioCenterListener {

		void recordingStopped(EntityId entityId, Path path, Long refId);

	}

}

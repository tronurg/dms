package com.ogya.dms.core.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.ogya.dms.core.factory.DmsFactory;

public class AudioCenter {

	private final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 8000, 8, 1, 1, 8000,
			true);

	private final AtomicReference<TargetDataLine> targetLineRef = new AtomicReference<TargetDataLine>();

	private final List<AudioCenterListener> listeners = Collections
			.synchronizedList(new ArrayList<AudioCenterListener>());

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	public AudioCenter() {

	}

	public void addAudioCenterListener(AudioCenterListener listener) {

		listeners.add(listener);

	}

	public void prepareRecording() throws Exception {

		targetLineRef.set(null);

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

		if (!AudioSystem.isLineSupported(info))
			throw new Exception("Capture device not found!");

		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(audioFormat);

		targetLineRef.set(line);

	}

	public void startRecording(final Long id, final Path path, final Long refId) throws Exception {

		final TargetDataLine line = targetLineRef.get();

		if (line == null)
			throw new Exception("Audio line not initialized!");

		taskQueue.execute(() -> {

			line.start();

			try (AudioInputStream audioInputStream = new AudioInputStream(line)) {

				AudioSystem.write(audioInputStream, Type.WAVE, path.toFile());

			} catch (Exception e) {

				stopRecording();

				e.printStackTrace();

			}

			listeners.forEach(listener -> listener.recordingStopped(id, path, refId));

		});

	}

	public void stopRecording() {

		TargetDataLine line = targetLineRef.getAndSet(null);

		if (line == null)
			return;

		line.stop();
		line.drain();
		line.close();

	}

	public static interface AudioCenterListener {

		void recordingStopped(Long id, Path path, Long refId);

	}

}

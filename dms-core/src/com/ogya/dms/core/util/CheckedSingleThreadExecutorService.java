package com.ogya.dms.core.util;

import java.util.concurrent.ExecutorService;

import com.ogya.dms.core.factory.DmsFactory;

public class CheckedSingleThreadExecutorService {

	private final ExecutorService executorService = DmsFactory.newSingleThreadExecutor();

	public CheckedSingleThreadExecutorService() {
		super();
	}

	public final void execute(Runnable r) {
		executorService.execute(() -> {
			try {
				r.run();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
	}

	public final void shutdown() {
		executorService.shutdown();
	}

}

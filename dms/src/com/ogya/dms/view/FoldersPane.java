package com.ogya.dms.view;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class FoldersPane extends BorderPane {

	private static final double GAP = 5.0;

	private final HBox topPane = new HBox(2 * GAP);
	private final StackPane centerPane = new StackPane();

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn = ViewFactory.newBackBtn();
	private final Label nameLabel = new Label(CommonMethods.translate("FILE_EXPLORER"));

	private final Map<Path, FolderView> folderViews = Collections.synchronizedMap(new HashMap<Path, FolderView>());

	private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();

	private final Map<WatchKey, Path> watchKeys = Collections.synchronizedMap(new HashMap<WatchKey, Path>());

	private WatchService watchService;

	FoldersPane(Path mainPath) {

		super();

		try {
			watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Thread watchThread = new Thread(this::processWatchServiceEvents);
		watchThread.setDaemon(true);
		watchThread.start();

		FolderView mainFolderView = newFolderView(mainPath, false);
		folderViews.put(mainPath, mainFolderView);
		centerPane.getChildren().add(mainFolderView);

		init();

	}

	private void init() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(GAP));

		topPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.setFitToWidth(true);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0));

		topPane.getChildren().addAll(backBtn, nameLabel);

		setTop(topPane);
		setCenter(scrollPane);

	}

	void setOnFileSelected(Consumer<Path> fileSelectedAction) {

		fileSelectedActionRef.set(fileSelectedAction);

	}

	void setOnBackAction(final Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void reset() {

		while (centerPane.getChildren().size() > 1)
			back();

	}

	private FolderView getSubFolderView(Path subFolder) {

		if (!folderViews.containsKey(subFolder)) {

			FolderView folderView = newFolderView(subFolder, true);

			folderViews.put(subFolder, folderView);

		}

		return folderViews.get(subFolder);

	}

	private FolderView newFolderView(Path folder, boolean isBackActionEnabled) {

		registerFolder(folder);

		FolderView folderView = new FolderView(folder, isBackActionEnabled);

		folderView.setOnFolderSelectedAction(this::folderSelected);
		folderView.setOnFileSelectedAction(this::fileSelected);
		if (isBackActionEnabled)
			folderView.setOnBackAction(this::back);

		return folderView;

	}

	private void removeFolderView(Path path) {

		FolderView folderView = folderViews.remove(path);

		if (folderView == null)
			return;

		centerPane.getChildren().remove(folderView);

		centerPane.getChildren().get(centerPane.getChildren().size() - 1).setVisible(true);

	}

	private void folderSelected(Path folder) {

		int size = centerPane.getChildren().size();

		centerPane.getChildren().get(size - 1).setVisible(false);

		centerPane.getChildren().add(getSubFolderView(folder));

	}

	private void fileSelected(Path file) {

		Consumer<Path> fileSelectedAction = fileSelectedActionRef.get();

		if (fileSelectedAction != null)
			fileSelectedAction.accept(file);

	}

	private void back() {

		int size = centerPane.getChildren().size();

		centerPane.getChildren().remove(size - 1);

		centerPane.getChildren().get(size - 2).setVisible(true);

	}

	private void registerFolder(Path folder) {

		if (watchService == null)
			return;

		try {

			watchKeys.put(folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), folder);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void processWatchServiceEvents() {

		if (watchService == null)
			return;

		while (true) {

			try {

				WatchKey watchKey = watchService.take();

				Path dir = watchKeys.get(watchKey);

				if (dir == null)
					continue;

				FolderView folderView = folderViews.get(dir);

				for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {

					Kind<?> kind = watchEvent.kind();

					if (kind.equals(StandardWatchEventKinds.OVERFLOW))
						continue;

					if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE))
						Platform.runLater(() -> removeFolderView(dir.resolve((Path) watchEvent.context())));

					if (folderView != null)
						Platform.runLater(() -> folderView.populate());

				}

				if (!watchKey.reset())
					watchKeys.remove(watchKey);

			} catch (InterruptedException e) {

				e.printStackTrace();

			}

		}

	}

	private static class CustomButton extends Button {

		private static Background whiteBackground = new Background(
				new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
		private static Background greyBackground = new Background(
				new BackgroundFill(Color.LIGHTGREY, CornerRadii.EMPTY, Insets.EMPTY));

		private CustomButton(String arg0, ImageView arg1) {

			super(arg0, arg1);

			setMaxWidth(Double.MAX_VALUE);

			setAlignment(Pos.CENTER_LEFT);
			setPadding(new Insets(5));
			setGraphicTextGap(5.0);

			backgroundProperty().bind(Bindings.createObjectBinding(() -> {

				if (parentProperty().get() == null)
					return whiteBackground;

				if (parentProperty().get().getChildrenUnmodifiable().indexOf(this) % 2 == 0)
					return whiteBackground;

				return greyBackground;

			}, parentProperty()));

		}

	}

	private static class FolderView extends VBox {

		private static final String FOLDER_ICO_PATH = "/resources/icon/folder.png";
		private static final String FILE_ICO_PATH = "/resources/icon/file.png";

		private final Path mainFolder;
		private final boolean isBackActionEnabled;

		private final AtomicReference<Consumer<Path>> folderSelectedActionRef = new AtomicReference<Consumer<Path>>();
		private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();
		private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

		FolderView(Path mainFolder, boolean isBackActionEnabled) {

			super();

			this.mainFolder = mainFolder;
			this.isBackActionEnabled = isBackActionEnabled;

			managedProperty().bind(visibleProperty());

			populate();

		}

		void populate() {

			getChildren().clear();

			List<Path> folders = new ArrayList<Path>();
			List<Path> files = new ArrayList<Path>();

			try {

				Files.list(mainFolder).forEach(path -> {

					if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
						folders.add(path);
					else
						try {
							if (Files.size(path) <= CommonConstants.MAX_FILE_LENGHT)
								files.add(path);
						} catch (Exception e) {

						}

				});

			} catch (Exception e) {

			}

			if (isBackActionEnabled) {

				CustomButton backButton = new CustomButton("...",
						new ImageView(new Image(getClass().getResourceAsStream(FOLDER_ICO_PATH))));

				backButton.setOnAction(e -> {

					Runnable backAction = backActionRef.get();

					if (backAction != null)
						backAction.run();

				});

				getChildren().add(backButton);

			}

			folders.forEach(path -> {

				CustomButton cButton = new CustomButton(path.getFileName().toString(),
						new ImageView(new Image(getClass().getResourceAsStream(FOLDER_ICO_PATH))));

				cButton.setOnAction(e -> {

					Consumer<Path> folderSelectedAction = folderSelectedActionRef.get();

					if (folderSelectedAction != null)
						folderSelectedAction.accept(path);

				});

				getChildren().add(cButton);

			});

			files.forEach(path -> {

				CustomButton cButton = new CustomButton(path.getFileName().toString(),
						new ImageView(new Image(getClass().getResourceAsStream(FILE_ICO_PATH))));

				cButton.setOnAction(e -> {

					Consumer<Path> fileSelectedAction = fileSelectedActionRef.get();

					if (fileSelectedAction != null)
						fileSelectedAction.accept(path);

				});

				getChildren().add(cButton);

			});

		}

		void setOnFolderSelectedAction(Consumer<Path> folderSelectedAction) {

			folderSelectedActionRef.set(folderSelectedAction);

		}

		void setOnFileSelectedAction(Consumer<Path> fileSelectedAction) {

			fileSelectedActionRef.set(fileSelectedAction);

		}

		void setOnBackAction(Runnable backAction) {

			backActionRef.set(backAction);

		}

	}

}

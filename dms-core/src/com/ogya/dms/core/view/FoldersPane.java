package com.ogya.dms.core.view;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class FoldersPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;
	private static final double VIEW_FACTOR = ViewFactory.VIEW_FACTOR;

	private final HBox topPane = new HBox(2 * GAP);
	private final StackPane centerPane = new StackPane();

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn;
	private final Label headingLbl = new Label(Commons.translate("FILE_EXPLORER"));
	private final Button cancelBtn = ViewFactory.newCancelBtn();

	private final Map<Path, FolderView> folderViews = Collections.synchronizedMap(new HashMap<Path, FolderView>());

	private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();
	private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

	private WatchService watchService;

	FoldersPane(Path mainPath, BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);

		try {
			watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Thread watchThread = new Thread(this::processWatchServiceEvents);
		watchThread.setDaemon(true);
		watchThread.start();

		FolderView mainFolderView = newFolderView(mainPath);
		folderViews.put(mainPath, mainFolderView);
		centerPane.getChildren().add(mainFolderView);

		init();

	}

	private void init() {

		initTopPane();
		initScrollPane();

		setTop(topPane);
		setCenter(scrollPane);

	}

	private void initTopPane() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(GAP));
		topPane.setAlignment(Pos.CENTER_LEFT);

		initBackBtn();
		initHeadingLbl();
		initCancelBtn();

		topPane.getChildren().addAll(backBtn, headingLbl, cancelBtn);

	}

	private void initScrollPane() {

		scrollPane.setFitToWidth(true);

		centerPane.setPadding(new Insets(GAP));

	}

	private void initBackBtn() {

		backBtn.setOnAction(e -> {

			if (centerPane.getChildren().size() > 1) {
				backInFolders();
				return;
			}

			Runnable backAction = backActionRef.get();
			if (backAction != null) {
				backAction.run();
			}

		});

	}

	private void initHeadingLbl() {

		headingLbl.getStyleClass().add("black-label");
		HBox.setHgrow(headingLbl, Priority.ALWAYS);
		headingLbl.setMaxWidth(Double.MAX_VALUE);
		headingLbl.setFont(Font.font(null, FontWeight.BOLD, 22.0 * VIEW_FACTOR));

	}

	private void initCancelBtn() {

		cancelBtn.setOnAction(e -> {

			Runnable backAction = backActionRef.get();

			if (backAction != null) {
				backAction.run();
			}

		});

	}

	void setOnFileSelected(Consumer<Path> fileSelectedAction) {

		fileSelectedActionRef.set(fileSelectedAction);

	}

	void setOnBackAction(final Runnable backAction) {

		backActionRef.set(backAction);

	}

	void reset() {

		while (centerPane.getChildren().size() > 1)
			backInFolders();

	}

	private FolderView getSubFolderView(Path subFolder) {

		if (!folderViews.containsKey(subFolder)) {

			FolderView folderView = newFolderView(subFolder);

			folderViews.put(subFolder, folderView);

		}

		return folderViews.get(subFolder);

	}

	private FolderView newFolderView(Path folder) {

		registerFolder(folder);

		FolderView folderView = new FolderView(folder);

		folderView.setOnFolderSelectedAction(this::folderSelected);
		folderView.setOnFileSelectedAction(this::fileSelected);

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

		scrollPane.setVvalue(0.0);

	}

	private void fileSelected(Path file) {

		Consumer<Path> fileSelectedAction = fileSelectedActionRef.get();

		if (fileSelectedAction != null)
			fileSelectedAction.accept(file);

		scrollPane.setVvalue(0.0);

	}

	private void backInFolders() {

		int size = centerPane.getChildren().size();

		centerPane.getChildren().remove(size - 1);

		centerPane.getChildren().get(size - 2).setVisible(true);

		scrollPane.setVvalue(0.0);

	}

	private void registerFolder(Path folder) {

		if (watchService == null)
			return;

		try {

			folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void processWatchServiceEvents() {

		if (watchService == null)
			return;

		while (!Thread.currentThread().isInterrupted()) {

			try {

				WatchKey watchKey = watchService.take();

				Path dir = (Path) watchKey.watchable();

				FolderView folderView = folderViews.get(dir);

				for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {

					Kind<?> kind = watchEvent.kind();

					if (StandardWatchEventKinds.OVERFLOW.equals(kind))
						continue;

					if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind))
						Platform.runLater(() -> removeFolderView(dir.resolve((Path) watchEvent.context())));

					if (folderView != null)
						Platform.runLater(() -> folderView.populate());

				}

				watchKey.reset();

			} catch (InterruptedException e) {

				e.printStackTrace();

			}

		}

	}

	private class FileButton extends Button {

		private FileButton(ImageView icon, String name, String date, String size) {

			super();

			getStyleClass().add("file-button");

			setMaxWidth(Double.MAX_VALUE);
			setPadding(new Insets(GAP));

			GridPane grid = new GridPane();
			grid.setHgap(GAP);
			Label nameLbl = new Label(name);
			Label dateLbl = new Label(date);
			Label sizeLbl = new Label(size);
			dateLbl.setMinWidth(Region.USE_PREF_SIZE);
			dateLbl.setOpacity(0.5);
			sizeLbl.setMinWidth(Region.USE_PREF_SIZE);
			sizeLbl.setOpacity(0.5);
			GridPane.setHgrow(dateLbl, Priority.ALWAYS);
			GridPane.setHalignment(sizeLbl, HPos.RIGHT);
			grid.add(icon, 0, 0, 1, 2);
			grid.add(nameLbl, 1, 0, 2, 1);
			grid.add(dateLbl, 1, 1, 1, 1);
			grid.add(sizeLbl, 2, 1, 1, 1);

			setGraphic(grid);

		}

	}

	private class FolderView extends VBox {

		private static final String FOLDER_ICO_PATH = "/resources/icon/folder.png";
		private static final String FILE_ICO_PATH = "/resources/icon/file.png";

		private final Path mainFolder;

		private final AtomicReference<Consumer<Path>> folderSelectedActionRef = new AtomicReference<Consumer<Path>>();
		private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();

		FolderView(Path mainFolder) {

			super();

			this.mainFolder = mainFolder;

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
							if (Files.size(path) <= Commons.MAX_FILE_LENGTH)
								files.add(path);
						} catch (Exception e) {

						}

				});

			} catch (Exception e) {

			}

			folders.forEach(path -> {

				try {

					FileButton cButton = new FileButton(
							new ImageView(new Image(getClass().getResourceAsStream(FOLDER_ICO_PATH))),
							path.getFileName().toString(), getFileDateStr(path), null);

					cButton.setOnAction(e -> {

						Consumer<Path> folderSelectedAction = folderSelectedActionRef.get();

						if (folderSelectedAction != null)
							folderSelectedAction.accept(path);

					});

					getChildren().add(cButton);

				} catch (Exception e) {

				}

			});

			files.forEach(path -> {

				try {

					String fileName = path.getFileName().toString();

					FileButton cButton = new FileButton(
							new ImageView(new Image(getClass().getResourceAsStream(FILE_ICO_PATH))), fileName,
							getFileDateStr(path), getFileSizeStr(path));

					cButton.setTooltip(new Tooltip(fileName));

					cButton.setOnAction(e -> {

						Consumer<Path> fileSelectedAction = fileSelectedActionRef.get();

						if (fileSelectedAction != null)
							fileSelectedAction.accept(path);

					});

					getChildren().add(cButton);

				} catch (Exception e) {

				}

			});

		}

		void setOnFolderSelectedAction(Consumer<Path> folderSelectedAction) {

			folderSelectedActionRef.set(folderSelectedAction);

		}

		void setOnFileSelectedAction(Consumer<Path> fileSelectedAction) {

			fileSelectedActionRef.set(fileSelectedAction);

		}

		private String getFileDateStr(Path path) throws Exception {

			return SimpleDateFormat.getInstance().format(new Date(Files.getLastModifiedTime(path).toMillis()));

		}

		private String getFileSizeStr(Path path) throws Exception {

			long sizeInB = Files.size(path);
			double sizeInKB = (double) sizeInB / 1024;

			if (sizeInKB < 1.0) {
				return String.format("%d B", sizeInB);
			}

			double sizeInMB = sizeInKB / 1024;

			if (sizeInMB < 1.0) {
				return String.format("%.2f KB", sizeInKB);
			}

			double sizeInGB = sizeInMB / 1024;

			if (sizeInGB < 1.0) {
				return String.format("%.2f MB", sizeInMB);
			}

			return String.format("%.2f GB", sizeInGB);

		}

	}

}

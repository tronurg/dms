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
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.component.ImSearchField;
import com.ogya.dms.core.view.component.ImSearchField.ImSearchListener;
import com.ogya.dms.core.view.factory.CssFactory;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import javafx.stage.PopupWindow.AnchorLocation;

public class FoldersPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;
	private static final int MAX_SEARCH_HIT = Commons.UNITS_PER_PAGE;

	private final HBox topPane = new HBox();
	private final StackPane centerPane = new StackPane();

	private final ScrollPane scrollPane = new DmsScrollPane(centerPane);
	private final Button backBtn;
	private final Label headingLbl = new Label(Commons.translate("FILE_EXPLORER"));
	private final ImSearchField imSearchField = new ImSearchField();
	private final Button menuBtn = ViewFactory.newSettingsMenuBtn();
	private final ContextMenu contextMenu = new ContextMenu();
	private final SearchView searchView = new SearchView();

	private final BooleanProperty searchModeProperty = new SimpleBooleanProperty(false);

	private final Map<Path, FolderView> folderViews = Collections.synchronizedMap(new HashMap<Path, FolderView>());

	private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();
	private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

	private final Comparator<Node> sorterByName = new Comparator<Node>() {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof FileButton && arg1 instanceof FileButton)) {
				return 0;
			}

			FileButton btn0 = (FileButton) arg0;
			FileButton btn1 = (FileButton) arg1;

			int val = Boolean.compare(btn0.isFile, btn1.isFile);
			if (val == 0) {
				val = collator.compare(btn0.name, btn1.name);
			}

			return val;

		}

	};

	private final Comparator<Node> sorterByDate = new Comparator<Node>() {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof FileButton && arg1 instanceof FileButton)) {
				return 0;
			}

			FileButton btn0 = (FileButton) arg0;
			FileButton btn1 = (FileButton) arg1;

			int val = Boolean.compare(btn0.isFile, btn1.isFile);
			if (val == 0) {
				val = btn1.date.compareTo(btn0.date);
			}
			if (val == 0) {
				val = collator.compare(btn0.name, btn1.name);
			}

			return val;

		}

	};

	private final ObjectProperty<Comparator<Node>> selectedSorter = new SimpleObjectProperty<Comparator<Node>>();

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

		initSearchView();

		searchModeProperty.addListener((e0, e1, e2) -> {
			boolean val = Boolean.TRUE.equals(e2);
			if (val) {
				Path topFolder = getTopFolder();
				searchView.setSearchFolder(topFolder);
				addView(searchView);
				imSearchField.requestFocus();
			} else {
				searchView.clearSearch();
				imSearchField.clear();
				backInFolders();
			}
		});

		initTopPane();
		initScrollPane();

		setTop(topPane);
		setCenter(scrollPane);

	}

	private void initTopPane() {

		topPane.getStyleClass().add("top-pane");

		initBackBtn();
		initHeadingLbl();
		initImSearchField();
		initMenuBtn();

		topPane.getChildren().addAll(backBtn, headingLbl, imSearchField, menuBtn);

	}

	private void initScrollPane() {

		scrollPane.setFitToWidth(true);

		centerPane.setPadding(new Insets(GAP));

	}

	private void initBackBtn() {

		backBtn.setOnAction(e -> {
			if (searchModeProperty.get()) {
				searchModeProperty.set(false);
				return;
			}
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
		headingLbl.setStyle(CssFactory.getFontStyle(FontWeight.BOLD, null, 22.0 / 15));
		headingLbl.visibleProperty().bind(searchModeProperty.not());
		headingLbl.managedProperty().bind(headingLbl.visibleProperty());

	}

	private void initImSearchField() {

		HBox.setHgrow(imSearchField, Priority.ALWAYS);
		imSearchField.setMaxWidth(Double.MAX_VALUE);
		imSearchField.visibleProperty().bind(searchModeProperty);
		imSearchField.managedProperty().bind(imSearchField.visibleProperty());

		imSearchField.setNavigationDisabled(true);

		imSearchField.addImSearchListener(new ImSearchListener() {

			@Override
			public void searchRequested(String fulltext) {
				searchView.search(fulltext);
			}

			@Override
			public void upRequested() {
				// No action
			}

			@Override
			public void downRequested() {
				// No action
			}

		});

	}

	private void initMenuBtn() {

		HBox.setMargin(menuBtn, new Insets(-GAP, 0, -GAP, 0));
		menuBtn.setMaxHeight(Double.MAX_VALUE);
		menuBtn.visibleProperty().bind(searchModeProperty.not());
		menuBtn.managedProperty().bind(menuBtn.visibleProperty());
		menuBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> menuBtn.isHover() || contextMenu.isShowing() ? 1.0 : 0.5,
						menuBtn.hoverProperty(), contextMenu.showingProperty()));
		menuBtn.setOnAction(e -> {
			Point2D point = menuBtn.localToScreen(menuBtn.getWidth() + GAP, menuBtn.getHeight());
			contextMenu.show(menuBtn, point.getX(), point.getY());
		});

		initContextMenu();

	}

	private void initContextMenu() {

		contextMenu.getStyleClass().add("folders-menu");
		contextMenu.setAnchorLocation(AnchorLocation.CONTENT_TOP_RIGHT);

		MenuItem findItem = new MenuItem(Commons.translate("FIND_DOTS"));
		findItem.setOnAction(e -> searchModeProperty.set(true));

		ToggleGroup orderGroup = new ToggleGroup();

		RadioMenuItem orderByNameItem = new RadioMenuItem(Commons.translate("ORDER_BY_NAME"));
		orderByNameItem.setToggleGroup(orderGroup);

		RadioMenuItem orderByDateItem = new RadioMenuItem(Commons.translate("ORDER_BY_DATE"));
		orderByDateItem.setToggleGroup(orderGroup);

		orderGroup.selectedToggleProperty().addListener((e0, e1, e2) -> {
			if (e2 == null) {
				orderGroup.selectToggle(orderByNameItem);
				return;
			}
			if (e2 == orderByNameItem) {
				selectedSorter.set(sorterByName);
			} else if (e2 == orderByDateItem) {
				selectedSorter.set(sorterByDate);
			}
		});
		orderGroup.selectToggle(orderByNameItem);

		MenuItem goBackItem = new MenuItem(Commons.translate("GO_BACK"));
		goBackItem.setOnAction(e -> {
			Runnable backAction = backActionRef.get();
			if (backAction != null) {
				backAction.run();
			}
		});

		contextMenu.getItems().addAll(findItem, new SeparatorMenuItem(), orderByNameItem, orderByDateItem,
				new SeparatorMenuItem(), goBackItem);

	}

	private void initSearchView() {

		searchView.setOnFileSelectedAction(e -> {
			fileSelected(e);
			searchModeProperty.set(false);
		});

	}

	void setOnFileSelected(Consumer<Path> fileSelectedAction) {

		fileSelectedActionRef.set(fileSelectedAction);

	}

	void setOnBackAction(final Runnable backAction) {

		backActionRef.set(backAction);

	}

	void reset() {

		while (centerPane.getChildren().size() > 1) {
			backInFolders();
		}

	}

	private void addView(FileView view) {

		centerPane.getChildren().get(centerPane.getChildren().size() - 1).setVisible(false);

		centerPane.getChildren().add(view);

	}

	private void removeView(FileView view) {

		centerPane.getChildren().remove(view);

		centerPane.getChildren().get(centerPane.getChildren().size() - 1).setVisible(true);

	}

	private Path getTopFolder() {
		try {
			FolderView topView = (FolderView) centerPane.getChildren().get(centerPane.getChildren().size() - 1);
			return topView.getFolder();
		} catch (Exception e) {

		}
		return null;
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

		if (folderView == null) {
			return;
		}

		removeView(folderView);

	}

	private void folderSelected(Path folder) {

		addView(getSubFolderView(folder));

		scrollPane.setVvalue(0.0);

	}

	private void fileSelected(Path file) {

		Consumer<Path> fileSelectedAction = fileSelectedActionRef.get();

		if (fileSelectedAction != null) {
			fileSelectedAction.accept(file);
		}

		scrollPane.setVvalue(0.0);

	}

	private void backInFolders() {

		int size = centerPane.getChildren().size();

		centerPane.getChildren().remove(size - 1);

		centerPane.getChildren().get(size - 2).setVisible(true);

		scrollPane.setVvalue(0.0);

	}

	private void registerFolder(Path folder) {

		if (watchService == null) {
			return;
		}

		try {

			folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void processWatchServiceEvents() {

		if (watchService == null) {
			return;
		}

		while (!Thread.currentThread().isInterrupted()) {

			try {

				WatchKey watchKey = watchService.take();

				Path dir = (Path) watchKey.watchable();

				FolderView folderView = folderViews.get(dir);

				for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {

					Kind<?> kind = watchEvent.kind();

					if (StandardWatchEventKinds.OVERFLOW.equals(kind)) {
						continue;
					}

					if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
						Platform.runLater(() -> removeFolderView(dir.resolve((Path) watchEvent.context())));
					}

					if (folderView != null) {
						Platform.runLater(() -> folderView.populate());
					}

				}

				watchKey.reset();

			} catch (InterruptedException e) {

				e.printStackTrace();

			}

		}

	}

	private class FileButton extends Button {

		private final String name;
		private final Date date;
		private final boolean isFile;

		private FileButton(ImageView icon, Path path) throws Exception {

			super();

			this.name = path.getFileName().toString();
			this.date = new Date(Files.getLastModifiedTime(path).toMillis());
			this.isFile = !Files.isDirectory(path);

			init(icon, path);

		}

		private void init(ImageView icon, Path path) throws Exception {

			String dateStr = SimpleDateFormat.getInstance().format(date);
			String sizeStr = null;
			if (isFile) {
				sizeStr = getFileSizeStr(path);
			}

			getStyleClass().add("file-button");

			setMaxWidth(Double.MAX_VALUE);
			setPadding(new Insets(GAP));

			GridPane grid = new GridPane();
			grid.setHgap(GAP);
			Label nameLbl = new Label(name);
			Label dateLbl = new Label(dateStr);
			Label sizeLbl = new Label(sizeStr);
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

			if (isFile) {
				setTooltip(new Tooltip(name));
			}

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

	private abstract class FileView extends VBox {

		private static final String FILE_ICO_PATH = "/resources/icon/file.png";

		private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();

		FileView() {
			super();
		}

		void setOnFileSelectedAction(Consumer<Path> fileSelectedAction) {

			fileSelectedActionRef.set(fileSelectedAction);

		}

		protected void addFile(Path path) {

			try {

				if (Files.size(path) > Commons.MAX_FILE_LENGTH) {
					return;
				}

				FileButton cButton = new FileButton(
						new ImageView(new Image(getClass().getResourceAsStream(FILE_ICO_PATH))), path);

				cButton.setOnAction(e -> {

					Consumer<Path> fileSelectedAction = fileSelectedActionRef.get();

					if (fileSelectedAction != null) {
						fileSelectedAction.accept(path);
					}

				});

				getChildren().add(cButton);

			} catch (Exception e) {

			}

		}

	}

	private class FolderView extends FileView {

		private static final String FOLDER_ICO_PATH = "/resources/icon/folder.png";

		private final Path mainFolder;

		private final BooleanProperty sortable = new SimpleBooleanProperty();

		private final AtomicReference<Consumer<Path>> folderSelectedActionRef = new AtomicReference<Consumer<Path>>();
		private final AtomicReference<Comparator<Node>> localSorter = new AtomicReference<Comparator<Node>>();

		FolderView(Path mainFolder) {
			super();
			this.mainFolder = mainFolder;
			init();
		}

		private void init() {

			managedProperty().bind(visibleProperty());
			sortable.bind(visibleProperty().and(parentProperty().isNotNull()));

			sortable.addListener((e0, e1, e2) -> sort());
			selectedSorter.addListener((e0, e1, e2) -> sort());

			populate();

		}

		void populate() {

			getChildren().clear();
			localSorter.set(null);

			try {

				Files.list(mainFolder).forEach(path -> {
					if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
						addFolder(path);
					} else {
						addFile(path);
					}
				});

			} catch (Exception e) {

			}

			sort();

		}

		void setOnFolderSelectedAction(Consumer<Path> folderSelectedAction) {

			folderSelectedActionRef.set(folderSelectedAction);

		}

		Path getFolder() {
			return mainFolder;
		}

		private void addFolder(Path path) {

			try {

				FileButton cButton = new FileButton(
						new ImageView(new Image(getClass().getResourceAsStream(FOLDER_ICO_PATH))), path);

				cButton.setOnAction(e -> {

					Consumer<Path> folderSelectedAction = folderSelectedActionRef.get();

					if (folderSelectedAction != null) {
						folderSelectedAction.accept(path);
					}

				});

				getChildren().add(cButton);

			} catch (Exception e) {

			}

		}

		private void sort() {

			if (!sortable.get()) {
				return;
			}
			Comparator<Node> sorter = selectedSorter.get();
			Comparator<Node> oldSorter = localSorter.getAndSet(sorter);
			if (sorter == oldSorter || sorter == null) {
				return;
			}
			FXCollections.sort(getChildren(), sorter);

		}

	}

	private class SearchView extends FileView {

		private final AtomicReference<String> searchTextRef = new AtomicReference<String>();
		private final ExecutorService searchPool = DmsFactory.newCachedThreadPool();

		private Path searchFolder;

		SearchView() {
			super();
			setAlignment(Pos.CENTER);
		}

		private void showSearchResults(String filter, List<Path> paths, boolean hasMore) {
			if (filter != searchTextRef.get()) {
				return;
			}
			getChildren().clear();
			if (paths.isEmpty()) {
				addNotification(Commons.translate("NOT_FOUND"));
			} else {
				paths.forEach(path -> addFile(path));
			}
			if (hasMore) {
				addNotification(Commons.translate("TOO_MANY_RESULTS_NOTIFICATION"));
			}
		}

		private void addNotification(String text) {
			Label noteLabel = ViewFactory.newNoteLabel(text);
			VBox.setMargin(noteLabel, new Insets(GAP, 0.0, GAP, 0.0));
			getChildren().add(noteLabel);
		}

		void setSearchFolder(Path searchFolder) {
			this.searchFolder = searchFolder;
		}

		void search(final String fulltext) {
			clearSearch();
			final String filter = fulltext.trim().replace(".", "\\.").replace("*", ".*").replace("?", ".?")
					.toLowerCase(Locale.getDefault());
			searchTextRef.set(filter);
			addNotification(Commons.translate("SEARCHING_DOTS"));
			searchPool.execute(() -> {
				try (Stream<Path> stream = Files.find(searchFolder, Integer.MAX_VALUE, (e0, e1) -> e1.isRegularFile()
						&& e0.getFileName().toString().toLowerCase(Locale.getDefault()).matches(filter))) {
					List<Path> paths = new ArrayList<Path>();
					Iterator<Path> iter = stream.iterator();
					boolean hasNext = false;
					while (filter == searchTextRef.get() && !(paths.size() > MAX_SEARCH_HIT)) {
						try {
							if (!(hasNext = iter.hasNext()) || paths.size() == MAX_SEARCH_HIT) {
								break;
							}
							paths.add(iter.next());
						} catch (Exception e) {

						}
					}
					final boolean hasMore = hasNext;
					Platform.runLater(() -> showSearchResults(filter, paths, hasMore));
				} catch (Exception e) {

				}
			});
		}

		void clearSearch() {
			getChildren().clear();
			searchTextRef.set(null);
		}

	}

}

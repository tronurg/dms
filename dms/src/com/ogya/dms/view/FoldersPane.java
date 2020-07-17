package com.ogya.dms.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.view.factory.ViewFactory;

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

	private final Map<Path, FolderView> subFolders = new HashMap<Path, FolderView>();

	private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();

	FoldersPane(Path mainPath) {

		super();

		FolderView mainFolderView = newFolderView(mainPath, false);
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

		if (!subFolders.containsKey(subFolder)) {

			FolderView folderView = newFolderView(subFolder, true);

			subFolders.put(subFolder, folderView);

		}

		return subFolders.get(subFolder);

	}

	private FolderView newFolderView(Path folder, boolean isBackActionEnabled) {

		FolderView folderView = new FolderView(folder, isBackActionEnabled);

		folderView.setOnFolderSelectedAction(this::folderSelected);
		folderView.setOnFileSelectedAction(this::fileSelected);
		if (isBackActionEnabled)
			folderView.setOnBackAction(this::back);

		return folderView;

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

		private final AtomicReference<Consumer<Path>> folderSelectedActionRef = new AtomicReference<Consumer<Path>>();
		private final AtomicReference<Consumer<Path>> fileSelectedActionRef = new AtomicReference<Consumer<Path>>();
		private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

		private FolderView(Path mainFolder, boolean isBackActionEnabled) {

			super();

			managedProperty().bind(visibleProperty());

			List<Path> folders = new ArrayList<Path>();
			List<Path> files = new ArrayList<Path>();

			try {

				Files.list(mainFolder).forEach(path -> {

					if (Files.isDirectory(path))
						folders.add(path);
					else
						try {
							if (Files.size(path) <= CommonConstants.MAX_FILE_LENGHT)
								files.add(path);
						} catch (IOException e) {
							e.printStackTrace();
						}

				});

			} catch (IOException e) {

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

		private void setOnFolderSelectedAction(Consumer<Path> folderSelectedAction) {

			folderSelectedActionRef.set(folderSelectedAction);

		}

		private void setOnFileSelectedAction(Consumer<Path> fileSelectedAction) {

			fileSelectedActionRef.set(fileSelectedAction);

		}

		private void setOnBackAction(Runnable backAction) {

			backActionRef.set(backAction);

		}

	}

}

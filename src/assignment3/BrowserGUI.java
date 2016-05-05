package assignment3;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import assignment3.DownloadBar;
import javafx.scene.control.Alert.AlertType;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebHistory;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.concurrent.Worker.State;
import javafx.animation.FadeTransition;
import javafx.animation.*;
import javafx.util.Duration;

public class BrowserGUI extends Application {

	private Stage primaryStage;
	private HBox browserButtonBox;
	private Button forButton, backButton, bookmarkButton;
	private TextField addressField;
	private WebEngine engine;
	private ArrayList<String> bookmarks;
	private String currentAddress, downloadDirectory, homepage;
	private MenuBar menuBar;
	private Menu bookmarksMenu, jsMenu, helpMenu, settings, fileMenu;
	private WebView wv;
	private ListView<WebHistory.Entry> historyList;

	public static void main(String[] args) {
		launch(args);
	}
	
	

	/**
	 * @param primaryStage the stage that holds the browser
	 * @throws Exception any sort of exception encountered through the display and use of this Browser GUI will be thrown
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		//read in settings from external file
		readSettings();
		
		//tell the stage to save settings on close
		primaryStage.setOnCloseRequest(event -> {
			saveSettings();
		});
		
		//Create root pane
		BorderPane root = new BorderPane();
		
		//Create web view
		createWebView();
		
		//call browser bar creation method
		createBrowserBar();
		
		//call menu bar creation method
		createMenuBar();
		
		
		//Set all elements in the root border pane object
		root.setTop(new VBox(menuBar, browserButtonBox));
		root.setCenter(wv);
		root.setRight(historyList);
		
		//set the scene
		Scene scene = new Scene(root, 800, 800);
		primaryStage.setScene(scene);
		primaryStage.show();
		
	}

	/**
	 * @author Christian
	 *	Click handler that handles the users click inputs on buttons and text fields
	 */
	private class MyButtonClickHandler implements EventHandler<MouseEvent> {
		@Override
		public void handle(MouseEvent event) {

			if (event.getSource() == backButton) {
				goBack();
			} else if (event.getSource() == forButton) {
				goForward();
			} else if ((event.getSource() == addressField)) {
				//If the user clicked twice, clear the text:
				if (event.getClickCount() > 1){
					addressField.setText("http://");
					addressField.positionCaret(addressField.getLength());
				} else {
				//else the user clicked once, so select all the text:
					addressField.selectAll();
				}
			}
		}
	}
	
	public void createWebView(){
		//create the web view
		wv = new WebView();
		wv.setOnKeyPressed(event -> {
			KeyCode code = event.getCode();
			if(event.isControlDown()){
				if (code == KeyCode.LEFT){
					goBack();
				} else if (code == KeyCode.RIGHT){
					goForward();
				}
			}
		});
		engine = wv.getEngine();
		// monitor the location url, and if newLoc ends with one of the download file endings, create a new DownloadTask.
		engine.locationProperty().addListener(new ChangeListener<String>() {
				
		@Override 
		public void changed(ObservableValue<? extends String> observableValue, String oldLoc, String newLocation) {
			String[] ends = {".EXE", ".PDF", ".ZIP", ".DOC", ".DOCX", ".XLS", ".XLSX",
					".ISO", ".IMG", ".DMG", ".TAR", ".TGZ", ".JAR", ".exe", ".pdf", 
					".zip", ".doc", ".docx", ".xls", ".xlsx", ".iso", ".img", ".dmg", 
					".tar", ".tgz", ".jar"};
			for(String end: ends){
				if(newLocation.endsWith(end) ){
					DownloadBar newDownload = new DownloadBar(newLocation, downloadDirectory);
					newDownload.getDownloadWindow().show();
				}
			}
		}});	
		engine.load(homepage);

		engine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == State.SUCCEEDED) {
				final WebHistory history = engine.getHistory();
				ObservableList<WebHistory.Entry> entryList = history.getEntries();
				int currentIndex = history.getCurrentIndex();

				//update the address field with the new engine URL:
				addressField.setText(currentAddress = engine.getLocation());
				//If the ArrayList<String> of book marked URLs contains the new address, then disable the Book marks button:
				bookmarkButton.setDisable( bookmarks.contains(currentAddress) );
				
				historyList.getSelectionModel().select(engine.getHistory().getCurrentIndex());
				backButton.setDisable(currentIndex == 0);
				forButton.setDisable(currentIndex == entryList.size() - 1);
			}
		});
		
		//set engine to display alerts in the window
		engine.setOnAlert((event)->{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Alert!");
			alert.setHeaderText("Attention:");
			alert.setContentText(event.getData());
			alert.show();
		});
	}
	
	/**
	 * Creates the menu bar for the browser and implements all of its functionality
	 */
	public void createMenuBar(){
		//browser menu bar
		menuBar = new MenuBar();
		
		//file menu
		fileMenu = new Menu("File");
		MenuItem currentItem = new MenuItem("Quit");
		//Set quit key-combo
		currentItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
		currentItem.setOnAction(event-> {
			saveSettings();
			Platform.exit();
		});
		fileMenu.getItems().add(currentItem);
		
		//settings menu
		settings = new Menu("Settings");
		
		//settings home page menu
		MenuItem homepageMenu = new MenuItem("Homepage");
		homepageMenu.setOnAction(event -> {
			TextInputDialog dialog = new TextInputDialog(this.homepage);
			dialog.setTitle("Homepage");
			dialog.setHeaderText("Set a new homepage");
			dialog.setContentText("Homepage: ");

			Optional<String> result = dialog.showAndWait();
			if (result.isPresent()){
				this.homepage = result.get();
				saveSettings();
			}
		});
		
		//settings download menu
		MenuItem downloadsMenu = new MenuItem("Downloads");
		downloadsMenu.setOnAction(event -> {
			TextInputDialog dialog = new TextInputDialog(this.downloadDirectory);
			dialog.setTitle("Downloads");
			dialog.setHeaderText("Set a new downloads directory");
			dialog.setContentText("directory path: ");

			Optional<String> result = dialog.showAndWait();
			if (result.isPresent()){
				File newDownloadDir = new File(result.get().trim());
				if(newDownloadDir.exists() && newDownloadDir.isDirectory()){
				} else {
					newDownloadDir.mkdir();
				}
				if (newDownloadDir.canWrite()){
					this.downloadDirectory = newDownloadDir.getAbsolutePath()+"\\";
					System.out.println(downloadDirectory);
					saveSettings();
				} else {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Selection not Valid!");
					alert.setHeaderText("Error in changing downloads directory");
					alert.setContentText("It appears that you do not have sufficient permissions to select "
							+result.get()+" as your download path");

					alert.showAndWait();
				}
			}
		});
		settings.getItems().addAll(homepageMenu, downloadsMenu);
		
		//help menu
		helpMenu = new Menu("Help");
		MenuItem javaHelp = new MenuItem("Look for documentation");
		javaHelp.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
		javaHelp.setOnAction(e -> {
			TextInputDialog dialog = new TextInputDialog("Type here");
			dialog.setTitle("Look for java documentation");
			dialog.setHeaderText("Search for Java class Documentation");
			dialog.setContentText("Which Java class do you want to research?");

			// Traditional way to get the response value.
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent()) {
				currentAddress = "https://www.google.ca/search?q=java+" + result.get();
				engine.load(currentAddress);
			}
		});
		//History check menu item and list
		historyList = new ListView<WebHistory.Entry>();
		historyList.setMaxWidth(0);
		
		//history menu closing sequence
		CheckMenuItem cmi = new CheckMenuItem("History");
		cmi.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
		cmi.setOnAction(e -> {
			if (cmi.isSelected()) {
				historyList.setMaxWidth(300.0f);
				ScaleTransition back = new ScaleTransition(Duration.millis(3000), historyList);
				back.setToX(1.0);
				back.setToY(1.0);
				FadeTransition ft = new FadeTransition(Duration.millis(3000), historyList);
				
				// Make sure the fade is starting from invisible:
				ft.setFromValue(0);
				ft.setToValue(1);
				ParallelTransition pt = new ParallelTransition();
				pt.getChildren().addAll(back, ft);
				pt.play();
			} else {
				ScaleTransition down = new ScaleTransition(Duration.millis(1000), historyList);
				down.setToY(0.2f);
				ScaleTransition right = new ScaleTransition(Duration.millis(1000), historyList);
				right.setToX(0.0f);
				SequentialTransition seq = new SequentialTransition();
				seq.getChildren().addAll(down, right);
				seq.play();
				seq.setOnFinished(arg -> historyList.setMaxWidth(0.0f));
			}
		});
		
		//define history list selection
		historyList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		historyList.setItems(engine.getHistory().getEntries());
		historyList.setOnMouseClicked(e -> {
			//previousItem is where the engine is currently in the history
			int previousItem = engine.getHistory().getCurrentIndex();
			
			//selectedItem is where the user wants to go in the history
			int selectedItem = historyList.getSelectionModel().getSelectedIndex();
			
			//change is the direction, and number of pages to go forward or backward:
			int change = selectedItem - previousItem;
			engine.getHistory().go(change);
		});
		
		//about menu item
		MenuItem about = new MenuItem("About");
		about.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
		about.setOnAction(e -> {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText("About");
			alert.setContentText("Eric's browser, v1.0. Feb. 18, 2016");
			alert.show();
		});
		helpMenu.getItems().addAll(javaHelp, cmi, about);
		
		//java script menu item
		jsMenu = new Menu("Javascript");
		MenuItem execute = new MenuItem("Exectute code");
		execute.setOnAction( (event) -> {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setTitle("Execute Javascript");
			dialog.setHeaderText("Type some javascript code for the browser to execute:");
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent()){
				try{
					engine.executeScript(result.get());
				} catch(Exception e) {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Error!");
					alert.setHeaderText("Error in executing your script");
					alert.setContentText(e.getMessage());

					alert.showAndWait();
				}
			}
		});
		jsMenu.getItems().add(execute);
		
		
		
		//add Menus to menuBar
		menuBar.getMenus().addAll(fileMenu, bookmarksMenu, settings, helpMenu, jsMenu);
	}
	
	/**
	 * @param primaryStage the primaryStage of our GUI
	 * Saves settings for the Browser
	 */
	public void saveSettings(){
		
		//book marks output stream
		try(FileOutputStream fos = new FileOutputStream("./bookmarks.tmp")){
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.bookmarks);
			
		} catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		
		//screen coordinates output
		try(BufferedWriter writer = Files.newBufferedWriter(Paths.get("./settings.txt"));){
			String height = String.valueOf(primaryStage.getHeight());
			String width = String.valueOf(primaryStage.getWidth());
			String x = String.valueOf(primaryStage.getX());
			String y = String.valueOf(primaryStage.getY());
			writer.write("height="+height);
			writer.newLine();
			writer.write("width="+width);
			writer.newLine();
			writer.write("screenX="+x);
			writer.newLine();
			writer.write("screenY="+y);
			writer.newLine();
			writer.write("downloadDirectory="+downloadDirectory);
			writer.newLine();
			writer.write("homepage="+homepage);
		} catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		
	}
	
	/**
	 * reads in settings from a specified file and applies them to the BrowserGUI
	 */
	public void readSettings(){
		File bFile = new File("./bookmarks.tmp");
		File sFile = new File("./settings.txt");
		//read settings file
		if(sFile.exists()){
			String thisLine = null;
			try(BufferedReader reader = Files.newBufferedReader(Paths.get("./settings.txt"));){  
				//read in lines
				while ((thisLine = reader.readLine()) != null){
					String id = thisLine.split("=")[0];
					String value = thisLine.split("=")[1];
					//set object variables based on id=value pairs
					switch (id){
						case "height": 	primaryStage.setHeight(Double.parseDouble(value));
						case "width":	primaryStage.setWidth(Double.parseDouble(value));
						case "screenX":	primaryStage.setX(Double.parseDouble(value));
						case "screenY":	primaryStage.setY(Double.parseDouble(value));
						case "downloadDirectory":	this.downloadDirectory = value;
						case "homepage":			this.homepage = value;
					}
				 };
				
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		//read book marks file
		if(bFile.exists()){
			try(FileInputStream fis = new FileInputStream("./bookmarks.tmp")){
				ObjectInputStream ois = new ObjectInputStream(fis);
				bookmarks = (ArrayList<String>) ois.readObject();
				
			} catch(IOException | ClassNotFoundException ioe) {
				System.out.println(ioe.getMessage());
			}
		}
	}
	
	/**
	 * creates the GUI's browser bar and adds it to the browserButtonBox
	 */
	private void createBrowserBar(){
		
		//Instantiate buttonClickHandler
		MyButtonClickHandler buttonClickHandler = new MyButtonClickHandler();
				
		//create browser control bar with tool tips
		browserButtonBox = new HBox();
		forButton = new Button("Forward");
		Tooltip.install(forButton, new Tooltip("Go forward one page"));
		backButton = new Button("Back");
		Tooltip.install(backButton, new Tooltip("Go back one page"));
		addressField = new TextField("address here");
		//Tell the address field to grow to fill empty space:
		addressField.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(addressField, Priority.ALWAYS);
		
		/*Enter address field key ENTER event. This key event will tell disable the forward button,
		load the page in the address bar
		*/
		
		addressField.setOnKeyReleased(event -> {
			KeyCode code = event.getCode();
			if (code == KeyCode.ENTER) {
				String add = addressField.getText();
				forButton.setDisable(true);
				engine.load(add);
				bookmarkButton.setDisable(true);
				currentAddress = add;
			}
		});
		
		//Add mouse click handlers to nodes.
		addressField.setOnMouseClicked(buttonClickHandler);
		backButton.setOnMouseClicked(buttonClickHandler);
		forButton.setOnMouseClicked(buttonClickHandler);

		//set the back and forward buttons disabled by default
		backButton.setDisable(true);
		forButton.setDisable(true);
		
		//Create book marks menu.
		bookmarksMenu = new Menu("Bookmarks");
		
		//Read in book marks from loaded object variable 
		Iterator<String> bmIterator = bookmarks.iterator();
		while(bmIterator.hasNext()){
			String url = bmIterator.next();
			MenuItem item = new MenuItem(url);
			item.setOnAction(event ->  engine.load(url) );
			bookmarksMenu.getItems().add(item);
		}
		//Add book mark button
		bookmarkButton = new Button("Add Bookmark");
		Tooltip.install(bookmarkButton, new Tooltip("Add the current page to your bookmarks"));
		bookmarkButton.setOnMouseClicked(e -> {
			if (!bookmarks.contains(currentAddress)) {
				bookmarks.add(currentAddress);
				bookmarkButton.setDisable(true);
				MenuItem newItem = new MenuItem(currentAddress);
				String addressCopy = new String(currentAddress);
				newItem.setOnAction(event ->  engine.load(addressCopy) );
				bookmarksMenu.getItems().add(newItem);
			}
		});
		//Add all elements to browser bar
		browserButtonBox.getChildren().addAll(backButton, addressField, bookmarkButton, forButton);
	}
	
	/**
	 * Takes the user back by one page in their web history
	 */
	public void goBack() {
		final WebHistory history = engine.getHistory();
		int currentIndex = history.getCurrentIndex();
		if (currentIndex > 0) {
			Platform.runLater(() -> {
				history.go(-1);
				currentAddress = history.getEntries().get(currentIndex - 1).getUrl();
				addressField.setText(currentAddress);
				backButton.setDisable(currentIndex - 1 ==0);
			});
		}
	}

	/**
	 * Takes the user forward one page in their web history
	 */
	public void goForward() {
		final WebHistory history = engine.getHistory();
		ObservableList<WebHistory.Entry> entryList = history.getEntries();
		int currentIndex = history.getCurrentIndex();
		if (currentIndex + 1 < entryList.size()) {
			Platform.runLater(new Runnable() {
				public void run() {
					history.go(1);
					currentAddress = history.getEntries().get(currentIndex + 1).getUrl();
					addressField.setText(currentAddress);
					forButton.setDisable(currentIndex + 1 == entryList.size());
				}
			});
		}
	}
}

package assignment3;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class DownloadBar extends HBox {

	private static Stage downloadWindow = null;
	private static VBox downloadTasks;
	private static TextArea messageArea;
	private ProgressBar progressBar;
	private Button cancel;
	private Text name;
	private File down;
	private String stringURL;
	private String downloadDirectory;
	
	/** Calling this function will guarantee that the downloadTasks VBox is created and visible.
	 * @return A Stage that will show each downloadTask's progress
	 */
	public Stage getDownloadWindow()
	{
		if(downloadWindow == null)
		{
			//Create a new borderPane for the download window
			BorderPane downloadRoot = new BorderPane();
			downloadTasks = new VBox();
			//downloadTasks will contain rows of DownloadTask objects, which are HBoxes
			downloadRoot.setCenter(		 downloadTasks		);
			
			//The bottom of the window will be the message box for download tasks
			downloadRoot.setBottom(		messageArea = new TextArea() 		);
			downloadWindow = new Stage();
			downloadWindow.setScene( new Scene(downloadRoot, 400, 600)  );
			
			//When closing the window, set the variable downloadWindow to null
			downloadWindow.setOnCloseRequest(		event -> downloadWindow = null		);
		}
		return downloadWindow;
	}
	
	/**The constructor for a DownloadTask
	 * 
	 * @param newLocation the URL for the file being downloaded
	 * @param directory the target download location
	 */
	public DownloadBar(String newLocation, String directory)
	{
		//See if the filename at the end of newLocation exists on your hard drive.
		// If the file already exists, then add (1), (2), ... (n) until you find a new filename that doesn't exist.
		stringURL = newLocation;
		downloadDirectory = directory;
		String downLocation = downloadDirectory+newLocation.substring(newLocation.lastIndexOf("/")+1, newLocation.length());
		down = new File(downLocation);
		String fPath = downLocation.substring(0, downLocation.length()-4);
		String ext = downLocation.substring(downLocation.length()-4, downLocation.length());
		int count = 1;
		while(down.exists()){
			down = new File(fPath+"("+(count++) +")"+ext);
		}
		
		//Create the window if it doesn't exist. After this call, the VBox and TextArea should exist.
		if(downloadWindow == null){
			getDownloadWindow();
		}

		
		
		//Add a Text label for the filename
		name = new Text(down.getName());
		
		//Add a ProgressBar to show the progress of the task
		progressBar = new ProgressBar();
		progressBar.setProgress(0);
		
		//Add a cancel button that asks the user for confirmation, and cancel the task if the user agrees
		cancel = new Button("Cancel");
		
		this.getChildren().addAll(name, progressBar, cancel);
		downloadTasks.getChildren().add(this);
		
		
		 //Start the download
		DownloadTask aFileDownload = new DownloadTask( ) ;
		// add the cancel and progress bar to task
		progressBar.progressProperty().bind(aFileDownload.progressProperty());
		cancel.setOnAction(event ->{
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Cancel");
			alert.setHeaderText("Cancel Download");
			alert.setContentText("Are you sure you want to cancel this download?");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK){
				aFileDownload.cancel();
				downloadTasks.getChildren().remove(this);
				down.delete();
			}
		});
		  new Thread( aFileDownload ).start();
	}
	
	
	
	
	
	/**This class represents a task that will be run in a separate thread. It will run call(), 
	 *  and then call succeeded, cancelled, or failed depending on whether the task was cancelled
	 *  or failed. If it was not, then it will call succeeded() after call() finishes.
	 */
	private class DownloadTask extends Task<String>
	{
		private static final int BUFFER_SIZE = 4096;
		//Creating a downloadtask
		@Override
		protected String call() throws Exception {
			String message;
				URL url = new URL(stringURL);
				HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
				int responseCode = httpConn.getResponseCode();
				
				//check response code
				if (responseCode == HttpURLConnection.HTTP_OK) {
		            long contentLength = httpConn.getContentLength();
	         
		            //open an input stream from the HTTP connection
		            InputStream inputStream = httpConn.getInputStream();
		            String saveFilePath = down.getAbsolutePath();
		            
		            //open an output stream from the HTTP connection
		            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
		            int bytesRead = -1;
		            int totalRead = 0;
		            byte[] buffer = new byte[BUFFER_SIZE];
		            while ((bytesRead = inputStream.read(buffer)) != -1 && !this.isCancelled()){
		            	outputStream.write(buffer, 0, bytesRead);
		            	totalRead += bytesRead;
		            	this.updateProgress(totalRead, contentLength);
		            }
		            
		            outputStream.close();
		            inputStream.close();
		            
		            if(this.isCancelled()){
		            	down.delete();
		            }
		            message = "Succeeded!";
				} else {
					message = "No file to download. Server replied HTTP code: "+responseCode;
				}
				httpConn.disconnect();
			return message;
		}
		
		@Override
		protected void succeeded() {
			super.succeeded();
			downloadTasks.getChildren().remove(DownloadBar.this);
			messageArea.appendText(name.getText()+" was successfully downloaded!\n");
			Alert downAlert = new Alert(AlertType.CONFIRMATION);
			downAlert.setTitle("Download Succeeded");
			downAlert.setHeaderText("Would you like to execute the file now?");
			Optional<ButtonType> result = downAlert.showAndWait();
			if (result.get() == ButtonType.OK){
			    try{
			    	if(Desktop.isDesktopSupported()){
			    		Desktop.getDesktop().open(down);
			    	}
			    } catch(Exception e){
			    	Alert openErrorAlert = new Alert(AlertType.ERROR);
			    	openErrorAlert.setTitle("Error!");
			    	openErrorAlert.setHeaderText("Error in executing your script");
			    	openErrorAlert.setContentText(e.getMessage());

			    	openErrorAlert.showAndWait();
			    }
			}
			
			
		}
		
		@Override
		protected void cancelled() {
			super.cancelled();
			downloadTasks.getChildren().remove(DownloadBar.this);
			messageArea.appendText(name.getText()+" download was cancelled\n");
			down.delete();
		}
		

		@Override
		protected void failed() {		
			super.failed();
			downloadTasks.getChildren().remove(DownloadBar.this);
			messageArea.appendText(name.getText()+" download failed\n");
			down.delete();
		}
	}		
}

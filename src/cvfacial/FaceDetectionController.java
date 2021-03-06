package cvfacial;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.photo.Photo;
import org.opencv.videoio.VideoCapture;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import utils.Utils;

/**
 * The controller associated with the only view of our application. The application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative controls and the face detection/tracking.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 1.1 (2015-11-10)
 * @since 1.0 (2014-01-10)
 *
 */
public class FaceDetectionController {
	// FXML buttons
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	@FXML
	private ImageView face;
	@FXML
	private ImageView spot2;
	@FXML
	private ImageView spot3;
	@FXML
	private Button doGates;

	// checkboxes for enabling/disabling a classifier
	@FXML
	private CheckBox haarClassifier;
	@FXML
	private CheckBox lbpClassifier;
	@FXML
	private GridPane grid1;
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;

	// face cascade classifier
	private CascadeClassifier faceCascade;
	private int absoluteFaceSize;
	private Mat head;
	private Mat spotImage;
	private Mat spot2Image;

	/**
	 * Init the controller, at start time
	 */
	protected void init() {
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.absoluteFaceSize = 0;

		// set a fixed width for the frame
		originalFrame.setFitWidth(600);
		// preserve image ratio
		originalFrame.setPreserveRatio(true);

		face.fitHeightProperty().bind(grid1.heightProperty().divide(4.1));
		spot2.fitHeightProperty().bind(grid1.heightProperty().divide(4.1));
		spot3.fitHeightProperty().bind(grid1.heightProperty().divide(4.1));
	}

	public static Mat equalizeIntensity(Mat inputImage) {
		if (inputImage.channels() >= 3) {
			Mat ycrcb = new Mat();

			Imgproc.cvtColor(inputImage, ycrcb, Imgproc.COLOR_BGR2YCrCb);

			ArrayList<Mat> channels = new ArrayList<>();
			Core.split(ycrcb, channels);

			Imgproc.equalizeHist(channels.get(0), channels.get(0));

			Mat result = new Mat();

			Core.merge(channels, ycrcb);

			Imgproc.cvtColor(ycrcb, result, Imgproc.COLOR_YCrCb2BGR);

			return result;
		}
		return new Mat();
	}

	static boolean bill = false;

	@FXML
	protected void godoGates() {
		stopAcquisition();

		Mat userImg = new Mat();
		head.copyTo(userImg);

		userImg = equalizeIntensity(userImg);
		Mat orig_image = Imgcodecs.imread("resources/thegates.jpg", Imgcodecs.CV_LOAD_IMAGE_COLOR);

		Size matSize = new Size();
		matSize.height = 2 * orig_image.height();
		matSize.width = 2 * orig_image.width();
		Imgproc.resize(orig_image, orig_image, matSize);
		bill = true;
		detectAndDisplay(orig_image);

		userImg = scaleMat(spotImage, userImg);
//		Utils.DisplayImage("mixed32", userImg);
//		Utils.DisplayImage("mixed32", spotImage);
		// Imgproc.resize(orig_image, orig_image, orig_image.size(), 3, 3, Imgproc.INTER_AREA);

		// Create an all white mask
		Mat out;
		Mat in[] = {userMask, userMask, userMask};
		
//		Core.merge(Arrays.asList(in), src_mask);
		
		Mat src_mask = Mat.ones(userImg.rows(), userImg.cols(), userImg.depth());

//		Core.merge(Arrays.asList(in), src_mask);
		Core.multiply(src_mask, new Scalar(255), src_mask);
//		Utils.DisplayImage("asdfasdfsdafads", src_mask);
//		src_mask.submat)
		// Photo.illuminationChange(, mask, result, 0.2f, 0.4f);

		// The location of the center of the src in the dst
//		 Point center(dst.cols/2,dst.rows/2);

		// // Seamlessly clone src into dst and put the results in output
		// Mat normal_clone;
		Mat mixed_clone = new Mat();

		// Photo.seamlessClone(userImg, dst, src_mask, gatesCenter, normal_clone, Photo);
		Photo.seamlessClone(userImg, orig_image, src_mask, gatesCenter, mixed_clone, Photo.MIXED_CLONE);

//		Utils.DisplayImage("mixed", mixed_clone);
		bill = false;
		
		spot2Image =mixed_clone;
		// grab a frame every 33 ms (30 frames/sec)
		Runnable frameGrabber = new Runnable() {

			@Override
			public void run() {
				// effectively grab and process a single frame
				Mat frame = grabFrame();
if(!bill){
	frame = spot2Image;
}
				// convert and show the frame
				Image imageToShow = Utils.mat2Image(frame);
				updateImageView(originalFrame, imageToShow);

				imageToShow = Utils.mat2Image(head);
				updateImageView(face, imageToShow);

				// spotImage = head;
				imageToShow = Utils.mat2Image(spotImage);
				updateImageView(spot2, imageToShow);

				// spot2Image = head;
				imageToShow = Utils.mat2Image(spot2Image);
				updateImageView(spot3, imageToShow);

			}
		};

		this.timer = Executors.newSingleThreadScheduledExecutor();
		this.timer.execute(frameGrabber);

	}

	/**
	 * 2:36am - Chase
	 *
	 * @param srcMat
	 *            Use matrix of image with wanted crop size
	 * @param oldMat
	 *            Use matrix of image that you want to crop
	 * @return newMat Returns resized matrix
	 */

	protected Mat scaleMat(Mat srcMat, Mat oldMat) {
		Mat newMat = new Mat();
		Size matSize = new Size();
		matSize.height = srcMat.height();
		matSize.width = srcMat.width();
		Imgproc.resize(oldMat, newMat, matSize);

		return newMat;
	}

	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera() {
		if (!this.cameraActive) {
			// disable setting checkboxes
			this.haarClassifier.setDisable(true);
			this.lbpClassifier.setDisable(true);

			// start the video capture
			this.capture.open(0);

			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						// effectively grab and process a single frame
						Mat frame = grabFrame();

						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(originalFrame, imageToShow);

						imageToShow = Utils.mat2Image(head);
						updateImageView(face, imageToShow);

						// spotImage = head;
						imageToShow = Utils.mat2Image(spotImage);
						updateImageView(spot2, imageToShow);

						// spot2Image = head;
						imageToShow = Utils.mat2Image(spot2Image);
						updateImageView(spot3, imageToShow);

					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.cameraButton.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");
			// enable classifiers checkboxes
			this.haarClassifier.setDisable(false);
			this.lbpClassifier.setDisable(false);

			// stop the timer
			this.stopAcquisition();
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Image} to show
	 */
	private Mat grabFrame() {
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					// face detection
					this.detectAndDisplay(frame);
				}

			} catch (Exception e) {
				// log the (full) error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return frame;
	}

	/**
	 * Method for face detection and tracking
	 *
	 * @param frame
	 *            it looks for faces in this frame
	 */
	private void detectAndDisplay(Mat frame) {
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();

		// convert the frame in gray scale
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);

		// compute minimum face size (20% of the frame height, in our case)
		if (this.absoluteFaceSize == 0) {
			int height = grayFrame.rows();

			if (Math.round(height * 0.2f) > 0) {

				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}

		// detect faces

		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

		// each rectangle in faces is a face: draw them!

		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			// Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);

			// Roi is bounding box of face
			Rect roi = facesArray[i];
			// get region of face only
			Mat contourRegion = frame.submat(roi);
			head = contourRegion;

			// get center of face
			Point center = new Point(roi.tl().x + (roi.br().x - roi.tl().x) / 2, roi.tl().y + (roi.br().y - roi.tl().y) / 2);
			gatesCenter = center;
			Mat mask = Mat.zeros(frame.size(), CvType.CV_8UC1);

			// Draw the ellipse using a solid white fill
			RotatedRect rr = new RotatedRect(center, new Size(roi.width * .8, roi.height), 0);
			Imgproc.ellipse(mask, rr, new Scalar(255, 255, 255), -1);
if(!bill){
	userMask =  mask.submat(roi);;
}
			if (bill) {
				
				
				System.out.println(Core.mean(spotImage));
				Scalar ss = Core.mean(frame, mask);
				Scalar s = new Scalar(ss.val[0], ss.val[1], ss.val[2]);
				Imgproc.ellipse(frame, rr, s, -1);
				Utils.DisplayImage("adsf", frame);

			}

			Mat x = new Mat();
			frame.copyTo(x, mask);
			spotImage = x.submat(roi);

		}

		// Mat orig_image = Imgcodecs.imread("res/person1.jpg", Imgcodecs.CV_LOAD_IMAGE_COLOR);
		// Mat newface = new Mat();
		// newface = scaleMat(orig_image, faces);
		// spotImage = newface;
		// spot2Image = orig_image;

	}

	private static Point gatesCenter;
	private static Mat userMask;

	/**
	 * The action triggered by selecting the Haar Classifier checkbox. It loads the trained set to be used for frontal face detection.
	 */
	@FXML
	protected void haarSelected(Event event) {
		// check whether the lpb checkbox is selected and deselect it
		if (this.lbpClassifier.isSelected())
			this.lbpClassifier.setSelected(false);

		this.checkboxSelection("resources/haarcascades/haarcascade_frontalface_alt.xml");
		// this.checkboxSelection("res/haarcascade_eye.xml");
	}

	/**
	 * The action triggered by selecting the LBP Classifier checkbox. It loads the trained set to be used for frontal face detection.
	 */
	@FXML
	protected void lbpSelected(Event event) {
		// check whether the haar checkbox is selected and deselect it
		if (this.haarClassifier.isSelected())
			this.haarClassifier.setSelected(false);

		this.checkboxSelection("resources/lbpcascades/lbpcascade_frontalface.xml");
	}

	/**
	 * Method for loading a classifier trained set from disk
	 *
	 * @param classifierPath
	 *            the path on disk where a classifier trained set is located
	 */
	private void checkboxSelection(String classifierPath) {
		// load the classifier(s)
		this.faceCascade.load(classifierPath);

		// now the video capture can start
		this.cameraButton.setDisable(false);
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();

			Utils.saveImg("output", spotImage);

		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 *
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}

}
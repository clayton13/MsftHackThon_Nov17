package utils;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Provide general purpose methods for handling OpenCV-JavaFX data conversion. Moreover, expose some "low level" methods for matching few JavaFX
 * behavior.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a>
 * @version 1.0 (2016-09-17)
 * @since 1.0
 * 
 */
public final class Utils {
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		} catch (Exception e) {
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}

	/**
	 * Generic method for putting element running on a non-JavaFX thread on the JavaFX thread, to properly update the UI
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value) {
		Platform.runLater(() -> {
			property.set(value);
		});
	}

	/**
	 * Support for the {@link mat2image()} method
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original) {
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);

		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

		return image;
	}

	public static void saveImg(String path, Mat arg) {
		BufferedImage img;
		try {
			// Create an empty image in matching format
			img = new BufferedImage(arg.width(), arg.height(), BufferedImage.TYPE_3BYTE_BGR);
			System.out.println(arg.channels());
			// Get the BufferedImage's backing array and copy the pixels
			// directly into it
			byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			arg.get(0, 0, data);

			BufferedImage i = Utils.matToBufferedImage(arg.clone());

			File outputfile = new File(path + ".png");
			ImageIO.write(i, "png", outputfile);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static List<JFrame> pre = new ArrayList<>();

	public static void DisplayImage(String txt, Mat arg) {
		BufferedImage img;
		try {

			// Create an empty image in matching format
			img = new BufferedImage(arg.width(), arg.height(), BufferedImage.TYPE_3BYTE_BGR);
			System.out.println(arg.channels());
			// Get the BufferedImage's backing array and copy the pixels
			// directly into it
			byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			arg.get(0, 0, data);
			// img = ImageIO.read(new File(arg));

			BufferedImage i = Utils.matToBufferedImage(arg.clone());

			File outputfile = new File(txt + ".png");
			ImageIO.write(i, "png", outputfile);

			ImageIcon icon = new ImageIcon(i);
			JFrame frame = new JFrame();
			frame.setLayout(new FlowLayout());
			frame.setSize(1200, 1200);
			frame.setTitle(txt);
			JLabel lbl = new JLabel();
			lbl.setIcon(icon);
			lbl.setText(txt);
			lbl.setSize(30, 200);
			frame.add(lbl);

			frame.pack();

			if (pre.size() > 0) {
				frame.setLocationRelativeTo(pre.get(pre.size() - 1));
				// frame.rela
				// frame.setLocation(1200 / 2, 600);
			} else {

			}
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			pre.add(frame);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

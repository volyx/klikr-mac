package io.github.volyx;

import okhttp3.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

public class Main {
	public static void main(String[] args) {
		Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		System.setProperty("java.awt.headless", "true");
		System.setProperty("apple.awt.UIElement", "true");

		cleanClipBoard();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {

					try {
						Thread.sleep(2_000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					final ClipboardOwner clipboardOwner = new ClipboardOwner() {
						@Override
						public void lostOwnership(Clipboard clipboard, Transferable contents) {
							try {
								System.out.println((String) clipboard.getData(DataFlavor.stringFlavor));
							} catch (UnsupportedFlavorException e) {
							} catch (IOException e) {
							}
						}
					};
					final Transferable contents = systemClipboard.getContents(clipboardOwner);
					try {
//							for (DataFlavor transferDataFlavor : contents.getTransferDataFlavors()) {
//								System.out.println(transferDataFlavor.toString());
//							}
						if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
//							final Object transferData = contents.getTransferData(DataFlavor.imageFlavor);
							Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
							final BufferedImage bufferedImage = toBufferedImage(image);

							String fileName = "savingAnImage";
							String ext = "png";
							File file = new File(fileName + "." + ext);
							try {
								ImageIO.write(bufferedImage, ext, file);  // ignore returned boolean
								uploadFile("https://klikr.org/upload.php", file);
							} catch(IOException e) {
								System.out.println("Write error for " + file.getPath() +
										": " + e.getMessage());
							}
							cleanClipBoard();
						}
//
					} catch (UnsupportedFlavorException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}


				}
			}
		}).start();

		JFrame frame = new JFrame("Klikr");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try (InputStream is = Main.class.getClassLoader().getResourceAsStream("doc.png");) {
			Objects.requireNonNull(is);
			Image image = ImageIO.read(is);
//			frame.setIconImage(image);
			if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
				Taskbar.getTaskbar().setIconImage(image);
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		frame.setSize(new Dimension(0, 0));
		frame.setVisible(false);

	}

	private static void cleanClipBoard() {
		StringSelection stringSelection = new StringSelection("");
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				stringSelection, null);
	}

	public static Boolean uploadFile(String serverURL, File file) {
		try {

			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("imagedata", file.getName(), RequestBody.create(MediaType.parse("png"), file))
//					.addFormDataPart("some-field", "some-value")
					.build();

			Request request = new Request.Builder()
					.url(serverURL)
					.post(requestBody)
					.build();
			OkHttpClient client = new OkHttpClient();
			client.newCall(request).enqueue(new Callback() {

				@Override
				public void onFailure(Call call, IOException e) {

				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					if (!response.isSuccessful()) {
						// Handle the error
					} else {
						final URL url = response.request().url().url();
						try {
							Desktop.getDesktop().browse(url.toURI());
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
					}
				}

			});

			return true;
		} catch (Exception ex) {
			// Handle the error
		}
		return false;
	}


	private static BufferedImage toBufferedImage(Image src) {
		int w = src.getWidth(null);
		int h = src.getHeight(null);
		int type = BufferedImage.TYPE_INT_RGB;  // other options
		BufferedImage dest = new BufferedImage(w, h, type);
		Graphics2D g2 = dest.createGraphics();
		g2.drawImage(src, 0, 0, null);
		g2.dispose();
		return dest;
	}
}

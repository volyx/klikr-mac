package io.github.volyx;

import okhttp3.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

public class Main {
	public static void main(String[] args) {
		Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		System.setProperty("java.awt.headless", "true");
		System.setProperty("apple.awt.UIElement", "true");

		cleanClipBoard();
		new Thread(() -> {
			while (true) {

				try {
					Thread.sleep(2_000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				final Transferable contents = systemClipboard.getContents(null);
				File file = null;
				try {
					if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
						Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
						final BufferedImage bufferedImage = toBufferedImage(image);

						String fileName = UUID.randomUUID().toString();
						String ext = "png";
						try {
							file = Files.createTempFile(fileName, "." + ext).toFile();
							ImageIO.write(bufferedImage, ext, file);  // ignore returned boolean
							uploadFile("https://klikr.org/upload.php", file);
						} catch(IOException e) {
							throw new RuntimeException(e);
						}
						cleanClipBoard();
					}
				} catch (UnsupportedFlavorException | IOException e) {
					throw new RuntimeException(e);
				}

			}
		}).start();

		JFrame frame = new JFrame("Klikr");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try (InputStream is = Main.class.getClassLoader().getResourceAsStream("doc.png");) {
			Objects.requireNonNull(is);
			Image image = ImageIO.read(is);
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
					.build();

			Request request = new Request.Builder()
					.url(serverURL)
					.post(requestBody)
					.build();
			OkHttpClient client = new OkHttpClient();
			client.newCall(request).enqueue(new Callback() {

				@Override
				public void onFailure(Call call, IOException e) {
					try {
						Files.delete(file.toPath());
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
					throw new RuntimeException(e);
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					try {
						Files.delete(file.toPath());
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
					if (!response.isSuccessful()) {
						// Handle the error
					} else {
						final URL url = response.request().url().url();
						try {
							Desktop.getDesktop().browse(url.toURI());
						} catch (URISyntaxException e) {
							throw new RuntimeException(e);
						}
					}
				}

			});

			return true;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
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

// compiled with Java 8 
// javac --release 8 -cp "comm.jar" BMP.java SimpleRead.java
//to get major version:  javap -verbose myClass | findstr "major"

package code;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/*from bmp class */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
/*from bmp class */

//for StandardCharsets
import java.nio.charset.StandardCharsets;
//for time in filena,e
import java.time.LocalDateTime;
//for dynamic array/list
import java.util.*;

import javax.comm.CommPortIdentifier;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

public class SimpleRead {
	private static final char[] IMAGE_START = { '*', 'R', 'D', 'Y', '*' };
	private static final int WIDTH = /* 320 */ 320 * 2;
	private static final int HEIGHT = /* 240 */ 240 * 2;

	private static CommPortIdentifier portId;
	InputStream inputStream;
	SerialPort serialPort;

	public static void main(String[] args) {
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				System.out.println("Port name: " + portId.getName());
				if (portId.getName().equals("COM3")) {
					SimpleRead reader = new SimpleRead();
				}
			}
		}
	}

	public SimpleRead() {

		try {
			serialPort = (SerialPort) portId.open("SimpleReadApp", 1000);
			inputStream = serialPort.getInputStream();

			serialPort.setSerialPortParams(1000000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			int counter = 0;

			while (true) {
				System.out.println("Looking for image");

				while (!isImageStart(inputStream, 0)) {
				}
				;
				System.out.println("img START: " + counter);

				int[][] pxmap = new int[WIDTH][HEIGHT];
				int isZero = 0; // number of isZeros in img tells alot about how much it is corrupted
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						int temp = read(inputStream);
						pxmap[x][y] = temp;
						if (temp == 0)
							isZero++;
					}
				}
				// bayer raw to rgb(see
				// https://documentation.euresys.com/Products/MultiCam/MultiCam_6_16/Content/MultiCam_6_7_HTML_Documentation/100280.htm:
				// Each pixel only has one given value for either R,G o B.
				// To get the missing values you use neighboring pixels that have the value
				// given.
				// you calculate:

				// For red pixel locations (case of R22)
				// G <= Mean4(GN, GS, GE, GW)
				// B <= Mean4(BNE, BSE, BSW, BNW)

				// For green pixel locations in lines with blue (case of G23)
				// R <= Mean4(RN, RS)
				// B <= Mean4(BE, BW)

				// For green pixel locations in lines with red (case of G32)
				// R <= Mean2(RE, RW)
				// B <= Mean2(BN, BS)

				// For blue pixel locations (case of B33)
				// G <= Mean4(GN, GS, GE, GW)
				// R <= Mean4(RNE, RSE, RSW, RNW)

				// optionally you can use
				// Median2Of4(a,b,c,d) =
				// Mean2{ Min [ Max(a,b), Max(c,d) ] , Max [Min(a,b),Min(c,d) ] }
				// instead

				// Ill use the simple method
				// I make an array for each colorplane and combine the colorplains at the end.
				// I suppose top row is B G B G not R G R G
				int[][] rPlane = new int[WIDTH][HEIGHT];
				int[][] gPlane = new int[WIDTH][HEIGHT];
				int[][] bPlane = new int[WIDTH][HEIGHT];

				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						if (y % 2 == 0) {
							// sidenote: I spent 2 hours until I realized that my images weren't colorful
							// because I switched up the content of the if satements.

							if (x % 2 == 0) { // blue

								rPlane[x][y] = compAVRG(new int[][] { { x - 1, y - 1 }, { x + 1, y - 1 },
										{ x + 1, y + 1 }, { x - 1, y + 1 } }, pxmap);
								gPlane[x][y] = compAVRG(
										new int[][] { { x, y - 1 }, { x + 1, y }, { x, y + 1 }, { x - 1, y } }, pxmap);
								bPlane[x][y] = pxmap[x][y];

							} else { // green inline with blue

								rPlane[x][y] = compAVRG(new int[][] { { x, y - 1 }, { x, y + 1 } }, pxmap);
								gPlane[x][y] = pxmap[x][y];
								bPlane[x][y] = compAVRG(new int[][] { { x - 1, y }, { x + 1, y } }, pxmap);

							}
						} else {
							if (x % 2 == 1) { // red
								rPlane[x][y] = pxmap[x][y];
								gPlane[x][y] = compAVRG(
										new int[][] { { x, y - 1 }, { x + 1, y }, { x, y + 1 }, { x - 1, y } }, pxmap);
								bPlane[x][y] = compAVRG(new int[][] { { x - 1, y - 1 }, { x + 1, y - 1 },
										{ x + 1, y + 1 }, { x - 1, y + 1 } }, pxmap);

							} else { // green inline with red
								rPlane[x][y] = compAVRG(new int[][] { { x - 1, y }, { x + 1, y } }, pxmap);
								gPlane[x][y] = pxmap[x][y];
								bPlane[x][y] = compAVRG(new int[][] { { x, y - 1 }, { x, y + 1 } }, pxmap);

							}
						}

					}
				}

				LocalDateTime now = LocalDateTime.now();

				int hour = now.getHour();
				int minute = now.getMinute();
				int second = now.getSecond();

				// saved as BMP .ppm
				// EXAMPLE:
				// P3 <- specific type
				// 3 2 #<-width height
				// 255 #<- color range
				// 255 0 0 0 255 0 0 0 255 <- r g b r g b r g b ....
				// 255 255 0 255 255 255 0 0 0

				try {
					FileOutputStream fos = new FileOutputStream(new File("c:/out/" + counter + "_t" + hour + "_"
							+ minute + "_" + second + "_isZero_" + isZero + ".ppm"));
					fos.write(("P3\n").getBytes(StandardCharsets.US_ASCII));
					// flip width and height to get an upside img
					fos.write((HEIGHT + " " + WIDTH + "\n").getBytes(StandardCharsets.US_ASCII));
					fos.write(("255\n").getBytes(StandardCharsets.US_ASCII));
					for (int x = WIDTH - 1; x >= 0; x--) {
						String row = "";

						for (int y = 0; y < HEIGHT; y++) {
							String str = "";
							// save as rgb
							str += rPlane[x][y] + " ";
							str += gPlane[x][y] + " ";
							str += bPlane[x][y] + " ";
							row += str;

						}
						fos.write((row + "\n").getBytes(StandardCharsets.US_ASCII));
					}

					fos.close();
					System.out.println("Saved pxmap: " + (counter) + " isZeros: " + isZero);

				} catch (IOException e) {
					throw new IllegalStateException(e);
				}

				// bayer_raw ppm / bitmap
				/*
				 * try { FileOutputStream fos = new FileOutputStream( new File("c:/out/" +
				 * counter + "_noCOLOR_" + hour + "_" + minute + "_" + second + ".ppm"));
				 * fos.write(("P3\n").getBytes(StandardCharsets.US_ASCII)); fos.write((WIDTH +
				 * " " + HEIGHT + "\n").getBytes(StandardCharsets.US_ASCII));
				 * fos.write(("255\n").getBytes(StandardCharsets.US_ASCII)); for (int y = 0; y <
				 * HEIGHT; y++) { String row = ""; for (int x = 0; x < WIDTH; x++) { String temp
				 * = Integer.toString(pxmap[x][y]); String str; if (y % 2 == 0) { if (x % 2 ==
				 * 0) { // blue str = "0 0 " + temp + " "; } else { //green
				 * 
				 * str = "0 " + temp + " 0 "; } } else { if (x % 2 == 1) { // blue str = temp +
				 * " 0 0 "; } else { //green
				 * 
				 * str = "0 " + temp + " 0 "; } }
				 * 
				 * row += str; } fos.write((row + "\n" ).getBytes(StandardCharsets.US_ASCII)); }
				 * 
				 * fos.close(); System.out.println("Saved no color: " + (counter));
				 * 
				 * } catch (IOException e) { throw new IllegalStateException(e); }
				 */
				counter++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private int read(InputStream inputStream) throws IOException {
		int temp = (char) inputStream.read();
		if (temp == -1) {
			throw new IllegalStateException("Exit");
		}
		return temp;
	}

	private boolean isImageStart(InputStream inputStream, int index) throws IOException {
		int temp = read(inputStream);
		if (index < IMAGE_START.length) {
			if (IMAGE_START[index] == temp) {
				return isImageStart(inputStream, ++index);
			} else {
				return false;
			}
		}
		return true;
	}

	private int compAVRG(int[][] arr, int[][] pxmap) {
		int divider = 0;
		int val = 0;

		for (int i = 0; i < arr.length; i++) {
			int temp[] = arr[i];
			if ((temp[0] + 1 <= 0 || temp[0] + 1 >= WIDTH || temp[1] + 1 <= 0 || temp[1] + 1 >= HEIGHT
					|| temp.length == 0))
				continue;
			divider++;
			val += pxmap[temp[0]][temp[1]];

		}

		if (divider == 0) {
			return 0;
		}
		return (int) (val / divider);

	}

}
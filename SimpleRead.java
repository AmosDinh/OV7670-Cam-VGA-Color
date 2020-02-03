// compiled with Java 8 
// javac --release 8 -cp "comm.jar" BMP.java SimpleRead.java
//get major version:  javap -verbose myClass | findstr "major"

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
	private static final char[] IMAGE_END = { '*', 'F', 'I', 'N', '*' };
	private static final int WIDTH = /* 320 */ 320 * 2;

	private static final int HEIGHT = /* 240 */ 240 * 2;
	private List<Integer> bayer_raw_rgb = new ArrayList<>();

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
		int[][] rgb = new int[HEIGHT][WIDTH];
		int[][] rgb2 = new int[WIDTH][HEIGHT];

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
				/*
				 * while (!isImageEnd_and_add_to_bayer_raw_list(inputStream, 0)) { } ;
				 * System.out.println("img END: " + counter); System.out.println("bayer size: "
				 * + bayer_raw_rgb.size()); bayer_raw_rgb.removeAll(bayer_raw_rgb);
				 */

				int[][] pxmap = new int[WIDTH][HEIGHT];
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						int temp = read(inputStream);
						pxmap[x][y] = temp;
					}
				}

				// BMP .ppm

				// P3
				// # Ein Farbbild der Größe 3 × 2 Pixel, maximaler Helligkeit 255.
				// # Darauf folgen die RGB-Tripel.
				// 3 2
				// 255
				// 255 0 0 0 255 0 0 0 255
				// 255 255 0 255 255 255 0 0 0

				LocalDateTime now = LocalDateTime.now();
				/*
				 * int year = now.getYear(); int month = now.getMonthValue(); int day =
				 * now.getDayOfMonth();
				 */
				int hour = now.getHour();
				int minute = now.getMinute();
				int second = now.getSecond();

				try {
					FileOutputStream fos = new FileOutputStream(
							new File("c:/out/" + counter + "_t_" + hour + "_" + minute + "_" + second + ".ppm"));
					fos.write(("P3\n").getBytes(StandardCharsets.US_ASCII));
					fos.write((WIDTH + " " + HEIGHT + "\n").getBytes(StandardCharsets.US_ASCII));
					fos.write(("255\n").getBytes(StandardCharsets.US_ASCII));
					for (int y = 0; y < HEIGHT; y++) {
						String row = "";
						for (int x = 0; x < WIDTH; x++) {
							String temp = Integer.toString(pxmap[x][y]) + " ";
							String str;
							if (y % 2 == 0) {
								if (x % 2 == 0) { // green
									str = "0 " + temp + " 0";
								} else { // blue
									str = "0 0 " + temp;
								}
							} else {
								if (x % 2 == 1) {
									// green
									str = "0 " + temp + " 0";
								} else { // red
									str = temp + " 0 0";
								}
							}
							row += str + " ";
						}
						fos.write((row + "\n").getBytes(StandardCharsets.US_ASCII));
					}

					fos.close();
					System.out.println("Saved pxmap: " + (counter++));
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}

				/* System.out.println(array[i]); */
				/*
				 * if(i%4==0){ System.out.println("Cb"+cbs+" "+temp);
				 * 
				 * }else if(((i+1)%4)==0){ System.out.println("Y"+(ysr++)+" "+temp); }else
				 * if(((i+2)%4)==0){ System.out.println("Cr"+(cbs++)+" "+temp); cbs++; }else{
				 * System.out.println("Y"+(ysr++)+" "+temp); }
				 */

				/*
				 * double cb = array[i];
				 * 
				 * double y1= array[i +1]; double cr= array[i +2];
				 * 
				 * double y2= array[i +3]; double r1 = y1 +cr/0.877; double r2 = y2 +cr/0.877;
				 * double g1 = y1 - 0.39393*cb - 0.58081*cr;
				 * 
				 * double g2 = y2 - 0.39393*cb - 0.58081*cr; double b1 = y1 + cb/0.493; double
				 * b2 =y2 + cb/0.493; System.out.println((int)r1+" "+(int)g1+" "+(int)b1);
				 * System.out.println((int)r1+" "+(int)g1+" "+(int)b1);
				 */
				/*
				 * System.out.println(y1+" "+cb+" "+cr); System.out.println(y2+" "+cb+" "+cr);
				 */

				/*
				 * while(bayer_raw_length>counters){ int temp = read(inputStream);
				 * 
				 * System.out.println("Binary: "+Integer.toBinaryString(temp));
				 * System.out.println("YUV: "+(counters%3)+" "+temp);
				 * System.out.println("Binary: "+Integer.toBinaryString(temp& 0xFF));
				 * System.out.println("Binary: "+Integer.toBinaryString(((temp& 0xFF) << 8)));
				 * System.out.println("Binary: "+Integer.toBinaryString(((temp& 0xFF) << 16)));
				 * System.out.println("Binary: "+Integer.toBinaryString(((temp& 0xFF) << 32)));
				 * System.out.println("Binary: "+Integer.toBinaryString(((temp& 0xFF) << 64)));
				 * System.out.println("stream: " + bayer_raw_length + " "+temp+" "+(temp&
				 * 0xFF)+" "+((temp & 0xFF) << 8)+" "+((temp & 0xFF) << 16) ); counters++; }
				 */

				/*
				 * for (int y = 0; y < HEIGHT; y++) { for (int x = 0; x < WIDTH; x++) { int temp
				 * = read(inputStream); if(temp==48){System.out.println(temp);} else
				 * if(temp==111){System.out.println(temp);}
				 * 
				 * // r rgb[y][x] = ((temp & 0xFF) << 16) | ((temp & 0xFF) << 8) | (temp &
				 * 0xFF); //arr: 16777215 = 16711680 65280 255 "plus"? } } //rgb2 flips image
				 * from rot(90deg) to rot(0) for (int y = 0; y < HEIGHT; y++) { for (int x = 0;
				 * x < WIDTH; x++) { rgb2[x][y] = rgb[y][x]; } }
				 * 
				 * BMP bp = new BMP(); bp.saveBMP("c:/out/" + (counter++) + ".bmp", rgb2);
				 * System.out.println("Saved image: " + counter);
				 */

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

	private boolean isImageEnd_and_add_to_bayer_raw_list(InputStream inputStream, int index) throws IOException {
		int temp = read(inputStream);
		bayer_raw_rgb.add(temp);
		if (index < IMAGE_END.length) {
			if (IMAGE_END[index] == temp) {
				return isImageEnd_and_add_to_bayer_raw_list(inputStream, ++index);
			} else {
				return false;
			}
		}
		return true;
	}
}
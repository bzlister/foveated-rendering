import org.jcodec.common.model.Picture;
import org.jcodec.api.FrameGrab;
import java.io.*;
import java.util.ArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.api.JCodecException;
import java.awt.Frame;
import javax.swing.Timer;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.image.*;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.scale.Yuv420jToRgb;
import java.lang.Math;
import java.util.HashMap;
import java.net.DatagramSocket;

public class Server {
	
	private DatagramSocket socket;
	
	public static void main(String[] args){
		Server server = new Server();
		server.run();
	}
	
	public void run(){
		DatagramSocket socket = null;
		InetAddress address = null;
		try{
			socket = new DatagramSocket();
			address = InetAddress.getByName("localhost");
		} catch (Exception e){
			System.out.println("Error initializing server");
		}
		File file = new File("Wildlife.mp4");
		int w = 0;
		int h = 0;
		try{
			Picture pic = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file)).getNativeFrame();
			w = pic.getWidth();
			h = pic.getHeight();
		} catch (Exception e){
			System.out.println("Exception determining video dimensions");
		}
		int threshold = 10;
		try{
			FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
			Picture picture;
			boolean first = true;
			boolean change = true;
			Yuv420jToRgb converter = new Yuv420jToRgb();
			byte[] previous = new byte[0];
			while (null != (picture = grab.getNativeFrame())) {
				Picture p = Picture.create(w, h, ColorSpace.RGB);
				converter.transform(picture, p);
				byte[] frame = p.getData()[0];
				if (first){
					DatagramPacket packet = new DatagramPacket(frame, frame.length);
					socket.send(packet);
					int x = 2;
					while (x < frame.length){
						int value = new Color((int)frame[x-2]+128, (int)frame[x-1]+128, (int)frame[x]+128).getRGB();
						//image.setRGB((x/3)%w, (x/3)/w, value);
						x+=3;
					}
					first = false;
				}
				else{
					int r = 6/0;
					change = false;
					for (int x = 2; x < frame.length; x+=3){
						int[] rgb = new int[]{(int)previous[x-2]+128, (int)previous[x-1]+128, (int)previous[x]+128};
						if (Math.abs((int)previous[x-2] - (int)frame[x-2]) > threshold){
							rgb[0] = (int)frame[x-2]+128;
							change = true;
						}
						if (Math.abs((int)previous[x-1] - (int)frame[x-1]) > threshold){
							rgb[1] = (int)frame[x-1]+128;
							change = true;
						}
						if (Math.abs((int)previous[x] - (int)frame[x]) > threshold){
							rgb[2] = (int)frame[x]+128;
							change = true;
						}
						if (change){
							//image.setRGB((x/3)%w, (x/3)/w, new Color(rgb[0], rgb[1], rgb[2]).getRGB());
						}
					}
				}
				if (change){
					DatagramPacket packet = new DatagramPacket(frame, frame.length, address, 4445);
					socket.send(packet);
					//graph.setImage(image);
				}
				previous = frame;
			}
		} catch (FileNotFoundException fn){
			System.out.println("File not found");
		} catch (IOException i){
			System.out.println("IO exception");
		} catch (JCodecException j){
			System.out.println("JCodec exception");
		}
	}
}
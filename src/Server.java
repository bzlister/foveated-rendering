import org.jcodec.common.model.Picture;
import org.jcodec.api.FrameGrab;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.api.JCodecException;
import java.net.*;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.scale.Yuv420jToRgb;
import java.net.DatagramSocket;
import java.util.*;

public class Server extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
	private ServerSocket serverSocket;
	private Socket tcpSocket;
	private DataOutputStream stream;
	private File file;
	private int w;
	private int h;
	private int port;
	private int tcpPort;
	private int ldThresh;
	private int hdThresh;

    public Server(int port, int tcpPort, InetAddress address, int ldThresh, int hdThresh, String path) {
    	this.port = port;
    	this.address = address;
    	this.tcpPort = tcpPort;
    	this.ldThresh = ldThresh;
    	this.hdThresh = hdThresh;
		try{
			socket = new DatagramSocket();
			file = new File(path);
			Picture pic = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file)).getNativeFrame();
			w = pic.getWidth();
			h = pic.getHeight();
		} catch (SocketException s){
			s.printStackTrace();
			System.out.println("Error initializing server");
		} catch (UnknownHostException u){
			u.printStackTrace();
			System.out.println("Error initializing server");
		} catch (FileNotFoundException f){
			System.out.println("File not found");
		} catch (IOException i){
			i.printStackTrace();
			System.out.println("Error determining video dimensions");
		} catch (JCodecException j){
			j.printStackTrace();
			System.out.println("Error determining video dimensions");
		}
    }

    @Override
    public void run(){
		try{
			System.out.println("Server listening for TCP connection...");
			serverSocket = new ServerSocket(tcpPort);
			tcpSocket = serverSocket.accept();
			System.out.println("Client connected on TCP");
			stream = new DataOutputStream(tcpSocket.getOutputStream());
			FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
			Picture picture;
			boolean first = true;
			boolean change = true;
			Yuv420jToRgb converter = new Yuv420jToRgb();
			byte[] previous = new byte[0];
			while (null != (picture = grab.getNativeFrame())) {
				if (first){
					picture = grab.getNativeFrame();
					picture = grab.getNativeFrame();
				}
				Picture p = Picture.create(w, h, ColorSpace.RGB);
				converter.transform(picture, p);
				byte[] frame = p.getData()[0];
				if (first){
					StringBuilder builder = new StringBuilder();
					for (byte b : frame){
						builder.append(b + " ");
					}
					write(builder.toString());
					stream.writeInt(w);
					stream.writeInt(h);
					stream.writeInt(frame.length);
					stream.write(frame);
					first = false;
				}
				else {
					HashMap<Integer, Integer> compressedFrame = new HashMap<>();
					change = false;
					int threshold = -1;
					for (int x = 2; x < frame.length; x += 3) {
						if (((x/3)/w > h/3) && ((x/3)/w < 6*h/7)){ //Middle band
							threshold = hdThresh;
						}
						else {
							threshold = ldThresh;
						}
						byte[] rgb = new byte[]{previous[x - 2], previous[x - 1], previous[x]};
						if (Math.abs((int) previous[x - 2] - (int) frame[x - 2]) > threshold) {
							rgb[0] = frame[x - 2];
							change = true;
						}
						if (Math.abs((int) previous[x - 1] - (int) frame[x - 1]) > threshold) {
							rgb[1] = frame[x - 1];
							change = true;
						}
						if (Math.abs((int) previous[x] - (int) frame[x]) > threshold) {
							rgb[2] = frame[x];
							change = true;
						}
						if (change){
							compressedFrame.put(x, new Color((int)rgb[0]+128, (int)rgb[1]+128, (int)rgb[2]+128).getRGB());
						}
					}
					byte[] toBeSent = new byte[6*compressedFrame.size()]; //6 bytes needed for every int, int pair
					int i = 0;
					if (toBeSent.length > 0) {
						for (Integer x : compressedFrame.keySet()) {
							toBeSent[i] = (byte) ((x & 0x00FF0000) >> 16);
							toBeSent[i + 1] = (byte) ((x & 0x0000FF00) >> 8);
							toBeSent[i + 2] = (byte) ((x & 0x000000FF) >> 0);
							int val = compressedFrame.get(x);

							toBeSent[i + 3] = (byte) ((val & 0x00FF0000) >> 16);
							toBeSent[i + 4] = (byte) ((val & 0x0000FF00) >> 8);
							toBeSent[i + 5] = (byte) ((val & 0x000000FF) >> 0);
							i += 6;
						}
						stream.writeInt(toBeSent.length);
						//DatagramPacket packet = new DatagramPacket(toBeSent, toBeSent.length, address, port);
						//socket.send(packet);
						stream.write(toBeSent);
					}
				}
				previous = frame;
			}
			stream.close();
			tcpSocket.close();
			socket.close();
			serverSocket.close();
		} catch (IOException i){
			i.printStackTrace();
			System.out.println("IO exception");
		} catch (JCodecException j){
			j.printStackTrace();
			System.out.println("JCodec exception");
		}
	}

	private void write(String s){
		try {
			FileWriter fileWriter = new FileWriter("C:/Users/bzlis/Documents/server.txt");
			fileWriter.write(s);
			fileWriter.close();
		} catch (Exception e){
			System.out.println("Couldnt write");
		}
	}
}
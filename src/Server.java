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
import java.lang.Math;

public class Server extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
	private ServerSocket serverSocket;
	private Socket tcpSocket;
	private DataOutputStream stream;
	private DataInputStream in;
	private File file;
	private int w;
	private int h;
	private int port;
	private int tcpPort;
	private int ldThresh;
	private int hdThresh;
	private int SPATIAL_THRESH_HD = 0;
	private int SPATIAL_THRESH_LD = 3;
	private final double RADIUS_ANGLE = Math.tan((Math.PI/180)*7.5);

    public Server(int port, int tcpPort, InetAddress address, int ldThresh, int hdThresh, int spatial_ld, int spatial_hd) {
    	this.port = port;
    	this.address = address;
    	this.tcpPort = tcpPort;
    	this.ldThresh = ldThresh;
    	this.hdThresh = hdThresh;
		SPATIAL_THRESH_LD = spatial_ld;
    	SPATIAL_THRESH_HD = spatial_hd;
		try{
			socket = new DatagramSocket();
			file = new File("C:/users/bzlis/IdeaProjects/foveated-rendering/src/main/resources/Wildlife.mp4");
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
			// Setting up connections
			System.out.println("Server listening for TCP connection...");
			serverSocket = new ServerSocket(tcpPort);
			tcpSocket = serverSocket.accept();
			System.out.println("Client connected on TCP");
			stream = new DataOutputStream(tcpSocket.getOutputStream());
			in = new DataInputStream(tcpSocket.getInputStream());

			// Extracting frames from video
			FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
			Picture picture;
			boolean first = true;
			boolean change = true;
			Yuv420jToRgb converter = new Yuv420jToRgb();
			byte[] previous = new byte[0];
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			while (null != (picture = grab.getNativeFrame())) {
				if (first){
					picture = grab.getNativeFrame();
					picture = grab.getNativeFrame();
				}
				Picture p = Picture.create(w, h, ColorSpace.RGB);
				converter.transform(picture, p);
				byte[] frame = p.getData()[0];

				// First frame protocol
				if (first){
					stream.writeInt(w);
					stream.writeInt(h);
					LinkedList<Integer> referencePoints = interFrameEncode(frame, w/2, h/2, 1000);
					byte[] toBeSent = new byte[6*referencePoints.size()];
					int i = 0;
					if (toBeSent.length > 0) {
						for (Integer x : referencePoints) {
							toBeSent[i] = (byte) ((x & 0x00FF0000) >> 16);
							toBeSent[i + 1] = (byte) ((x & 0x0000FF00) >> 8);
							toBeSent[i + 2] = (byte) ((x & 0x000000FF) >> 0);
							toBeSent[i + 3] = frame[x-2]; // R
							toBeSent[i + 4] = frame[x-1]; // G
							toBeSent[i + 5] = frame[x]; // B
							i += 6;
						}
						stream.writeInt(toBeSent.length);
						stream.write(toBeSent);
						first = false;
						previous = frame;
					}
				}

				// Subsequent frame protocol
				else {
					int gazeX = in.readInt();
					int gazeY = in.readInt();
					int gazeR = in.readInt();
					double r = gazeR*RADIUS_ANGLE;
					if (r == Double.NaN){
						r = 2.0*h/3;
					}
					LinkedList<Integer> referencePoints = interFrameEncode(frame, gazeX, gazeY, r);
					change = false;
					int threshold = -1;
					ListIterator<Integer> iter = referencePoints.listIterator();
					while (iter.hasNext()){
						int x = iter.next();
						if (Math.pow((x/3)%w - gazeX, 2) + Math.pow((x/3)/w - gazeY, 2) < r*r){
							threshold = hdThresh;
						}
						else {
							threshold = ldThresh;
						}
						if (Math.abs((int) previous[x - 2] - (int) frame[x - 2]) > threshold) {
							change = true;
						}
						if (Math.abs((int) previous[x - 1] - (int) frame[x - 1]) > threshold) {
							change = true;
						}
						if (Math.abs((int) previous[x] - (int) frame[x]) > threshold) {
							change = true;
						}
						if (!change){
							iter.remove();
						}
					}
					byte[] toBeSent = new byte[6*referencePoints.size()]; //6 bytes needed for every int, int pair
					int i = 0;
					if (toBeSent.length > 0) {
						int old = 2;
						byte[] oldVals = new byte[]{previous[0], previous[1], previous[2]};
						for (Integer x : referencePoints) {
							while (old < x){
								previous[old-2] = oldVals[0];
								previous[old-1] = oldVals[1];
								previous[old] = oldVals[2];
								old++;
							}
							previous[x-2] = frame[x-2];
							previous[x-1] = frame[x-1];
							previous[x] = frame[x];
							toBeSent[i] = (byte) ((x & 0x00FF0000) >> 16);
							toBeSent[i + 1] = (byte) ((x & 0x0000FF00) >> 8);
							toBeSent[i + 2] = (byte) ((x & 0x000000FF) >> 0);
							toBeSent[i + 3] = frame[x-2];
							toBeSent[i + 4] = frame[x-1];
							toBeSent[i + 5] = frame[x];
							i += 6;
							old = x+2;
						}
						stream.writeInt(toBeSent.length);
						//DatagramPacket packet = new DatagramPacket(toBeSent, toBeSent.length, address, port);
						//socket.send(packet);
						stream.write(toBeSent);
					}
				}
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

	private LinkedList<Integer> interFrameEncode(byte[] frame, int gazeX, int gazeY, double r){
    	LinkedList<Integer> referencePoints = new LinkedList<Integer>();
    	int thresh = SPATIAL_THRESH_HD;
    	int[] reference = new int[]{(int)frame[0], (int)frame[1], (int)frame[2]};
    	referencePoints.add(2);
    	for (int x = 5; x < frame.length; x+=3){
			if (Math.pow((x/3)%w - gazeX, 2) + Math.pow((x/3)/w - gazeY, 2) < r*r){ // In-focus region
				thresh = SPATIAL_THRESH_HD;
			}
			else {
				thresh = SPATIAL_THRESH_LD;
			}
			if ((x/3)%w ==2
					||((Math.abs(reference[0] - (int) frame[x - 2]) > thresh)
						||(Math.abs(reference[1] - (int) frame[x - 1]) > thresh)
						||(Math.abs(reference[2] - (int) frame[x]) > thresh))){
				referencePoints.add(x);
				reference = new int[]{(int) frame[x-2], (int)frame[x-1], (int)frame[x]};
			}
		}
    	return referencePoints;
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
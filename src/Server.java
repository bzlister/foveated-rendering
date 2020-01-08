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
	private final double RADIUS_ANGLE = Math.tan((Math.PI/180)*7.5);
	private VideoFrameProducer producer;

    public Server(int port, int tcpPort, InetAddress address, VideoFrameProducer producer) {
    	this.port = port;
    	this.address = address;
    	this.tcpPort = tcpPort;
    	this.producer = producer;
		try{
			socket = new DatagramSocket();
			file = new File("C:/users/bzlis/IdeaProjects/foveated-rendering/src/main/resources/Wildlife.mp4");
			FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
			Picture pic = grab.getNativeFrame();
			w = pic.getWidth();
			h = pic.getHeight();
			producer.init(grab, w, h);
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
			stream.writeInt(w);
			stream.writeInt(h);
			boolean cont = true;
			while (cont){
				int gazeX = in.readInt();
				int gazeY = in.readInt();
				int gazeR = in.readInt();
				double r = gazeR*RADIUS_ANGLE;
				if (r == Double.NaN){
					r = 2.0*h/3;
				}
				byte[] toBeSent = producer.getNextFrame(gazeX, gazeY, r);
				stream.writeInt(toBeSent.length);
				stream.write(toBeSent);
			}
			stream.close();
			tcpSocket.close();
			socket.close();
			serverSocket.close();
		} catch (IOException i){
			i.printStackTrace();
			System.out.println("IO exception");
		} catch (EndOfVideoException v){
			v.printStackTrace();
			System.out.println("End of video exception");
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
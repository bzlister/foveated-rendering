import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.Socket;

public class Client extends Thread {

	private DatagramSocket socket;
	private Socket tcpSocket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private boolean running;
	private int port;
	private int tcpPort;
	private InetAddress address;

	public Client(int port, int tcpPort, InetAddress address) {
		this.port = port;
		this.tcpPort = tcpPort;
		this.address = address;
		try{
			socket = new DatagramSocket(port);
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Error initializing client");
		}
	}

	@Override
	public void run() {
		running = true;
		BufferedImage image = null;
		VideoPlayer player = null;
		int w = -1;
		int h = -1;
		try{
			System.out.println("Client trying to connect TCP");
			tcpSocket = new Socket(address, tcpPort);
			dis = new DataInputStream(tcpSocket.getInputStream());
			dos = new DataOutputStream(tcpSocket.getOutputStream());
			w = dis.readInt();
			h = dis.readInt();
			System.out.println("Client TCP connected");
			int size = dis.readInt();
			byte[] initial = new byte[size];
			dis.readFully(initial);
			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			int x = 2;
			while (x < initial.length){
				int value = new Color((int)initial[x-2]+128, (int)initial[x-1]+128, (int)initial[x]+128).getRGB();
				image.setRGB((x/3)%w, (x/3)/w, value);
				x+=3;
			}
			File outputfile = new File("C:/Users/bzlis/Documents/image.jpg");
			ImageIO.write(image, "jpg", outputfile);
			player = new VideoPlayer(w, h);
			player.loadFrame(image);
		} catch (IOException i){
			System.out.println("Client error initializing TCP connection");
			i.printStackTrace();
		}
		while (running) {
			try{
				int[] gaze = getGaze();
				dos.writeInt(gaze[0]);
				dos.writeInt(gaze[1]);
				dos.writeInt(gaze[2]);
				int size = dis.readInt();
				byte[] received = new byte[size];
				dis.readFully(received);
				int i = 0;
				int val = image.getRGB(0, 0);
				int oldX = 0;
				while (i < received.length-6){
					int x = ((received[i]&0xFF) << 16) + ((received[i+1]&0xFF) << 8) + (received[i+2]&0xFF);
					while (oldX < x){
						image.setRGB((x/3)%w, (x/3)/w, val);
						oldX += 1;
					}
					val = ((-1&0xFF) << 24) + ((received[i+3]&0xFF) << 16) + ((received[i+4]&0xFF) << 8) + (received[i+5]&0xFF);
					image.setRGB((x/3)%w, (x/3)/w, val);
					i+=6;
				}
				player.loadFrame(image);
			} catch (Exception e){
				e.printStackTrace();
				System.out.println("Client error");
				running = false;
			}
		}
		try {
			socket.close();
			tcpSocket.close();
			dis.close();
		} catch (Exception e){
			System.out.println("Client error relinquishing connections");
			e.printStackTrace();
		}
	}

	//Placeholder method for getting eyetracker information
	private int[] getGaze(){
		return new int[]{0, 0, 0};
	}

	private void write(String s){
		try {
			FileWriter fileWriter = new FileWriter("C:/Users/bzlis/Documents/client.txt");
			fileWriter.write(s);
			fileWriter.close();
		} catch (Exception e){
			System.out.println("Couldnt write");
		}
	}
}
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
import java.net.InetAddress;
import java.net.DatagramSocket;

public class VideoExample {
	
	public static void main(String[] args){
		VideoExample v = new VideoExample();
		v.run();
	}
	
	public void run(){
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
		JFrame f = new JFrame("Client");
		f.pack();
		GraphicsPanel graph = new GraphicsPanel();
		graph.setBounds(0, 0, w, h);

		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		f.setSize(new Dimension((int)(w*1.1), (int)(h*1.1)));
		f.getContentPane().add(graph, BorderLayout.CENTER);
		f.setVisible(true);
		
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		
		int ldThresh = 16;
		int hdThresh = 16;
		int changeCount = 0;
		int sameCount = 0;
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
					int x = 2;
					while (x < frame.length){
						int value = new Color((int)frame[x-2]+128, (int)frame[x-1]+128, (int)frame[x]+128).getRGB();
						image.setRGB((x/3)%w, (x/3)/w, value);
						x+=3;
					}
					first = false;
				}
				else{
					change = false;
					int threshold = ldThresh;
					for (int x = 2; x < frame.length; x+=3){
						if (((x/3)/w > h/3) && ((x/3)/w < 6*h/7)){ //Middle band
							threshold = hdThresh;
						}
						else {
							threshold = ldThresh;
						}
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
							image.setRGB((x/3)%w, (x/3)/w, new Color(rgb[0], rgb[1], rgb[2]).getRGB());
							changeCount+=1;
						}
						else{
							sameCount+=1;
						}
					}
				}
				if (change){
					graph.setImage(image);
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
		System.out.println(((double)changeCount)/(changeCount+sameCount));
	}
	
	private class GraphicsPanel extends JPanel {

		private BufferedImage image;
		
		public void setImage(BufferedImage image){
			this.image = image;
			setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
			this.revalidate();
			this.repaint();
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	}
}
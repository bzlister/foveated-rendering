package com.bzlis.video.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class VideoPlayer extends JFrame {

    private GraphicsPanel graph;

    public VideoPlayer(int w, int h){
        JFrame f = new JFrame("Client");
        f.pack();
        graph = new GraphicsPanel();
        graph.setBounds(0, 0, w, h);

        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        f.setSize(new Dimension((int)(w*1.1), (int)(h*1.1)));
        f.getContentPane().add(graph, BorderLayout.CENTER);
        f.setVisible(true);
    }

    public void loadFrame(BufferedImage image){
        graph.setImage(image);
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

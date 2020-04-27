package com.bzlis.video;

import com.bzlis.video.client.Client;
import com.bzlis.video.server.SimpleSpatialCompression;
import com.bzlis.video.server.SimpleTemporalCompression;
import com.bzlis.video.server.SimpleVideoFrameProducer;
import com.bzlis.video.server.Server;

import java.net.InetAddress;

public class ClientServerExample {
    public static void main(String[] args) {
        InetAddress address = null;
        try{
            address = InetAddress.getByName("localhost");
        } catch (Exception e){
            System.out.println("Could not resolve IP address");
        }
        SimpleVideoFrameProducer producer = new SimpleVideoFrameProducer(new SimpleSpatialCompression(24, 2), new SimpleTemporalCompression(10, 2));
        Server server = new Server(5000, 6001, address, producer);
        Client client = new Client(5000, 6001, address);
        server.start();
        client.start();
    }
}

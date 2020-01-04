import java.net.InetAddress;

public class ClientServerExample {
    public static void main(String[] args) {
        InetAddress address = null;
        try{
            address = InetAddress.getByName("localhost");
        } catch (Exception e){
            System.out.println("Could not resolve IP address");
        }
        Server server = new Server(5000, 6001, address, 32, 2);
        Client client = new Client(5000, 6001, address);
        server.start();
        client.start();
    }
}

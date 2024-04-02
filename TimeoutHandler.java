import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {
    private DatagramSocket UDPSocket;
    private DatagramPacket packet;
    private int sequenceNumber;

    public TimeoutHandler(DatagramSocket UDPSocket, DatagramPacket packet, int sequenceNumber) {
        this.UDPSocket = UDPSocket;
        this.packet = packet;
        this.sequenceNumber = sequenceNumber;
    }

    public void run() {
        System.out.println("timeout");
        try {
            UDPSocket.send(packet);
            System.out.println("retx " + sequenceNumber);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

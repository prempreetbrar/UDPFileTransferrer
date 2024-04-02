import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {
    DatagramSocket UDPSocket;
    DatagramPacket packet;

    public TimeoutHandler(DatagramSocket UDPSocket, DatagramPacket packet) {
        this.UDPSocket = UDPSocket;
        this.packet = packet;
    }

    public void run() {
        // timeout
        try {
            UDPSocket.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

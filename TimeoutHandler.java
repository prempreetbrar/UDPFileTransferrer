/**
 * TimeoutHandler Class
 * 
 * CPSC 441 - L01 - T01
 * Assignment 4
 * 
 * TA: Amir Shani
 * Student: Prempreet Brar
 * UCID: 30112576
 * 
 * Retransmit a packet based on a timer. 
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {
    private DatagramSocket UDPSocket;
    private DatagramPacket packet;
    private int sequenceNumber;

    /**
     * Constructor to initialize the program. 
     * @param UDPSocket The UDP socket being used for transmission.
     * @param packet The datagram that potentially needs to be retransmitted.
     * @param sequenceNumber The sequence number of the packet being retransmitted. 
     */
    public TimeoutHandler(DatagramSocket UDPSocket, DatagramPacket packet, int sequenceNumber) {
        this.UDPSocket = UDPSocket;
        this.packet = packet;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * A method that retransmits the UDP datagram and prints messages to the console. 
     */
    public void run() {
        /* we have this message outside of the try block, in-case the actual retransmission fails. This way,
           we can at least see in the console that we ATTEMPTED retransmission.
         */ 
        System.out.println("timeout");
        try {
            UDPSocket.send(packet);
            System.out.println("retx " + sequenceNumber);
        } 

        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

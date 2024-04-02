/**
 * StopWaitFtp Class
 * 
 * CPSC 441 - L01 - T01
 * Assignment 4
 * 
 * TA: Amir Shani
 * Student: Prempreet Brar
 * UCID: 30112576
 * 
 * Implements a stop-and-wait FTP client using UDP. 
 */

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.logging.*;

public class StopWaitFtp {
	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger

    private static final int EOF = -1;
    private static final int OFFSET = 0;
    private static final int INITIAL_TIMER_DELAY = 0;
    private static final int MULTIPLE_OF_RETRANSMISSION_TIMEOUT = 10;

    private Socket TCPSocket;
    private DatagramSocket UDPSocket;
    private DataInputStream TCPInputStream;
    private DataOutputStream TCPOutputStream;

    private FileInputStream fileInputStream;

    private int serverUDPPortNumber;
    private int currentSequenceNumber;
    private int retransmissionTimeout;
    private int connectionTimeout;
    private Timer timer;


	/**
	 * Constructor to initialize the program 
	 * 
	 * @param timeout		The time-out interval for the retransmission timer, in milli-seconds
	 */
	public StopWaitFtp(int timeout) {
        this.TCPSocket = null;
        this.UDPSocket = null;
        this.TCPInputStream = null;
        this.TCPOutputStream = null;

        this.fileInputStream = null;

        this.retransmissionTimeout = timeout;
        this.connectionTimeout = retransmissionTimeout * MULTIPLE_OF_RETRANSMISSION_TIMEOUT;
        this.timer = new Timer();
    }


	/**
	 * Send the specified file to the specified remote server.
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 * @return 				true if the file transfer completed successfully, false otherwise
	 */
	public boolean send(String serverName, int serverPort, String fileName) {
        boolean wasSuccessful = false;

        try {
            TCPSocket = new Socket(serverName, serverPort);
            UDPSocket = new DatagramSocket();
            TCPInputStream = new DataInputStream(TCPSocket.getInputStream());
            TCPOutputStream = new DataOutputStream(TCPSocket.getOutputStream());

            completeTCPHandshake(fileName);
            sendFile(fileName, serverName);

            wasSuccessful = true;
        } 
        
        /*
         * We catch UnknownHostException first because it is a subclass of IOException. Of course,
         * the behaviour in both catch blocks is identical (printStackTrace), but if we wanted different
         * behaviour in the future, having these different blocks is good practice.
         */
        catch (SocketTimeoutException e) {
            e.printStackTrace();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            closeGracefully(fileInputStream, TCPOutputStream, TCPInputStream, UDPSocket, TCPSocket);
            timer.cancel();
            timer.purge();
        }

        return wasSuccessful;
    }

    /**
     * Completes the TCP handshake process with the server.
     * @param fileName The name of the file whose info will be transmitted over the handshake.
     * @throws IOException
     */
    private void completeTCPHandshake(String fileName) throws IOException {
        TCPOutputStream.writeUTF(fileName);
        TCPOutputStream.writeLong((new File(fileName)).length());
        TCPOutputStream.writeInt(UDPSocket.getLocalPort());
        TCPOutputStream.flush();

        serverUDPPortNumber = TCPInputStream.readInt();
        currentSequenceNumber = TCPInputStream.readInt();
    }

    private void sendFile(String fileName, String serverName) throws SocketTimeoutException, IOException {
        /*
        * The numBytes tells us how many bytes to actually write to the stream; this may
        * be different from the buffer size (ie. if the number of bytes remaining is <
        * buffer.length). This is why we cannot specify buffer.length as the number of bytes being written,
        * as we would get an IndexOutOfBounds exception when we reach the end.
        */
        int numBytes = 0;
        byte[] buffer = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
        fileInputStream = new FileInputStream(fileName);
        UDPSocket.setSoTimeout(connectionTimeout);

        while ((numBytes = fileInputStream.read(buffer)) != EOF) {
            FtpSegment segment = new FtpSegment(currentSequenceNumber, buffer, numBytes);
            DatagramPacket packet = FtpSegment.makePacket(segment, InetAddress.getByName(serverName), serverUDPPortNumber);
            UDPSocket.send(packet);

            TimeoutHandler timerForInFlightPacket = new TimeoutHandler(UDPSocket, packet);
            timer.scheduleAtFixedRate(timerForInFlightPacket, INITIAL_TIMER_DELAY, retransmissionTimeout);
            
            while (true) {
                UDPSocket.receive(packet);
                FtpSegment ack = new FtpSegment(packet);
                int ackNum = ack.getSeqNum();
    
                if ((currentSequenceNumber + 1) == ackNum) {
                    currentSequenceNumber += 1;
                    timerForInFlightPacket.cancel();
                    break;
                }
            }
        }
    } 

    /**
     * Close all opened streams, sockets, and other resources before terminating the program.
     *
     * @param resources all resources which need to be closed
     */
    private void closeGracefully(Closeable... resources) {
        /*
         * We need to surround this with a try-catch block because the closing itself can raise
         * an IOException. In this case, if closing fails, there is nothing else we can do. We must also
         * ensure the resource is not null. This is because other parts of the program instantiate certain
         * resources to null before reassignment.
         */
        try {
            for (Closeable resource : resources) {
                if (resource != null) {
                    resource.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
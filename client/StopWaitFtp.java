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
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.util.Timer;
import java.util.logging.*;

public class StopWaitFtp {
	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger

    // constants
    private static final int EOF = -1;
    private static final int MULTIPLE_OF_RETRANSMISSION_TIMEOUT = 10;

    // socket variables
    private Socket TCPSocket;
    private DatagramSocket UDPSocket;
    private DataInputStream TCPInputStream;
    private DataOutputStream TCPOutputStream;

    // file variables
    private FileInputStream fileInputStream;

    // miscellaneous info, including timer info
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

            /* we need to create the file object here and save it; we cannot do new File(fileName)
               multiple times, as otherwise Java has unexpected behaviour (the garbage collector may
               not close the connection to the file in time for your new File instance)
            */
            File fileObject = new File(fileName);
            completeTCPHandshake(fileObject);
            sendFile(fileObject, serverName);

            wasSuccessful = true;
        } 
        
        // will occur if server is non-responsive; NOT for retransmission. 
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
            // cancel all yet to be executed tasks
            timer.cancel();
            // remove references to all cancelled tasks so that Java can garbage collect memory
            timer.purge();
        }
        return wasSuccessful;
    }

    /**
     * Completes the TCP handshake process with the server.
     * @param fileObject The fileObject that will eventually be transmitted to the server.
     * @throws IOException
     */
    private void completeTCPHandshake(File fileObject) throws IOException {
        TCPOutputStream.writeUTF(fileObject.getName());
        TCPOutputStream.writeLong(fileObject.length());
        TCPOutputStream.writeInt(UDPSocket.getLocalPort());
        // force TCP to actually send the handshake messages to the server
        TCPOutputStream.flush();

        /* this is the actual port number that we want to use for sending the file; NOT
           the port number used to establish the TCP connection.*/
        serverUDPPortNumber = TCPInputStream.readInt();
        currentSequenceNumber = TCPInputStream.readInt();
    }

    /**
     * sends the file to the server over UDP.
     * @param fileObject The fileObject to transmit to the UDP server.
     * @param serverName The name of the UDP server (hostname). 
     * @throws SocketTimeoutException Thrown if the server is unresponsive. 
     * @throws IOException
     */
    private void sendFile(File fileObject, String serverName) throws SocketTimeoutException, IOException {
        /*
        * The numBytes tells us how many bytes to actually write to the stream; this may
        * be different from the buffer size (ie. if the number of bytes remaining is <
        * buffer.length). This is why we cannot specify buffer.length as the number of bytes being written,
        * as we would get an IndexOutOfBounds exception when we reach the end.
        */
        int numBytes = 0;
        byte[] buffer = new byte[FtpSegment.MAX_PAYLOAD_SIZE];

        fileInputStream = new FileInputStream(fileObject);
        // to check for a non-responsive server
        UDPSocket.setSoTimeout(connectionTimeout);

        while ((numBytes = fileInputStream.read(buffer)) != EOF) {
            FtpSegment segment = new FtpSegment(currentSequenceNumber, buffer, numBytes);
            DatagramPacket packet = FtpSegment.makePacket(segment, InetAddress.getByName(serverName), serverUDPPortNumber);
            UDPSocket.send(packet);
            System.out.println("\nsend " + currentSequenceNumber);

            // the timer first starts after retransmissionTimeout seconds and then repeats until cancelled
            TimeoutHandler timerForInFlightPacket = new TimeoutHandler(UDPSocket, packet, currentSequenceNumber);
            timer.scheduleAtFixedRate(timerForInFlightPacket, retransmissionTimeout, retransmissionTimeout);
            
            /*
             * We need a while loop here when checking for ack packets; this is because we must 
             * wait until we receive an ACK for the packet we just sent (STOP and WAIT; we are 
             * stopping and WAITING). It is possible that the ack we get back is NOT the ack for the
             * packet just sent (maybe it is a duplicate ACK or some other ACK), in which case stop-and-wait
             * just ignores and does nothing (hence the while loop).
             */
            while (true) {
                DatagramPacket ackPacket = new DatagramPacket(new byte[FtpSegment.MAX_SEGMENT_SIZE], FtpSegment.MAX_SEGMENT_SIZE);

                UDPSocket.receive(ackPacket);
                FtpSegment ack = new FtpSegment(ackPacket);
                int ackNum = ack.getSeqNum();
                System.out.println("ack " + ackNum);
    
                if ((currentSequenceNumber + 1) == ackNum) {
                    currentSequenceNumber += 1;
                    /*
                        will not cancel a timer task currently executing (ie. in certain cases, the
                        timer will still retransmit)
                     */ 
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
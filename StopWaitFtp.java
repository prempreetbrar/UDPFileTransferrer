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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.*;

public class StopWaitFtp {
	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger	
    private static final int SUCCESSFUL_TERMINATION = 0;
    private static final int UNSUCCESSFUL_TERMINATION = -1;

    private Socket TCPSocket;
    private DatagramSocket UDPSocket;
    private DataInputStream TCPInputStream;
    private DataOutputStream TCPOutputStream;

    private int timeout;
    private String fileName;

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

        this.timeout = timeout;
        this.fileName = null;
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
            wasSuccessful = true;
        } /*
         * We catch UnknownHostException first because it is a subclass of IOException. Of course,
         * the behaviour in both catch blocks is identical (printStackTrace), but if we wanted different
         * behaviour in the future, having these different blocks is good practice.
         */
        catch (UnknownHostException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            if (!wasSuccessful) {
                closeGracefully(TCPOutputStream, TCPInputStream, UDPSocket, TCPSocket);
                System.exit(UNSUCCESSFUL_TERMINATION);
            }
        }
    }

    private void completeTCPHandshake() {
        
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
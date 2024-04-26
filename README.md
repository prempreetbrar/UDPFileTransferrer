# UDP File Transferrer

A program that implements a simplified FTP client based on UDP; since UDP does not provide reliability, the
program implements its own reliability mechanism based on the stop-and-wait protocol. See a GIF of me using it below!

&nbsp;

## Features
- Supports _sending_ a file to the server over UDP; before transmission, the client and server go through an initial handshake process
  to exchange control information about the file transfer. The handshake takes place over TCP, while the actual file transfer is carried out
  over UDP. See the following figure:
<br></br>
![image](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/17ec3a65-ce55-4e29-b9b0-f2263ff72dfd)
&nbsp;
- The TCP `Socket` is used to exchange information about the file name to be sent, its length, the initial sequence number, as well as the UDP port
  number from the server.
- The provided file is read chunk-by-chunk; each chunk becomes the payload of an `FtpSegment` for transmission to the server; the `FtpSegment` itself is
  encapsulated in a UDP `DatagramPacket` and then written to the UDP socket.
- The sequence number for segments starts at the _initial sequence number_ received from the server during the handshake process and is incremented per every
  _segment_ transmitted (not byte, unlike TCP). The "ACK" packets received from the server as `DatagramPacket`'s carry the sequence number of the _next expected
  segment_ at the server. The ACK `DatagramPacket`'s are de-encapsulated to `FtpSegments` in order to obtain the acknowledgment number.
- As soon as a segment is transmitted, the client starts a retransmission timer (as is typical of a stop-and-wait protocol). If the correct ACK arrives, the timer is
  stopped. If the timer expires, the segment is retransmitted and the timer is restarted. 
- The server terminates a file transfer session if it does not receive any UDP packets from the client during a certain amount of time; to prevent the client program
  from waiting indefinitely for an ACK, the client aborts the file transfer if it does not receive an ACK within a _connection timeout_ period. 

## Usage/Limitations
- The root directory of the web server, where the objects are located, is specified as a command line input parameter. If the object path in
  the `GET` request is `/object-path`, then the file containing the object is located on the absolute path `server-root/object-path` in the file
  system, where `server-root` is the root directory of the web server.
- `-p <port_number>` specifies the server's port; default is `2025`
- `-t <idle_connection_timeout>` specifies the time after which the server closes the TCP connection in **milli-seconds**; default is `0` (which means infinity,
   ie. idle connections are not closed)
- `-r <server-root>` is the root directory of the web server (where all its HTTP objects are located); default is the current directory (directory in which program
   is ran)
- `quit` is typed in the system terminal to shut the server down. 
- Sends responses with HTTP version `HTTP/1.1`; only returns responses with the following status codes/phrases:
  ```
  200 OK
  400 Bad Request
  404 Not Found
  408 Request Timeout
  ```
- Assumes all header lines in the HTTP request are formatted correctly; only an error in the HTTP request line can trigger a `400 Bad Request` error. A
  properly formatted request line consists of three _mandatory_ parts which are separated by one or more spaces, as follows: `GET /object-path HTTP/1.1`.
  The command `GET` and protocol `HTTP/1.1` are fixed, while the `object-path` is optional. If no `object-path` is provided, _ie._ the request only specifies "/",
  then `index.html` is assumed by default. 

## If you want to start up the project on your local machine:
1. Download the code as a ZIP:
<br></br>
![download](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/291dc4a0-fe63-40b8-a70a-8bd3f987d5b6)
&nbsp;

2. Unzip the code:
<br></br>
![unzip](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/e2283434-6b61-41a1-b9b9-bb6380900798)
&nbsp;

3. Open the folder in an IDE, such as VSCode:
<br></br>
![open](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/aa1e0040-15af-4697-b9ab-52104b28e5b4)
&nbsp;

4. Start the server by compiling all files and then running `ServerDriver.java`, as follows:
   ```
   javac *.java
   java ServerDriver -p <port_number> -t <idle_connection_timeout> -r <server_root>
   ```
<br></br>
![server](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/51398c4c-fa7b-4867-b6b9-0b3d40d2bf55)
&nbsp;

5. Send a request to the server using `telnet`, a web browser, or any other application layer protocol:
<br></br>
![request](https://github.com/prempreetbrar/TCPWebServer/assets/89614923/44472d33-d81a-4b1a-a282-0cf861a3d654)



# UDP File Transferrer

A program that implements a simplified FTP client based on UDP; since UDP does not provide reliability, the
program implements its own reliability mechanism based on the stop-and-wait protocol. See a GIF of me using it below!

![client](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/4b1e1710-01d0-44fd-a2a3-2cc7f1d48fde)
&nbsp;

## Features
- Supports _sending_ a file to the server over UDP; before transmission, the client and server go through an initial handshake process
  to exchange control information about the file transfer. The handshake takes place over TCP, while the actual file transfer is carried out
  over UDP. See the following figure:
<br></br>
![image](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/8a966698-f6b0-4b0f-9171-c7733a819621)
&nbsp;
- The TCP `Socket` is used to exchange information about the file name to be sent, its length, the initial sequence number, as well as the UDP port
  number from the server.
- The provided file is read chunk-by-chunk; each chunk becomes the payload of an `FtpSegment` for transmission to the server; the `FtpSegment` itself is
  encapsulated in a UDP `DatagramPacket` and then written to the UDP socket.
- The sequence number for segments starts at the _initial sequence number_ received from the server during the handshake process and is incremented per every
  _segment_ transmitted (not byte, unlike TCP). The "ACK" packets received from the server as `DatagramPacket`'s carry the sequence number of the _next expected
  segment_ at the server. The ACK `DatagramPacket`'s are de-encapsulated to `FtpSegment`'s in order to obtain the acknowledgment number.
- As soon as a segment is transmitted, the client starts a retransmission timer (as is typical of a stop-and-wait protocol). If the correct ACK arrives, the timer is
  stopped. If the timer expires, the segment is retransmitted and the timer is restarted. 
- The server terminates a file transfer session if it does not receive any UDP packets from the client during a certain amount of time; to prevent the client program
  from waiting indefinitely for an ACK, the client aborts the file transfer if it does not receive an ACK within a _connection timeout_ period. 

## Usage/Limitations

### When Running the Client:

- `-i <file_name>` specifies the name of the file to be sent to the server; **REQUIRED**
- `-s <server_name>` specifies the hostname of the server (ex. cslinux.ucalgary.ca); defaults to `localhost`
- `-p <port_number>` specifies the port number of the server; note that this is the port number at which the server is running. This is DIFFERENT from the port number
  used for the server's UDP socket. See the image below:

<br></br>
![image](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/ff566ec4-baa4-4919-983b-c3481a4d4061)
<br></br>
In the above image, you see a server process `P1` open a `DatagramSocket` with port number `6428`. Similarly, our server will open a `DatagramSocket` **at its own
chosen port number. The server's UDP Socket port number is DIFFERENT from the server's port number itself.**
&nbsp;

- `-t <timeout_interval>` specifies the time in milliseconds after which a segment is retransmitted if an ACK is not received; defaults to 1000 milliseconds (1 second). 

### When Running the Server:

- `-p <port_number>` specifies the port number of the server; **the `-p` flags should match when running both the client and server. Otherwise the client will be unable
  to connect.** Defaults to `2025`.
- `-i <initial_seq_num>` specifies the initial sequence number to be used by the server. This is communicated to the client during the TCP handshake process.
  Defaults to `1`.
- `-d <average_delay>` specifies the average amount of time the server will wait before sending each ACK; specified in milli-seconds. Default is `1`.
- `-l <segment_loss_probability>` specifies the probability that the server will drop an arriving segment (to simulate packet loss which is typical in UDP-based transport). Default is `0.010` (1%).
- `-x <seq_number_to_be_dropped>` specifies the sequence number of the segment to be dropped by the server; this is useful when debugging as you can force the server
  to always drop a specific segment. Defaults to `-1` (no segment is forcefully dropped).
- `-t <idle_time>` specifies the time in milliseconds after which the server will shutdown if it does not receive a `DatagramPacket` from the client. Default is `5000`.
- `-r <seed>` specifies the seed for the random number generator; use the same seed every time to produce the same drop and delay results. Defaults to `13579`. 

## If you want to start up the project on your local machine:
1. Download the code as a ZIP:
<br></br>
![download](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/a801f9fd-6845-4cf5-925d-184af22a2357)
&nbsp;

2. Unzip the code:
<br></br>
![unzip](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/4bda1763-0643-4a89-ae95-27ed07695752)
&nbsp;

3. Open the folder in an IDE, such as VSCode:
<br></br>
![open](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/f93da787-8f29-41fd-b63a-d037249fe76d)
&nbsp;

4. Start the server by running:
   ```
   java -jar server/ftpserver.jar
   [-p <port_number>]
   [-i <initial_seq_num>]
   [-d <average_delay>]
   [-l <segment_loss_probability>]
   [-x <seq_number_to_be_dropped>]
   [-t <idle_time>]
   [-r <seed>]
   ```
<br></br>
![server](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/ee58b56e-0264-4ea6-a54c-cf956ce859b0)
&nbsp;

5. Start the client by opening up a new terminal and running:

```
cd client
javac *.java
java StopWaitDriver -i client_files/<name_of_file_to_transfer>
     [-s <server_name>]
     [-p <port_number>]
     [-t <timeout_interval>]
```

<br></br>
![client](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/571528aa-598d-4ff0-953f-9f891efa90d2)
&nbsp;

6. The transmitted file will be saved in the parent directory of the server; feel free to interact with it:
<br></br>
![file](https://github.com/prempreetbrar/UDPFileTransferrer/assets/89614923/ed683b1f-e750-4a2c-ac41-fcd5a954d575)

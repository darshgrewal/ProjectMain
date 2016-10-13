import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.io.*;
 
public class Client {
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket sendReceiveSocket;
     
    private final int INT_HOST_PORT = 2223;
    private final int SERVER_PORT = 2269;
    private boolean isNormal;
 
    private int destinationPort;
    private boolean verbose;
 
    public Client()
    {
        try {
            // Construct a datagram socket and bind it to any available 
            // port on the local host machine. This socket will be used to
            // send and receive UDP Datagram packets.
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {   // Can't create the socket.
            se.printStackTrace();
            System.exit(1);
        }
    }
 
    // Create RRQ(secondByte=1)/WRQ(secondByte=2)
    private byte[] createReadWriteReq(int secondByte, String file) {
 
        byte arr1[] = new byte[100];          
        int arr1ValidLength;
 
        arr1[0] = 0;
        arr1[1] = (byte) secondByte;
        arr1ValidLength = 2;
 
        byte arr2[] = file.getBytes();
        System.arraycopy(arr2, 0, arr1, arr1ValidLength, arr2.length);
        arr1ValidLength += arr2.length;
 
        arr1[arr1ValidLength] = 0;
        arr1ValidLength++;
 
        String  mode = "octet";
        byte arr3[] = mode.getBytes();
        System.arraycopy(arr3, 0, arr1, arr1ValidLength, arr3.length);        
        arr1ValidLength += arr3.length;
 
        arr1[arr1ValidLength] = 0;
        arr1ValidLength++;
 
        byte output[] = new byte[arr1ValidLength];
        System.arraycopy(arr1, 0, output, 0, arr1ValidLength);        
 
        return output;                     
    }
 
    private String findPacketType(DatagramPacket packet) {
        switch (packet.getData()[1]) {
            case 1:
                return "RRQ";
            case 2:
                return "WRQ";
            case 3:
                return "DATA";
            case 4:
                return "ACK";
            case 5:
                return "ERR";
            default:
                return "Unknown";
        }
    }
 
    private void printOutgoingPacketInfo(DatagramPacket packet) {
 
        String destination = isNormal ? "Server" : "Intermediate Host";
        String packetType = findPacketType(packet);
 
        if(verbose) {
            System.out.println("Packet Information from client to " + destination);
            //IP Address of client
            System.out.println("IP Address of client: " + packet.getAddress());
     
            //Client Port
            System.out.println("Port: " + packet.getPort());
            //Packet type
            System.out.println("Packet type: " + packetType);
 
            int len = packet.getLength();
            System.out.println("Length: " + len);
            System.out.print("Containing: ");
            System.out.println(new String(packet.getData(),0,len));
            System.out.println("as string and ");
            System.out.println(Arrays.toString(packet.getData()));
            System.out.println("as byte.");
        }
    }
 
    private void printIncomingPacketInfo(DatagramPacket packet) {
 
        String source = isNormal ? "Server" : "Intermediate Host";
        String packetType = findPacketType(packet);
 
        if(verbose) {
            System.out.println("Packet Information from " + source);
            //IP Address of source, either server or intermediate host, based on Normal or Test selection
            System.out.println("IP Address of " + source + ": " + packet.getAddress());
 
            //Port of source, either server or intermediate host, based on Normal or Test selection
            System.out.println("Port: " + packet.getPort());
            //Packet type
            System.out.println("Packet type: " + packetType);
 
            int len = packet.getLength();
            System.out.println("Length: " + len);
 
            System.out.print("Containing: ");
            System.out.println(new String(packet.getData(),0,len));
            System.out.println("as string and ");
            System.out.println(Arrays.toString(packet.getData()));
            System.out.println("as byte.");
        }
    }
 
    //Create an ack to send to server
    private byte[] createAck(byte[] data) {
 
        byte arr[] = new byte[4];         
        arr[0] = 0;
        arr[1] = 4;
        arr[2] = data[2];
        arr[3] = data[3];
 
        return arr;
    }
 
    //Prepare and send a packet
    private void prepareAndSend(byte[] input) {
        prepareAndSend(input, input.length);
    }
 
    private void prepareAndSend(byte[] input, int length) {
        try {
            sendPacket = new DatagramPacket(input, length,
                    InetAddress.getLocalHost(), destinationPort);
            sendReceiveSocket.send(sendPacket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        printOutgoingPacketInfo(sendPacket);
    }
 
    ////////////////////////////////////////////////////////////////
    //Send RRQ to server, receive read data packets from server until 
    //the end of file, write the received data into a file, send ack
    //to server
    private void handleReadReq(String serverReadFile, String clientWriteFile) {
 
        //Instantiate BufferedOutputStream to write the read data received from server
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(clientWriteFile));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
         
        //Prepare and send a RRQ to server
        System.out.println("Read Request Packet Information:");
        byte[] readReq = createReadWriteReq(1, serverReadFile);
        prepareAndSend(readReq);
         
        boolean moreToCome = true;
        while(moreToCome) {
 
            //Prepare a datagram packet to receive data from server
            byte[] data = new byte[517];
            receivePacket = new DatagramPacket(data, data.length);
            try {
                sendReceiveSocket.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
 
            printIncomingPacketInfo(receivePacket);
 
            //Effective length of received data in range of 0-512
            int dataLen = receivePacket.getLength() - 4;
 
            if(dataLen > 512) {
                System.out.println("Data packet received from server has a data length greater than 512.");
                System.exit(1);
            }
            if(data[0] != 0) {
                System.out.println("First byte of data packet received from server is not 0.");
                System.exit(1);
            }
            if(data[1] != 3) {
                System.out.println("Second byte of data packet received from server is not 3.");
                System.exit(1);
            }
 
            destinationPort = receivePacket.getPort();
 
            //Write received data to file
            try {
                out.write(data, 4, dataLen);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
 
            if(dataLen < 512) {
                moreToCome = false;
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
 
            //Prepare and send an ack packet to server
            System.out.println("Packet Information for ACK from client to server:");
            byte[] ack = createAck(data);
            prepareAndSend(ack);
        }
    }
 
    /////////////////////////////////////////////////////////////////
    //Send WRQ to server, receive ack for WRQ from server, send write
    //data packets to server until the file ends, receive ack from server
    //for each write data packet sent
    private void handleWriteReq(String clientReadFile, String serverWriteFile) {
 
        //Instantiate BufferedInputStream to read the data into.
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(clientReadFile));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
         
        //Prepare and send a WRQ to server
        System.out.println("Write Request Packet Information:");
        byte[] writeReq = createReadWriteReq(2, serverWriteFile);
        prepareAndSend(writeReq);
 
        //Prepare a datagram packet to receive ack from server
        byte[] ack = new byte[5];
        receivePacket = new DatagramPacket(ack, ack.length);
        try {
            sendReceiveSocket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        printIncomingPacketInfo(receivePacket);
 
        if(receivePacket.getLength() != 4) {
            System.out.println("Ack packet from server is not 4 bytes.");
            System.exit(1);
        }
        if (ack[0] != 0) {
            System.out.println("First byte of ack packet from server is not 0.");
            System.exit(1);
        }
        if (ack[1] != 4) {
            System.out.println("Second byte of ack packet from server is not 4.");
            System.exit(1);
        }
 
        destinationPort = receivePacket.getPort();
 
        boolean moreToWrite = true;
        while(moreToWrite) {
            //Create an integer representation of block# from the two least significant bytes received in the ack
            int blockNo = (ack[2] * 256) + ack[3];
     
            //Prepare data for write
            byte arr[] = new byte[516];       
            arr[0] = 0;
            arr[1] = 3;
            blockNo++;
            arr[3] = (byte) (blockNo % 256);
            arr[2] = (byte) (blockNo >> 8);
            int readByteLen = -1;
            try {
                readByteLen = in.read(arr, 4, 512);
            } catch (IOException e1) {
                e1.printStackTrace();
                System.exit(1);
            }
            if( (readByteLen == -1) || (readByteLen < 512) ) {
                moreToWrite = false;
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
             
            //Prepare and send write data packet to server. If this is the end
            //of file, a zero-byte packet will be sent. Receiving this packet  
            //(with zero-byte and nonzero-byte) is handled in server.
            System.out.println("Write Data Packet Information:");
            if (readByteLen == -1)
                prepareAndSend(arr, 4);
            else
                prepareAndSend(arr, readByteLen + 4);
 
            //Prepare a datagram packet to receive ack from server
            receivePacket = new DatagramPacket(ack, ack.length);
            try {
                sendReceiveSocket.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            printIncomingPacketInfo(receivePacket);
 
            if(receivePacket.getLength() != 4) {
                System.out.println("Ack packet from server is not 4 bytes.");
                System.exit(1);
            }
            if (ack[0] != 0) {
                System.out.println("First byte of ack packet from server is not 0.");
                System.exit(1);
            }
            if (ack[1] != 4) {
                System.out.println("Second byte of ack packet from server is not 4.");
                System.exit(1);
            }
        }
    }
 
    //Implement read or write based on the command line inputs
    private void runReadWrite() {
 
        Scanner in = new Scanner(System.in);
        String req = null;
        String readFile = null;
        String writeFile = null;
        boolean isQuit = false;
        boolean flag;
 
        while(!isQuit) {
            // Read, Write or Quit
            flag = true;
            while (flag) {
                System.out.println("(R)ead, (W)rite, or (Q)uit?");
                req = in.nextLine();
                if (!(req.equalsIgnoreCase("R") || req.equalsIgnoreCase("W") || req.equalsIgnoreCase("Q"))) {
                    System.out.println("invalid input! Just type R, W, or Q.");
                } else {
                    flag = false;
                    System.out.println("Your command is: " + req);
 
                    if (req.equalsIgnoreCase("Q"))
                        isQuit = true;
                }
            }
 
            if(isQuit)
                break;
 
            flag = true;
            // Normal vs. test mode
            while (flag) {
                System.out.println("(N)ormal or (T)est?");
                String reqMode = in.nextLine();
                if (!(reqMode.equalsIgnoreCase("N") || reqMode.equalsIgnoreCase("T"))) {
                    System.out.println("invalid input! Just type N or T.");
                } else {
                    flag = false;
                    System.out.println("Your choice is: " + reqMode);
                    if (reqMode.equalsIgnoreCase("N")) {
                        destinationPort = SERVER_PORT;
                        isNormal = true;
                    } else {
                        destinationPort = INT_HOST_PORT;
                        isNormal = false;
                    }
                }
            }
 
            flag = true;
            // verbose vs quiet
            while (flag) {
                System.out.println("(V)erbose or (Q)uiet?");
                String reqVerbose = in.nextLine();
                if (!(reqVerbose.equalsIgnoreCase("V") || reqVerbose.equalsIgnoreCase("Q"))) {
                    System.out.println("invalid input! Just type V or Q.");
                } else {
                    flag = false;
                    System.out.println("Your choice is: " + reqVerbose);
                    if (reqVerbose.equalsIgnoreCase("V")) {
                        verbose = true;
                    } else {
                        verbose = false;
                    }
                }
            }
 
            flag = true;
            if (req.equalsIgnoreCase("W")) {
                while (flag) {
                    System.out.println("Choose the file (path and) name on client to read from; e.g. text1.txt or /tmp/client/test.txt.");
                    readFile = in.nextLine();
                    File f = new File(readFile);
                    if (f.exists() && !f.isDirectory()) {
                        flag = false;
                    } else {
                        System.out.println("Requested file does not exist.");
                    }
                }
                System.out.println("Choose the file name on server to write to. The file will be under ./server/ folder.");
                writeFile = in.nextLine();
                handleWriteReq(readFile, writeFile);
            } else { //if(req.equalsIgnoreCase("read")) {
                System.out.println("Choose the file name on server to read from. It should exist under ./server/ folder.");
                readFile = in.nextLine();
                System.out.println("Choose the file (path and) name on client to write to; e.g. text1.txt or /tmp/client/test.txt.");
                writeFile = in.nextLine();
                handleReadReq(readFile, writeFile);
            }
        }
        in.close();
    }
     
    public static void main(String args[])
    {
        Client c = new Client();
        c.runReadWrite();
    }
}
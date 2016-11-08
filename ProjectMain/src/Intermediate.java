import java.net.*;
import java.util.Scanner;

public class Intermediate implements Runnable {

    DatagramSocket receiveSocket, sendReceiveSocket;
    //TODO: intermediate was supposed to go along side the client. Since the client is single threaded what is the use for this variable here? This variable is never used for any condition checking!
    //DONE: i'm not the one that put it there so you may have to talk to whoever did
    int threads = 0;//number of current threads
    String type = null;

    public Intermediate() {
        try {
            receiveSocket = new DatagramSocket(23);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        int chosenPacket = 0;
        String typeChosen = "";
        String side = "";
        int delay = 0;
        int choice = 0;
        Boolean verbose;

        //TODO: maybe we need different packet numbers based on ack or data type
        //DONE: shouldn't it be the same because there will be for example an ack #3 packet in response to data #3 packet?
        int packetNo = 0;

        Scanner scan = new Scanner(System.in);
        
        System.out.print("Enter (0) for quiet mode or (1) for verbose:");
        if (scan.nextInt() == 0) {
        	verbose = false;
        } else {
        	verbose = true;
        }
        
        
        while (choice < 1 || choice > 4) {
            System.out.print("Enter (1) for normal operation, enter (2) to lose a packet, enter (3) delay a packet, or enter (4) to duplicate a packet:");
            choice = scan.nextInt();
        }

        if (choice == 1) {
            //do nothing
        } else if (choice == 2) {
	        System.out.print("Choose an integer number for which packet to lose:");
	        chosenPacket = scan.nextInt();
	        System.out.print("Choose type of packet to lose(request,ack, or data):");
	        typeChosen = scan.next();
	        System.out.print("Choose to implement error for server (type 0) or client (type 1):");
	        side = scan.next();
        } else if (choice == 3) {
            System.out.print("Choose an integer number for which packet to delay:");
            chosenPacket = scan.nextInt();
            System.out.print("Choose an integer number for how many milliseconds to delay:");
            delay = scan.nextInt();
            System.out.print("Choose type of packet to delay(request,ack, or data):");
            typeChosen = scan.next();
            System.out.print("Choose to implement error for server (type 1) or client (type 2):");
            side = scan.next();
        } else { // if (choice == 4)
            System.out.print("Choose an integer number for which packet to duplicate:");
            chosenPacket = scan.nextInt();
            //System.out.print("Choose an integer number for how many milliseconds between duplicates:");
            //delay = scan.nextInt();
            System.out.print("Choose type of packet to duplicate(request,ack, or data):");
            typeChosen = scan.next();
            System.out.print("Choose to implement error for server or client:");
            side = scan.next();
        }
        scan.close();

        while (true) {
        	
            try {
            	
                addThread();
                //create packet for recieving
                byte data[] = new byte[100];
                DatagramPacket forwardingPacket = new DatagramPacket(data, data.length);

                // Receive packet from client
                receiveSocket.receive(forwardingPacket);
                InetAddress clientAddress = forwardingPacket.getAddress();
                int clientPort = forwardingPacket.getPort();

                //Process the received datagram.
                if (verbose) {
                	printInfo(forwardingPacket);
                }
                if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
                    type = "request";
                } else if (forwardingPacket.getData()[1] == 3) {
                    packetNo++;
                    type = "ack";
                } else if (forwardingPacket.getData()[1] == 4) {
                    packetNo++;
                    type = "data";
                } else if (forwardingPacket.getData()[1] == 5) {
                    packetNo++;
                    type = "error";
                }
                
                packetNo = ((forwardingPacket.getData()[1] & (byte)0xff) << 8) | (forwardingPacket.getData()[1] & (byte)0xff);
                
              
                /* insert statement to check for client sent ack or client sent data or request and chosen packet*/
                //TODO: looks like: if user chooses side 2 (server) this if condition is not satisfied and the first packet (from client to server) is never delivered. Make sure if the packets don't match user specified criteria, the packet forwarding is working fine!
                if (!(choice == 2 && type == typeChosen && packetNo == chosenPacket && side.equals("client"))) {

                    if (choice == 3 && type == typeChosen && packetNo == chosenPacket && side.equals("client")) {
                        wait(delay);
                    }
					  
					// Forward packet to server
					forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), InetAddress.getLocalHost(), 69);
	
					//print out packet sent
					if (verbose) {
						System.out.println("Intermediate: Packet sending:");
						System.out.println("String: " + new String(forwardingPacket.getData(), 0, forwardingPacket.getLength()));
						System.out.println("Bytes: " + forwardingPacket.getData());
					}
	
					sendReceiveSocket = new DatagramSocket();
					sendReceiveSocket.send(forwardingPacket);
					
					//if chose to duplicate this particular packet, send again
					if (choice == 4 && type == typeChosen && packetNo == chosenPacket && side.equals("client")) {
						sendReceiveSocket.send(forwardingPacket);
				    }
	
					// Receive response from server
					data = new byte[100];
					forwardingPacket = new DatagramPacket(data, data.length);
	
					sendReceiveSocket.receive(forwardingPacket);
	
					//print information
					if (verbose) {
						printInfo(forwardingPacket);
					}
					
					//check for type again
					if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
	                    type = "request";
	                } else if (forwardingPacket.getData()[1] == 3) {
	                    packetNo++;
	                    type = "ack";
	                } else if (forwardingPacket.getData()[1] == 4) {
	                    packetNo++;
	                    type = "data";
	                } else if (forwardingPacket.getData()[1] == 5) {
	                    packetNo++;
	                    type = "error";
	                }
	
					/* insert statement to check for server sent ack or server sent data and chosen packet*/
					if (!(choice == 2 && type == typeChosen && packetNo == chosenPacket && side.equals("server"))) {
	
					    if (choice == 3 && type == typeChosen && packetNo == chosenPacket && side.equals("server") && packetNo == chosenPacket) {
					    	wait(delay);
					    }
					    
					    // Forward response to client
					    forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), clientAddress, clientPort);
					    sendReceiveSocket.send(forwardingPacket);
					    
					    //if choice is to duplicate this particular packet, send again
					    if (choice == 4 && type == typeChosen && packetNo == chosenPacket && side.equals("server")) {
					    	sendReceiveSocket.send(forwardingPacket);
					    }
					    
					    
					    //print out sent datagram
					    if (verbose) {
						    System.out.println("Intermediate: Packet sending:");
						    System.out.println("String: " + new String(forwardingPacket.getData(), 0, forwardingPacket.getLength()));
						    System.out.println("Bytes: " + forwardingPacket.getData());
					    }
					}

                }
                removeThread();
                stop(sendReceiveSocket);

            } catch(Exception e){
                e.printStackTrace();
            }
        }
        
    }

    public void printInfo(DatagramPacket x) {

        // Process the received datagram.
        System.out.println("Intermediate:");
        System.out.println("Host: " + x.getAddress());
        System.out.println("Host port: " + x.getPort());
        int len = x.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");

        // Form a String from the byte array.
        System.out.println("String: " + new String(x.getData(), 0, len));
        System.out.println("Bytes: " + x.getData());

    }

    
    public void addThread() {
        threads++;
    }

    public void removeThread() {
        threads--;
    }

    public void stop(DatagramSocket x) {
        x.close();
        removeThread();
    }

    public static void main(String[] args) {
        (new Thread(new Intermediate())).start();
    }

}

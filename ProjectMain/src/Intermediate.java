import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class Intermediate implements Runnable {

    DatagramSocket receiveSocket, sendReceiveSocket;int threads = 0;//number of current threads

    public Intermediate() {
        try {
            receiveSocket = new DatagramSocket(2268);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String typeChosen = null;
        String side = null;
        String type = null;
        int delay = 0;
        int choice = 0;
        int clientPort;
        int serverPort = 2269;
        int packetNo = 0;
        int chosenPacket = 0;
		int servLength = 0;
        Boolean verbose = false;

        Scanner scan = new Scanner(System.in);
        
        System.out.print("Enter (0) for quiet mode or (otherwise) for verbose:");
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
	        System.out.print("Choose side to implement(client or server)");
	        side = scan.next();
        } else if (choice == 3) {
            System.out.print("Choose an integer number for which packet to delay:");
            chosenPacket = scan.nextInt();
            System.out.print("Choose an integer number for how many milliseconds to delay:");
            delay = scan.nextInt();
            System.out.print("Choose type of packet to delay(request,ack, or data):");
            typeChosen = scan.next();
	        System.out.print("Choose side to implement(client or server)");
            side = scan.next();
        } else { // if (choice == 4)
            System.out.print("Choose an integer number for which packet to duplicate:");
            chosenPacket = scan.nextInt();
            //System.out.print("Choose an integer number for how many milliseconds between duplicates:");
            //delay = scan.nextInt();
            System.out.print("Choose type of packet to duplicate(request,ack, or data):");
            typeChosen = scan.next();
	        System.out.print("Choose side to implement(client or server)");
            side = scan.next();
        }
        scan.close();

        while (true) {
        	
            try {
            	
                addThread();
                //create packet for recieving
                byte data[] = new byte[516];
                DatagramPacket forwardingPacket = new DatagramPacket(data, data.length);

                // Receive packet from client
                receiveSocket.receive(forwardingPacket);
                InetAddress clientAddress = forwardingPacket.getAddress();
                clientPort = forwardingPacket.getPort();

                //Process the received datagram.
                if (verbose) {
                	printInfo(forwardingPacket);
                }
                if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
                    type = "request";
                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
                } else if (forwardingPacket.getData()[1] == 3) {
                    type = "data";
                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
                } else if (forwardingPacket.getData()[1] == 4) {
                    type = "ack";
                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
                } else if (forwardingPacket.getData()[1] == 5) {
                    type = "error";
                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
                }
                
                //make packetNo unsigned? not sure how
                //packetNo = ((forwardingPacket.getData()[1] & (byte)0xff) << 8) | (forwardingPacket.getData()[1] & (byte)0xff)
                
              
                /* insert statement to check for client sent ack or client sent data or request and chosen packet*/
                if (!(choice == 2 && type.equals(typeChosen)  && packetNo == chosenPacket && side.equals("client"))) {

                    if (choice == 3 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("client")) {
                        Thread.sleep(delay);
                    }					
                    
					// Forward packet to server
					forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), InetAddress.getLocalHost(), serverPort);
	
					//print out packet sent
					if (verbose) {
						printInfo(forwardingPacket);
					}
					
					//create sending socket and send packet
					sendReceiveSocket = new DatagramSocket();
					sendReceiveSocket.send(forwardingPacket);
					
					//if chose to duplicate this particular packet, send again
					if (choice == 4 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("client")) {
						System.out.println("\nSending out duplicate packet\n");
						sendReceiveSocket.send(forwardingPacket);
				    }
	
					// Receive response from server
					data = new byte[516];
					forwardingPacket = new DatagramPacket(data, data.length);	
					sendReceiveSocket.receive(forwardingPacket);
					serverPort = forwardingPacket.getPort();
	
					//print information
					if (verbose) {
						printInfo(forwardingPacket);
					}
					
					//check for type again
					if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
	                    type = "request";
	                    servLength = 516;
	                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
	                } else if (forwardingPacket.getData()[1] == 3) {
	                    type = "data";
	                    servLength = 516;	                    
	                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
	                } else if (forwardingPacket.getData()[1] == 4) {
	                    type = "ack";
	                    servLength = 4;
	                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
	                } else if (forwardingPacket.getData()[1] == 5) {
	                    type = "error";
	                    servLength = 4;
	                    packetNo = forwardingPacket.getData()[3]+forwardingPacket.getData()[4];
	                }
	
					/* insert statement to check for server sent ack or server sent data and chosen packet*/
					if (!(choice == 2 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server"))) {
	
					    if (choice == 3 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server")) {
	                        Thread.sleep(delay);
					    }
					    
					    // Forward response to client
					    DatagramPacket forwarding2Packet = new DatagramPacket(forwardingPacket.getData(), servLength, clientAddress, clientPort);
					    receiveSocket.send(forwarding2Packet);
					    
					    //if choice is to duplicate this particular packet, send again
					    if (choice == 4 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server")) {
							System.out.println("\nSending out duplicate packet\n");
					    	sendReceiveSocket.send(forwarding2Packet);
					    }
					    
					    
					    //print out sent datagram
					    if (verbose) {
							printInfo(forwarding2Packet);
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
        System.out.println("\nHost: " + x.getAddress() + " port: " + x.getPort());
        System.out.println("Length: " + x.getLength());
        // Form a String from the byte array.
        int block = x.getData()[3];
        int packType = x.getData()[1];
        if (packType == 1) {
        	System.out.println("Type: Read Request Packet");
        } else if (packType == 2) {
        	System.out.println("Type: Write Request Packet");
        } else if (packType == 3) {
        	System.out.println("Type: Data Packet");
        } else if (packType == 4) {
        	System.out.println("Type: ACK Packet");
        } else if (packType == 5) {
        	System.out.println("Type: Error Packet");
        }
        System.out.println("Block Number: " + block);
        System.out.println("Containing " + new String(x.getData(),0,(x.getData()).length));
        System.out.println("Information in byte form: " + Arrays.toString(x.getData()));

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

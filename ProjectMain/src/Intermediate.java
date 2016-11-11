import java.net.*;
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
                	Utils.printInfo(forwardingPacket);
                }

                packetNo = Utils.getBlockNo(forwardingPacket);

                if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
                    type = "request";
                } else if (forwardingPacket.getData()[1] == 3) {
                    type = "data";
                } else if (forwardingPacket.getData()[1] == 4) {
                    type = "ack";
                } else if (forwardingPacket.getData()[1] == 5) {
                    type = "error";
                }
                
                /* insert statement to check for client sent ack or client sent data or request and chosen packet*/
                if (!(choice == 2 && type.equals(typeChosen)  && packetNo == chosenPacket && side.equals("client"))) {

                    if (choice == 3 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("client")) {
                        Thread.sleep(delay);
                    }					
                    
					// Forward packet to server
					forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), InetAddress.getLocalHost(), serverPort);
	
					//print out packet sent
					if (verbose) {
						Utils.printInfo(forwardingPacket);
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
						Utils.printInfo(forwardingPacket);
					}

                    packetNo = Utils.getBlockNo(forwardingPacket);

					//check for type again
					if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
	                    type = "request";
	                    servLength = 516;
	                } else if (forwardingPacket.getData()[1] == 3) {
	                    type = "data";
	                    servLength = 516;	                    
	                } else if (forwardingPacket.getData()[1] == 4) {
	                    type = "ack";
	                    servLength = 4;
	                } else if (forwardingPacket.getData()[1] == 5) {
	                    type = "error";
	                    servLength = 4;
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
							Utils.printInfo(forwarding2Packet);
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

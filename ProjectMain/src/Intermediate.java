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
        boolean verbose = false;

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
        } else if (choice == 2) { // loose packet
	        System.out.print("Choose an integer number for which packet to lose:");
	        chosenPacket = scan.nextInt();
	        System.out.print("Choose type of packet to lose(request,ack, or data):");
	        typeChosen = scan.next();
	        System.out.print("Choose side to implement(client or server)");
	        side = scan.next();
        } else if (choice == 3) { // delay packet
		System.out.print("Choose an integer number for which packet to delay:");
            	chosenPacket = scan.nextInt();
            	System.out.print("Choose an integer number for how many milliseconds to delay (greater than 3000):");
            	delay = scan.nextInt();
            	System.out.print("Choose type of packet to delay(request,ack, or data):");
            	typeChosen = scan.next();
	        System.out.print("Choose side to implement(client or server)");
            	side = scan.next();
        } else { // if (choice == 4) // duplicate packet
            	System.out.print("Choose an integer number for which packet to duplicate:");
            	chosenPacket = scan.nextInt();
            	System.out.print("Choose type of packet to duplicate(request,ack, or data):");
            	typeChosen = scan.next();
	        System.out.print("Choose side to implement(client or server)");
            	side = scan.next();
        }
        scan.close();

        while (true) {
		
            try {
                //create packet for recieving
                byte data[] = new byte[516];
                DatagramPacket forwardingPacket = new DatagramPacket(data, data.length);

                // Receive packet from client
                receiveSocket.receive(forwardingPacket);
                InetAddress clientAddress = forwardingPacket.getAddress();
                clientPort = forwardingPacket.getPort();

                //Process the received datagram.
                if (verbose) {
                	Utils.printInfo(forwardingPacket, Utils.RECEIVE);
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
		                    		choice = 1;
		                        	Thread.sleep(delay);
		                    	}					
		                    
					// Forward packet to server
					forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), InetAddress.getLocalHost(), serverPort);
		
					//print out packet sent
					if (verbose) {
						Utils.printInfo(forwardingPacket, Utils.SEND);
					}
		
					//create sending socket and send packet
					sendReceiveSocket = new DatagramSocket();
					sendReceiveSocket.send(forwardingPacket);
		
					//if chose to duplicate this particular packet, send again
					if (choice == 4 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("client")) {
						System.out.println("\nSending out duplicate packet\n");
						sendReceiveSocket.send(forwardingPacket);
						choice = 1;
				    	}
					
					while (true) {
						// Receive response from server
						data = new byte[516];
						forwardingPacket = new DatagramPacket(data, data.length);	
						sendReceiveSocket.receive(forwardingPacket);
						serverPort = forwardingPacket.getPort();
		
						//print information
						if (verbose) {
							Utils.printInfo(forwardingPacket, Utils.RECEIVE);
						}
			
			                    	packetNo = Utils.getBlockNo(forwardingPacket);
			                    	servLength = forwardingPacket.getLength();
		
						//check for type again
						if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
				                    type = "request";
				                } else if (forwardingPacket.getData()[1] == 3) {
				                    type = "data";
				                } else if (forwardingPacket.getData()[1] == 4) {
				                    type = "ack";
				                } else if (forwardingPacket.getData()[1] == 5) {
				                    type = "error";
				                }					
								
						//uncomment for testing
						//System.out.println(choice);
						//System.out.println(typeChosen);
						//System.out.println(packetNo);
						//System.out.println(chosenPacket);
						//System.out.println(side);
		
		
						/* insert statement to check for server sent ack or server sent data and chosen packet*/
						if (!(choice == 2 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server"))) {
		
						    if (choice == 3 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server")) {
							    choice = 1;
							    Thread.sleep(delay);
						    }
		
						    // Forward response to client
						    DatagramPacket forwarding2Packet = new DatagramPacket(forwardingPacket.getData(), servLength, clientAddress, clientPort);
						    receiveSocket.send(forwarding2Packet);
		
						    //if choice is to duplicate this particular packet, send again
						    if (choice == 4 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server")) {
							    System.out.println("\nSending out duplicate packet\n");
							    sendReceiveSocket.send(forwarding2Packet);
							    choice = 1;
						    }
		
						    //print out sent datagram
						    if (verbose) {
							    Utils.printInfo(forwarding2Packet, Utils.SEND);
						    }
						    break;
							
						} else {
							System.out.println("LOST THE PACKET");
							choice = 1;
						}
					}

                } else {
                	if (typeChosen.equals("ack")) {
                		choice = 1;
                		while (true) {
    						// Receive response from server
    						data = new byte[516];
    						forwardingPacket = new DatagramPacket(data, data.length);	
    						sendReceiveSocket.receive(forwardingPacket);
    						serverPort = forwardingPacket.getPort();
    		
    						//print information
    						if (verbose) {
    							Utils.printInfo(forwardingPacket, Utils.RECEIVE);
    						}
    			
    			                    	packetNo = Utils.getBlockNo(forwardingPacket);
    			                    	servLength = forwardingPacket.getLength();
    		
    						//check for type again
    						if (forwardingPacket.getData()[1] == 1 || forwardingPacket.getData()[1] == 2) {
    				                    type = "request";
    				                } else if (forwardingPacket.getData()[1] == 3) {
    				                    type = "data";
    				                } else if (forwardingPacket.getData()[1] == 4) {
    				                    type = "ack";
    				                } else if (forwardingPacket.getData()[1] == 5) {
    				                    type = "error";
    				                }					
    								
    						//uncomment for testing
    						//System.out.println(choice);
    						//System.out.println(typeChosen);
    						//System.out.println(packetNo);
    						//System.out.println(chosenPacket);
    						//System.out.println(side);
    		
    		
    						/* insert statement to check for server sent ack or server sent data and chosen packet*/
    						if (!(choice == 2 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server"))) {
    		
    		
    						    // Forward response to client
    						    DatagramPacket forwarding2Packet = new DatagramPacket(forwardingPacket.getData(), servLength, clientAddress, clientPort);
    						    receiveSocket.send(forwarding2Packet);
    		
    						    //if choice is to duplicate this particular packet, send again
    						    if (choice == 4 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("server")) {
    							    System.out.println("\nSending out duplicate packet\n");
    							    sendReceiveSocket.send(forwarding2Packet);
    							    choice = 1;
    						    }
    		
    						    //print out sent datagram
    						    if (verbose) {
    							    Utils.printInfo(forwarding2Packet, Utils.SEND);
    						    }
    						    break;    							
    						}
    					}
                		
                	}
                }
                
                if ((choice == 2 && type.equals(typeChosen) && packetNo == chosenPacket && side.equals("client"))) {
                	choice = 1;
                }

            } catch(Exception e){
                e.printStackTrace();
            }
        }
        
    }

    public static void main(String[] args) {
        (new Thread(new Intermediate())).start();
    }
    
    public DatagramPacket opCodeError(DatagramPacket packet) {    	
    	byte[] newData = packet.getData();
    	newData[0] = 9;
    	newData[1] = 9;
		return new DatagramPacket(newData, newData.length, packet.getAddress(), packet.getPort());    	
    }
    
    public DatagramPacket blockNumberError(DatagramPacket packet, int blockNum) {
		packet.getData()[2] = (byte) ((blockNum >> 8) & 0xFF);
		packet.getData()[3] = (byte) (blockNum & 0xFF);
		return new DatagramPacket(packet.getData(), packet.getLength(),	packet.getAddress(), packet.getPort());
	}
    
    public DatagramPacket noEndZeroError(DatagramPacket packet) {    	
    	return new DatagramPacket(packet.getData(), packet.getLength()-1, packet.getAddress(), packet.getPort());   	
    }
    
    public DatagramPacket badEndZeroError(DatagramPacket packet) {    	
    	byte[] newData = packet.getData();
    	newData[newData.length-1] = 9;
		return new DatagramPacket(newData, newData.length, packet.getAddress(), packet.getPort());    	
    }
    
    public DatagramPacket modeError(DatagramPacket packet) {    	
    	byte[] newData = packet.getData();
		int i = 1;
		i = getZero(newData,1);
		byte[] newMode = ("xxx").getBytes();
		for (int j = 0; j < newMode.length; j++) {
			newData[i + j + 1] = newMode[j];
		}
		return new DatagramPacket(newData, newData.length, packet.getAddress(),	packet.getPort());	
    }
    
 	public DatagramPacket fileNameError(DatagramPacket packet) {
 		byte[] oldData = packet.getData();
 		byte[] newData = new byte[oldData.length];
 		int i = 1;
		i = getZero(oldData,1);
 		
 		newData[0] = oldData[0];
 		newData[1] = oldData[1];
 		
 		for (int k=0 ; k < i-2 ; k++) {
 			newData[k+2] = 0;
 		}
 		return new DatagramPacket(newData, newData.length, packet.getAddress(),	packet.getPort());
 	}
 	
 	public DatagramPacket sizeBiggerDataError(DatagramPacket packet) {
 		byte[] newData = packet.getData();
 		newData[518] = 9;
 		return new DatagramPacket(newData, newData.length, packet.getAddress(),	packet.getPort());
 	}
 	
 	public DatagramPacket sizeBiggerAckError(DatagramPacket packet) {
 		byte[] newData = new byte[] {packet.getData()[0], packet.getData()[1], packet.getData()[2], packet.getData()[3], packet.getData()[0], packet.getData()[1]};
 		return new DatagramPacket(newData, newData.length, packet.getAddress(),packet.getPort());
 	}
 	
 	public DatagramPacket sizeSmallerAckError(DatagramPacket packet) {
 		byte[] newData = new byte[] { packet.getData()[0], packet.getData()[1], (byte)0x00 };
 		return new DatagramPacket(newData, newData.length, packet.getAddress(),packet.getPort());
 	}   
    
    private int getZero(byte[] msg, int currPos)
    {
        for (int i = currPos; i < msg.length; i++){
            if (msg[i] == 0){
                return i;
            }
        }
        return 0;
    }    

}

import java.net.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.io.*;
 
public class TransferHub {
     
    //the size values shouldn't change so they were made final variables
    public final int SIZEB = 516;
    public final int SIZEDB = 512;

    public final int SERVER = 0;
    public final int CLIENT = 1;

    private enum AckType {ERROR, FRESH, DUPLICATE }

    //creates a receive request to send to the main server class, creating the socket and packet to hold the info
    public boolean clientRequest(DatagramSocket rSocket, DatagramPacket rPacket, String type)
    {
    	boolean normal = false;
    	while (!normal) {
	        try {
	        	rSocket.setSoTimeout(2000);
	            rSocket.receive(rPacket);
	            //rPacket.setData(Arrays.copyOfRange(rPacket.getData(), 0, rPacket.getLength()));
	            //client recieves the notification that packet has reached it from the server
	            System.out.println("Host: The packet has been received.");
	            Utils.printInfo(rPacket);
	            normal = true;
	        } catch (SocketTimeoutException e){
	        	if (type.equals("server")) {
	        	} else {
	        		System.out.println("Timeout cccured.");
	        		return false;
	        	}
		        	
	        } catch (IOException inoutE){
	            inoutE.printStackTrace();
	            //exits if it is found
	            System.exit(1);
	        }
    	}

    	return true;
    }
     
  //class handles the files that being sent over from the client
    //stores them in a file of the users choice
 
    public void getFile(DatagramSocket socket, String fName, int callerId){
         
        //holds the message that will be sent over
        byte[] fileInfo = new byte[SIZEB];
        //the packet in which the message will be sent in
        DatagramPacket dataPacket = new DatagramPacket(fileInfo, fileInfo.length);
         
        byte aT[] = new byte[]{0, 4};
        InOut newFile = new InOut(fName);
         
        while (true) {
        	if (!clientRequest(socket, dataPacket, "req")) {
				cAndSendError(socket, "Timeout Occured.", 0, callerId);
				break;
        	}
            // if received packet is an error no need to continue
            if (!checkD(dataPacket.getData())) {
                break;
            }
            byte blockbyte[] = Arrays.copyOfRange(dataPacket.getData(), 2, 4);
            byte dblock[] = Arrays.copyOfRange(dataPacket.getData(), 4, dataPacket.getLength());
             
            try {
                if(!newFile.write(dblock, socket, dataPacket.getPort(), callerId, blockbyte))
                    return;
            } catch (Exception e) {
                return;
            }
            
            byte aByte[] = byteArrayCreater(aT, blockbyte);
             
            sendBytes(socket, dataPacket.getPort(), aByte);
             
            //System.out.println("Finish");
             
            //once the file is been completely received, it states it in the console
            if (dblock.length < SIZEDB){
                 
                System.out.println("Done receiving File...");
                //breaks out of the inner loop
                break;
                 
            }
        }
         
    }
     
    //called when you need to send a datapacket to the client
    public void sendBytes(DatagramSocket sendingS, int pNumber, byte[] msg)
    {       
        DatagramPacket sendDataP;
        // send data
        try {
            sendDataP = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), pNumber);
             
            System.out.println("Client: Packet is being sent");
             
            //prints all the information in the packet that is being sent
            Utils.printInfo(sendDataP);
             
            try {
                sendingS.send(sendDataP);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            //throws an exception if it can't be sent to the client
        } catch(UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
     
    //in charge of sending the file over to the client
    //the file that is needed is passed in and then transferred over from the server folder to the client
    public void sendFile(DatagramSocket socket, int pNumber, String fName, int callerId)
    {
        byte[] fileInfo;
         
        InOut newFile = new InOut(fName);
         
        byte[] dataBlockInfo = new byte[]{0, 3};
         
        byte[] newB = new byte[]{0,1};
         
        //while the byte is still getting info from the file on its server
        while (true) {
            byte[] dataBInfo = null;
             
            //throws an exception if file cannot be sent
            try {
                dataBInfo = newFile.read(SIZEDB);
            } catch (SecurityException e) {
                String commonErrorMssg = callerId == SERVER? "server for RRQ." : "client for WRQ.";
				if (e.getMessage().contains("Permission denied")) {
                    String errorMessage = String.format("Access Violation happened on the %s", commonErrorMssg);
                    System.out.println(errorMessage);
					cAndSendError(socket, "Access violation.", 2, pNumber);
				}
                return;
            } catch (IOException e){
            	if (e.getMessage().contains("Access is denied")) {
            		//working error handler for access denied in server
					cAndSendError(socket, "Access is Denied.", 2, pNumber);
					break;
				}
        	}
            
            if (!(dataBInfo == null)){
	            fileInfo = byteArrayCreater(dataBlockInfo, newB);
	            fileInfo = byteArrayCreater(fileInfo, dataBInfo);
	            sendBytes(socket, pNumber, fileInfo);
            } else {//working error handler for file not found on server
            	cAndSendError(socket,"\nError: File not found.", 1, pNumber);
            }
             
            fileInfo = new byte[SIZEB];
            DatagramPacket ack = new DatagramPacket(fileInfo, fileInfo.length);
            if (!clientRequest(socket, ack,"req")) {
				cAndSendError(socket, "Timeout Occured.", 0, callerId);
				break;
        	}
            //clientRequest(socket, ack);

            while (checkAckType(ack.getData(), newB) == AckType.DUPLICATE) {
            	if (!clientRequest(socket, ack,"req")) {
    				cAndSendError(socket, "Timeout Occured.", 0, callerId);
    				break;
            	}
                //clientRequest(socket, ack);
                System.out.println("DUPLICATE");
            }
            
            pNumber = ack.getPort();

            if (checkAckType(ack.getData(), newB) == AckType.FRESH) {

                if (newB[1] < 255) newB[1]++;
                else if (newB[1] == 255 && newB[0] < 255) {
                    newB[0]++;
                    newB[1] = 0;
                }
                else {
                    newB[0] = 0;
                    newB[1] = 1;
                }

                if (dataBInfo.length < 512) {
                    break;
                }
            } else { // received error packet instead of ack
                break;
            }
        }
    }
     
    //creates an byte array that will be sent over to the client
    public byte[] byteArrayCreater(byte[] source, byte[] goingB)
    {
    	//System.out.println("\n\n\n\ngoingB = " + goingB + "\n\n\n\n" );
        int size = source.length+ goingB.length;
        byte[] finalBA = new byte[size];
        System.arraycopy(source, 0, finalBA, 0, source.length);
        System.arraycopy(goingB, 0, finalBA, source.length, goingB.length);
        //returns final byte array
        return finalBA;
    }
     
    //checks to see if the data is ACK or ERROR. If the data is ack, is it duplicate or not
    private AckType checkAckType(byte[] fileInfo, byte[] block) {
        //checks to see if it is 0 4 0 1

        int expectedBlockNo = Utils.getBlockNo(block);
        int receivedBlockNo = Utils.getBlockNo(fileInfo);

        if (fileInfo[0] == 0 && fileInfo[1] == 4) { // data is ack
            if (receivedBlockNo < expectedBlockNo) {
                return AckType.DUPLICATE;
            } else {
                return AckType.FRESH;
            }
        }
         
        return AckType.ERROR;
    }
     
 
    //checks to see if the content of the packet is DATA or ERROR
    private boolean checkD(byte[] fileInfo) {
        //checks to see if it is 0 4 0 1
        if (fileInfo[0] == 0 && fileInfo[1] == 3) {
            return true;
        }

        return false;
    }


    //creates an error packet and sends it depending on the type
    //not fully functional, needed for next iteration to fully work
    protected void cAndSendError(DatagramSocket socket, String info, int eInfo, int pNumber) {
        byte[] msg = {0, 5, 0, (byte)eInfo};

        msg = byteArrayCreater(msg, info.getBytes());
        msg = byteArrayCreater(msg, new byte[]{0});

        sendBytes(socket, pNumber, msg);
    }

//combined class with other group members
//deals with file input and output
    class InOut
    {
        private int location = 0;
        private int bytesRead;
        private String fileName;

        //creates a instance of the class which contains the certain file
        public InOut(String file)
        {
            fileName = file;
            location = 0;
            bytesRead = 0;
        }

        //reads from the file and returns it in byte form
        public byte[] read(int blocks) throws FileNotFoundException, IOException, SecurityException
        {
        	try {
	            BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));	
	            byte[] dataRead = new byte[blocks];
	
	            in.skip((long) location);
	            
	            if ((bytesRead = in.read(dataRead)) != -1) {
	                location += bytesRead;
	            } else {
	                location = 0;
	                bytesRead = 0;
	            }
	
	            in.close();
	
	            if(bytesRead < blocks) {
	                System.out.println("Fixing array information... ");
	                byte dataReadTrim[] = Arrays.copyOf(dataRead, bytesRead);
	                return dataReadTrim;
	            }
	
	            return dataRead;
        	} //catch (FileNotFoundException e){
        		//String errorMessage = String.format("File not found.");
                //System.out.println(errorMessage);
                //return null;
        	//}
        	finally {
        		
        	}
        	//add send error packet
        }

        //writes to the file
        public boolean write(byte[] info, DatagramSocket sock, int port, int callerId, byte[] blockbyte) throws IOException {
            FileOutputStream out = null;
            File find = new File(fileName);
            int blockNo = Utils.getBlockNo(blockbyte);

            if(find.exists() && blockNo <= 1){
            	//working error handler for file already exists
            	System.out.println(blockNo);
                String commonErrorMssg = callerId == SERVER? "server for WRQ." : "client for RRQ.";
                String errorMessage = String.format("File already exists error happened on the %s", commonErrorMssg);
                System.out.println(errorMessage);
                cAndSendError(sock, "File already exists.", 6, port);
            } else {
                try {
                    out = new FileOutputStream(fileName, true);
                } catch (IOException e) {                	
                    String commonErrorMssg = callerId == SERVER? "server for WRQ." : "client for RRQ.";
                    if (e.getMessage().contains("Permission denied")) {
                        String errorMessage = String.format("Access Violation happened on the %s", commonErrorMssg);                        
                        System.out.println(errorMessage);
                        cAndSendError(sock, "Access violation.", 2, port);
                    } else if (e.getMessage().contains("Access is denied")) {
                		//working error handler for access denied in server
    					cAndSendError(sock, "Access is Denied.", 2, port);
    				}
                    return false;
                }
            }

            try {
                out.write(info, 0, info.length);
                out.getFD().sync();
            } catch (SyncFailedException e) {
                String commonErrorMssg = callerId == SERVER? "server for WRQ." : "client for RRQ.";
                String errorMessage = String.format("Disk full happened on the %s", commonErrorMssg);
                System.out.println(errorMessage);
                cAndSendError(sock, "Disk full or allocation exceeded.", 3, port);
                out.close();
                return false;
            } catch (SecurityException e){
                System.err.println();
                return false;
            } catch (IOException e) {
                String commonErrorMssg = callerId == SERVER? "server for WRQ." : "client for RRQ.";
                if (e.getMessage().contains("Permission denied")) {
                    String errorMessage = String.format("Access Violation happened on the %s", commonErrorMssg);
                    System.out.println(errorMessage);
                    cAndSendError(sock, "Access violation.", 2, port);
                } else if (e.getMessage().contains("No such file or directory")) {
                    String errorMessage = String.format("File not found on the %s", commonErrorMssg);
                    System.out.println(errorMessage);
                    //cAndSendError(sock, "File not found.", 1, port);
                } else if (e.getMessage().contains("There is not enough space on the disk")) {
                	//working disk is full error handler for server and client
                	cAndSendError(sock, "Disk is full.", 3, port);
                }
                return false;
            }
            out.close();
            return true;
        }

    }
}

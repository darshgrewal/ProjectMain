import java.net.*;
import java.util.*;
import java.io.*;

public class TransferHub {

    //the size values shouldn't change so they were made final variables
    public final int SIZEB = 520;
    public final int SIZEDB = 512;

    public final int SERVER = 0;
    public final int CLIENT = 1;
    
    protected InetAddress IPAddress;
    protected InetAddress serverIPAddress;

    private enum ReceivedType {ERROR, FRESH, DUPLICATE, OUTOFSYNC }

    //creates a receive request to send to the main server class, creating the socket and packet to hold the info
    public boolean clientRequest(DatagramSocket rSocket, DatagramPacket rPacket, String type)
    {
    	boolean normal = false;
    	while (!normal) {
	        try {
	        	rSocket.setSoTimeout(3000);
	            rSocket.receive(rPacket);
	            //rPacket.setData(Arrays.copyOfRange(rPacket.getData(), 0, rPacket.getLength()));
	            //client receives the notification that packet has reached it from the server
	            System.out.println("\nA packet has been received.");
	            Utils.printInfo(rPacket, Utils.RECEIVE);
	            normal = true;
	        } catch (SocketTimeoutException e){
	        	if (type.equals("server")) {
	        	} else {
	        		System.out.println("Timeout occured.");
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
    // Need to send the ack even if the packet is duplicate. The duplicate ack is handled by the transmitter.
    public void getFile(DatagramSocket socket, String fName, int callerId) {
        //holds the message that will be sent over
        byte[] fileInfo = new byte[SIZEB];
        //the packet in which the message will be sent in
        DatagramPacket dataPacket = new DatagramPacket(fileInfo, fileInfo.length);

        while (!clientRequest(socket, dataPacket, "req")) {
        }

        getFile(dataPacket, socket, fName, callerId);
    }

    public void getFile(DatagramPacket dataPacket, DatagramSocket socket, String fName, int callerId){

        int expectedPacketNo = 1;

        byte aT[] = new byte[]{0, 4};
        InOut newFile = new InOut(fName);

        while (true) {
            try {
                Utils.checkPacketStructure(dataPacket, Utils.DATA);
            } catch (Utils.InvalidPacketException e) {
            	System.out.println(e.getMessage());
                cAndSendError(socket, "Illegal TFTP operation. " + e.getMessage(), 4, dataPacket.getPort());
                break;
            }

            ReceivedType receivedPacketType = checkPacketType(dataPacket.getData(),expectedPacketNo);
            // if received packet is an error no need to continue
            byte blockbyte[] = Arrays.copyOfRange(dataPacket.getData(), 2, 4);
            byte dblock[] = Arrays.copyOfRange(dataPacket.getData(), 4, dataPacket.getLength());

            if (receivedPacketType == ReceivedType.FRESH) {
                expectedPacketNo++;
                try {
                    if (!newFile.write(dblock, socket, dataPacket.getPort(), callerId, blockbyte))
                        return;
                } catch (Exception e) {
                    return;
                }
            } else if (receivedPacketType == ReceivedType.DUPLICATE) {
                System.out.println("Received a duplicated packet, hence not writing to the file.");
            } else if (receivedPacketType == ReceivedType.ERROR){ // received error packet instead of ack
                break;
            } else if (receivedPacketType == ReceivedType.OUTOFSYNC) {		
            	System.out.println("Received a packet with unexpected block number.");		
                cAndSendError(socket, "Illegal TFTP operation.", 4, dataPacket.getPort());		
                break;
            }

            byte aByte[] = byteArrayCreater(aT, blockbyte);

            sendBytes(socket, dataPacket.getPort(), aByte);

            //once the file is been completely received, it states it in the console
            if (dblock.length < SIZEDB){

                System.out.println("Done receiving File...");
                //breaks out of the inner loop
                break;

            }

            while (!clientRequest(socket, dataPacket, "req")) {
            }
        }
    }

    //called when you need to send a datapacket
    public void sendBytes(DatagramSocket sendingS, int pNumber, byte[] msg)
    {
        DatagramPacket sendDataP;
    	try {
			IPAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        // send data
        try {
            sendDataP = new DatagramPacket(msg, msg.length, IPAddress, pNumber);

            System.out.println("\nA Packet is being sent.");

            //prints all the information in the packet that is being sent
            Utils.printInfo(sendDataP, Utils.SEND);

            try {
                sendingS.send(sendDataP);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            //throws an exception if it can't be sent to the client
        } catch(IllegalArgumentException e) {
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
        File f = new File(fName);
        byte[] dataBlockInfo = new byte[]{0, 3};

        byte[] newB = new byte[]{0,1};

        //while the byte is still getting info from the file on its server
        while (true) {
            byte[] dataBInfo = null;

            //throws an exception if file cannot be sent
            if(!f.canWrite()) {
            	System.out.println("File is not accessible.");
            	cAndSendError(socket, "Access is Denied.", 2, pNumber);
    			break;
            } 
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
            } 
            else if(f.canWrite()){
            	cAndSendError(socket,"\nError: File retarded.", 1, pNumber);
            	break;
            }
            else {//working error handler for file not found on server
            	cAndSendError(socket,"\nError: File not found.", 1, pNumber);
            	break;
            }

            fileInfo = new byte[SIZEB];
            DatagramPacket ack = new DatagramPacket(fileInfo, fileInfo.length);

             // - wait to receive an ack. If timeout happens, retransmit the previous packet
             // - when an ack is received check to make sure it is not duplicate
             // - if duplicate ack received, throw that away and wait for another ack. start timeout timer.
             // - if a fresh ack is received proceed as normal
            boolean errorHappened = false;
            ReceivedType tempType = ReceivedType.DUPLICATE;
            while (tempType == ReceivedType.DUPLICATE) {
                while (!clientRequest(socket, ack,"req")) {
                    fileInfo = byteArrayCreater(dataBlockInfo, newB);
                    fileInfo = byteArrayCreater(fileInfo, dataBInfo);
                    sendBytes(socket, pNumber, fileInfo);
                }

                // check that the received packet is an ack and it is formed  correctly
                pNumber = ack.getPort();
                try {
                    Utils.checkPacketStructure(ack, Utils.ACK);
                } catch (Utils.InvalidPacketException e) {
                	System.out.println(e.getMessage());
                    cAndSendError(socket, "Illegal TFTP operation." + e.getMessage(), 4, pNumber);
                    errorHappened = true;
                    break;
                }

                tempType = checkAckType(ack.getData(), newB);
                if (tempType == ReceivedType.DUPLICATE)
                	System.out.println("Ack is Duplicate.");
                if (tempType == ReceivedType.OUTOFSYNC) {		
                    System.out.println("Block numaber in ack was unexpected.");		
	                cAndSendError(socket, "Illegal TFTP operation.", 4, pNumber);		
	                errorHappened = true;		
	                break;		
                }
            }

            if (errorHappened)
                break;

            if (tempType == ReceivedType.FRESH) {

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
        int size = source.length+ goingB.length;
        byte[] finalBA = new byte[size];
        System.arraycopy(source, 0, finalBA, 0, source.length);
        System.arraycopy(goingB, 0, finalBA, source.length, goingB.length);
        //returns final byte array
        return finalBA;
    }

    //checks to see if the data is ACK or ERROR. If the packet is ack, is it duplicate, fresh or outofsync
    private ReceivedType checkAckType(byte[] fileInfo, byte[] block) {
        //checks to see if it is 0 4 0 1

        int expectedBlockNo = Utils.getBlockNo(block);
        int receivedBlockNo = Utils.getBlockNo(fileInfo);

        if (fileInfo[0] == 0 && fileInfo[1] == 4) { // data is ack
            if (receivedBlockNo < expectedBlockNo) {
                return ReceivedType.DUPLICATE;
            } else if (receivedBlockNo == expectedBlockNo) {		
                return ReceivedType.FRESH;
            } else {
                return ReceivedType.OUTOFSYNC;
            }
        }

        return ReceivedType.ERROR;
    }


    //checks to see if the content of the packet is DATA or ERROR. If the packet is data, is it duplicate, fresh or outofsync
    private ReceivedType checkPacketType(byte[] fileInfo, int expectedBlockNo) {
        //checks to see if it is 0 4 0 1


         int receivedBlockNo = Utils.getBlockNo(fileInfo);
         //uncomment for testing
         //System.out.println(expectedBlockNo);
         //System.out.println(receivedBlockNo);

         if (fileInfo[0] == 0 && fileInfo[1] == 3) { // data is data
             if (receivedBlockNo < expectedBlockNo) {
                 return ReceivedType.DUPLICATE;
             } else if (receivedBlockNo == expectedBlockNo) {		
                 return ReceivedType.FRESH;
             } else {
                 return ReceivedType.OUTOFSYNC;
             }
         }

         return ReceivedType.ERROR;
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
	                byte dataReadTrim[] = Arrays.copyOf(dataRead, bytesRead);
	                return dataReadTrim;
	            }

	            return dataRead;
        	}
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

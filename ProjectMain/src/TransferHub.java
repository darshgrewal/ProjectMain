//TransferHub Class
//deals with most of the recieve and tranfer functions needed for the server to operate
  
import java.net.*;
import java.util.*;
import java.io.*;
  
public class TransferHub {
      
    //the size values shouldn't change so they were made final variables
    public final int SIZEB = 516;
    public final int SIZEDB = 512;  
      
    //creates a receive request to send to the main server class, creating the socket and packet to hold the info
    public void clientRequest(DatagramSocket rSocket, DatagramPacket rPacket)
    {
        try {
            rSocket.receive(rPacket);
              
            rPacket.setData(Arrays.copyOfRange(rPacket.getData(), 0, rPacket.getLength()));
            //client recieves the notification that packet has reached it from the server
            System.out.println("Host: The packet has been received.");
            displayInfo(rPacket, rPacket.getData());
        //checks for an input output exception  
        } catch (IOException inoutE){
            inoutE.printStackTrace();
            //exits if it is found
            System.exit(1);
        }
    }
      
    //class handles the files that being sent over from the client
    //stores them in a file of the users choice
  
    public void getFile(DatagramSocket socket, String fName){
          
        //holds the message that will be sent over
        byte[] fileInfo = new byte[SIZEB];
        //the packet in which the message will be sent in
        DatagramPacket dataPacket = new DatagramPacket(fileInfo, fileInfo.length);
          
        byte aT[] = new byte[]{0, 4};
        InOut newFile = new InOut(fName);
          
        while (true){
            clientRequest(socket, dataPacket);
            byte blockbyte[] = Arrays.copyOfRange(dataPacket.getData(), 2, 4);
            byte dblock[] = Arrays.copyOfRange(dataPacket.getData(), 4, dataPacket.getLength());
              
            try {
                newFile.write(dblock);
            }  catch (SyncFailedException e){
                cAndSendError(socket, "Disc full.", 3, dataPacket.getPort());
                e.printStackTrace();
                return;
            } catch (SecurityException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
              
            byte aByte[] = byteArrayCreater(aT, blockbyte);
              
            sendBytes(socket, dataPacket.getPort(), aByte);
              
            System.out.println("Finish");
              
            //once the file is been completely received, it states it in the console
            if (dblock.length < SIZEDB){
                  
                System.out.println("Done receiving packet...");
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
            displayInfo(sendDataP, msg);
              
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
    public void sendFile(DatagramSocket socket, int pNumber, String fName)
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
            } catch (SecurityException | IOException e) {
                e.printStackTrace();
                return;
            }
              
            fileInfo = byteArrayCreater(dataBlockInfo, newB);
            fileInfo = byteArrayCreater(fileInfo, dataBInfo);
              
            sendBytes(socket, pNumber, fileInfo);
              
            fileInfo = new byte[SIZEB];
            DatagramPacket ack = new DatagramPacket(fileInfo, fileInfo.length);
            clientRequest(socket, ack);
            checkA(ack.getData(), newB);
              
            if (newB[1] < 255) newB[1]++;
            else if (newB[1] == 255 && newB[0] < 255) newB[0]++;
            else {
                newB[0] = 0;
                newB[1] = 1;
            }
              
            if (dataBInfo.length < 512){ 
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
      
    //checks to see if the data is ack
    private boolean checkA(byte[] fileInfo, byte[] block) {
        //checks to see if it is 0 4 0 1
        if (fileInfo[0] == 0 && fileInfo[1] == 4 && fileInfo[2] == block[0] && fileInfo[3] == block[1]) {
            return true;
        }
          
        return false;
    }
      
  
    //creates an error packet and sends it depending on the type
    //not fully functional, needed for next iteration to fully work
    protected void cAndSendError(DatagramSocket socket, String info, int eInfo, int pNumber) {
        byte[] msg = {0, 5, 0, (byte)eInfo};
          
        msg = byteArrayCreater(msg, info.getBytes());
          
        sendBytes(socket, pNumber, msg);
    }
      
    //prints the file information
    protected void displayInfo(DatagramPacket packet, byte[] fileInfo){
        int packetLength = packet.getLength();
          
        System.out.println("Host ID: " + packet.getAddress() + " recieved on port number " + packet.getPort());
        System.out.println("The length of the packet:  " + packetLength);
        System.out.println("Containing " + new String(fileInfo,0,packetLength));
        System.out.println("Information in byte form: " + Arrays.toString(fileInfo) + "\n");
    }
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
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
          
        byte[] dataRead = new byte[blocks];
          
        in.skip((long) location);
        if((bytesRead = in.read(dataRead)) != -1)
        {
            location += bytesRead;
        }       
        else
        {
            location = 0;   
            bytesRead = 0;
        }
          
        in.close();
          
        if(bytesRead < blocks)
        {
            System.out.println("Fixing array information... ");
            byte dataReadTrim[] = Arrays.copyOf(dataRead, bytesRead);
            return dataReadTrim;
        }
          
        return dataRead;
    }
      
    //writes to the file
    public void write(byte[] info) throws FileNotFoundException, IOException, SecurityException
    {   
        BufferedOutputStream out;
        out = new BufferedOutputStream(new FileOutputStream(fileName, true));
        out.write(info, 0, info.length);    
        out.close();
    }
      
}
import java.util.Arrays;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.DatagramSocket;

public class AssistantC extends TransferHub implements Runnable{
    
    DatagramSocket SendSocket;
//    byte [] fileinfo = new byte [512];
    int pnum;
    private Server listener;
    
    private DatagramPacket reqPacket;
  
     
    public AssistantC(int Port, DatagramPacket packet, Server x){
        try {
        	reqPacket = packet;
        	listener = x;
            SendSocket = new DatagramSocket();
//            fileinfo = packet.getData();
            pnum = Port;
            IPAddress = x.IPAddress;
        }catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
    }
 
     
    //returns the position as integer next to the 0
//    private int getZero(byte[] msg, int currPos)
//    {
//        for (int i = currPos; i < msg.length; i++){
//            if (msg[i] == 0){
//                return i;
//            }
//        }
//        return 0;
//    }
      
//    protected void makeFolder(String pathname)
//    {
//        File f1 = new File(pathname);
//
//        if(!f1.exists()){
//            try{
//                f1.mkdirs();
//            }
//            catch(SecurityException se){
//                se.printStackTrace();
//            }
//        }
//    }

  //passes a string array that contains the request made by the client 
    private String[] createArray(DatagramPacket packet) throws InvalidRequestException
    {
    	byte[] message = packet.getData();
        try {
            Utils.checkPacketStructure(packet, Utils.REQ);
        }
        catch (Utils.InvalidPacketException e){
            System.out.println("Invalid packet structure was found:" + e.getMessage());
            throw new InvalidRequestException("Illegal TFTP operation.");
//          System.exit(1);
        }
                  
        String[] req = new String[3];
        if (message[1] == 1)  {
            req[0] = "Read";
        } 
        else if (message[1] == 2){
            req[0] = "Write";
        } 
        Integer prev = 1;
        Integer next =  Utils.getZero(packet.getData(), packet.getLength(), prev);
          
        req[1] = new String(Arrays.copyOfRange(message, prev + 1, next));
          
        prev = new Integer(next);
        next =  Utils.getZero(packet.getData(), packet.getLength(), prev+1);
          
        req[2] = new String(Arrays.copyOfRange(message, prev + 1, next));
          
        return req;
    }
     
 
//    //check to see if the data packet is valid and formatted properly
//    private void packetVerify(byte[] packetData) throws InvalidRequestException
//    {
//        if(packetData[0] != (byte)0){
//            throw new InvalidRequestException("Error first byte is not a 0");
//        }
//        if(packetData[1] != (byte)1 && packetData[1] != (byte)2){
//            throw new InvalidRequestException("Error second byte is not a 1 or a 2");
//        }
//        if(packetData[2] == (byte)0){
//            throw new InvalidRequestException("Error no file name");
//        }
//    }
      
    //lets the client know the response depending on which type of request is being received
    private void reqHandle(DatagramPacket packet, int port){
    	byte[] message;
        String[] req = {};
          
        try {
            DatagramSocket requestSocket = new DatagramSocket();
            try {
                req = this.createArray(packet);
            } catch (InvalidRequestException e1) {
                cAndSendError(requestSocket, e1.getMessage(), 4, port);
                return;
            }
              
            if (req[0] == "Write"){
                message = new byte[]{(byte)0, (byte)4, (byte)0, (byte)0};
                sendBytes(requestSocket, port, message);
                getFile(requestSocket, "Server/" + req[1], SERVER);
            } else if (req[0] == "Read"){
                sendFile(requestSocket, port, "Server/" + req[1], SERVER);
            }
            listener.removeThread();
            requestSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
      
    public void run() {
        reqHandle(reqPacket, this.pnum);
        SendSocket.close();
    }
    
          
}

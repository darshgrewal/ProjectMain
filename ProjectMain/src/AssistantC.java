import java.util.Arrays;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.DatagramSocket;

public class AssistantC extends TransferHub implements Runnable{
    
    DatagramSocket SendSocket;
//    byte [] fileinfo = new byte [512];
    int pnum;
    Server listener;
    
    private DatagramPacket reqPacket;
  
     
    public AssistantC(int Port, DatagramPacket packet, Server x){
        try {
        	reqPacket = packet;
        	listener = x;
            SendSocket = new DatagramSocket();
//            fileinfo = packet.getData();
            pnum = Port;
            //IPAddress = x.IPAddress;
            IPAddress = packet.getAddress();
        }catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
    }
 
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

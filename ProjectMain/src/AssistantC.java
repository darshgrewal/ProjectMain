import java.io.File;
import java.util.Arrays;
import java.net.SocketException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
  
public class AssistantC extends TransferHub implements Runnable{
    
    DatagramPacket SendPacket;
    DatagramSocket SendSocket;
    byte [] fileinfo = new byte [512];
    int threads;
    int pnum;
    Server listener;
  
     
    public AssistantC(int Port, byte [] Data, Server x){
        try {
        	listener = x;
            SendSocket = new DatagramSocket();
            fileinfo = Data;
            pnum = Port;              
        }catch(SocketException se){
            se.printStackTrace();
            System.exit(1);
        }
    }
 
     
    //returns the position as integer next to the 0
    private int getZero(byte[] msg, int currPos)
    {
        for (int i = currPos; i < msg.length; i++){
            if (msg[i] == 0){
                return i;
            }
        }
        return 0;
    }
      
    protected void makeFolder(String pathname)
    {
        File f1 = new File(pathname);
         
        if(!f1.exists()){
            try{
                f1.mkdirs();
            }
            catch(SecurityException se){
                se.printStackTrace();
            }
        }
    }
     
  //passes a string array that contains the request made by the client 
    private String[] createArray(byte[] message) throws InvalidRequestException
    {
        try {
            packetVerify(message);
        }
        catch (InvalidRequestException e){
            System.out.println("Error");
            e.printStackTrace();
            System.exit(1);
        }
                  
        String[] req = new String[3];
        if (message[1] == 1)  {
            req[0] = "Read";
        } 
        else if (message[1] == 2){
            req[0] = "Write";
        } 
        Integer prev = 1;
        Integer next =  getZero(message, prev);
          
        req[1] = new String(Arrays.copyOfRange(message, prev + 1, next));
          
        prev = new Integer(next);
        next =  getZero(message, prev+1);
          
        req[2] = new String(Arrays.copyOfRange(message, prev + 1, next));
          
        return req;
    }
     
 
    //check to see if the data packet is valid and formatted properly  
    private void packetVerify(byte[] packetData) throws InvalidRequestException
    {       
        if(packetData[0] != (byte)0){
            throw new InvalidRequestException("Error first byte is not a 0");
        }
        if(packetData[1] != (byte)1 && packetData[1] != (byte)2){
            throw new InvalidRequestException("Error second byte is not a 1 or a 2");
        }
        if(packetData[2] == (byte)0){
            throw new InvalidRequestException("Error no file name");
        }
    }
      
    //lets the client know the response depending on which type of request is being received
    private void reqHandle(byte[] reqBytes, int port){
        byte[] message;
        String[] req = {};
          
        try {
            DatagramSocket requestSocket = new DatagramSocket();
            try {
                req = this.createArray(reqBytes);
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
        reqHandle(this.fileinfo, this.pnum);
        SendSocket.close();
    }
    
          
}

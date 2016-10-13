//Darshpreet Grewal
//100972449
 
import java.util.Scanner;
import java.io.File;
  
//class used to close server 
public class CloseServer implements Runnable {
      
    private Scanner in = new Scanner(System.in);
    private Server EndServer;
      
    public CloseServer(Server x){
        EndServer = x;
    }
      
    public void run(){
        while(true){
            System.out.println("To close server, please enter 'close'.");
            getTextInfo(in.next());
        }
    }
      
    private void getTextInfo(String inputData){
        if (inputData.equals("close")){
            EndServer.holder = false;
            EndServer.shutdownServer();
            System.exit(1);
        }
    }
      
}
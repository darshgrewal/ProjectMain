import java.util.Scanner;
  
//class used to close server 
public class CloseServer implements Runnable {
      
    private Scanner in = new Scanner(System.in);
    private Server EndServer;
    private Boolean done;
      
    public CloseServer(Server x){
        EndServer = x;
    }
      
    public void run(){
    	done = false;
        while(true){
            System.out.println("To close server, please enter 'close'.");
            getTextInfo(in.next());
        }
    }
      
    private void getTextInfo(String inputData){
        if (inputData.equals("close")){
        	while (true) {
        		if (EndServer.getThreads() == 0) {
        			EndServer.shutdownServer();
    	            System.exit(1);
        		}
        		try {
        			Thread.sleep(500);
        		} catch (InterruptedException e) {
        			
        		}
        	}        		
        }
    }
}

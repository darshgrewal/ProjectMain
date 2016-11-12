import java.net.SocketException;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


//used to throw invalid exceptions
class InvalidRequestException extends Exception
{
	public InvalidRequestException() {}	
	public InvalidRequestException(String IREText)
	{
		super(IREText);
	}
}

public class Server extends TransferHub implements Runnable{
	
	
	public Boolean holder = true; 		//boolean value which decides whether server should keep checking for incoming requests
	private DatagramPacket rPacket;  	//datagram packet for the server
	private DatagramSocket rSocket;		//datagram socket for the server
	int threads = 0;
	
	public Server()
	{
		try {
			rSocket = new DatagramSocket(2269);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	//class creates a folder
	//did not learn this in any class, used online resources to make
	//Reference: https://www.mkyong.com/java/how-to-create-directory-in-java/
	protected void createFolder(String folderName)
    {
        File filedirectory = new File(folderName);
       
        if(!filedirectory.exists())
        {
            try
            {
            	filedirectory.mkdirs();
            }
            catch(SecurityException se)
            {
                se.printStackTrace();
            }
        }
    }
	
	//closes the socket and terminates the server
	public void shutdownServer(){
		
		System.out.println("Server is now going offline.");
		
		this.rSocket.close();
		
		System.exit(1);
	}

	//starts running the server, keeps on looping to check if a packet has been recieved
	//runs till the server is told to quit, changes temp to false and thus terminates the server
	public void run()
	{
		System.out.println("Server is running.");
		while (holder){
			byte dByte[] = new byte[this.SIZEB];
		
			this.rPacket = new DatagramPacket(dByte, dByte.length);
			this.clientRequest(this.rSocket, this.rPacket, "server");
			this.addThread();
			
			Thread serverX = new Thread (new AssistantC(this.rPacket.getPort(), this.rPacket.getData(),this));			
			serverX.start();
		}
	}
	
	
	//main creates a new server to run and creates a folder to store the date 
	//to close the server a secondary thread will be created to terminate all threads and exit
	public static void main(String args[]){
		Server startServer = new Server();
		startServer.createFolder("server");
		
		//creates a new server thread
		Thread threadS = new Thread (startServer);
		threadS.start();
		
		//creates a thread to end the server when needed
		Thread endServer = new Thread (new CloseServer(startServer));
		endServer.start();
	}
	
	public void addThread() {
        threads++;
    }

    public void removeThread() {
        threads--;
    }
    
    public int getThreads() {
    	return threads;
    }
}

import java.io.File;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.util.Scanner;

public class Client extends TransferHub
{
	private DatagramSocket socketSR;
	
	public Client()
	{
		try {
			socketSR = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void getandSend()
	{
		Scanner scan = new Scanner(System.in);
		int socket = 2269;
		
		while(true){
			
			System.out.print("Enter (1) to run normally, enter (2) to run in test mode: ");
			int mode = scan.nextInt();
			
			if ( mode == 1 ) {
				socket = 2269;
				break;
			} else if ( mode == 2 ) {
				socket = 2268;
				break;
			}
		}
		
		while(true) {
			byte message[];

			System.out.print("Enter (1) to begin reading the file, enter (2) to begin writing to the file (3) to quit: ");
			int choice = scan.nextInt();
			if ( choice > 0 && choice < 3 ){
				message = new byte[]{(byte)0, (byte)choice};
			} 
			else if ( choice == 3 ){
				break;
			} else {
				continue;
			}
			
			System.out.print("Enter file name you want to use: ");
			String fName = scan.next();

			String x = "octet";
			
			message = byteArrayCreater(message, fName.getBytes());
			message = byteArrayCreater(message, new byte[]{0});
			message = byteArrayCreater(message, x.getBytes());
			message = byteArrayCreater(message, new byte[]{0});
			
			sendBytes(socketSR, socket, message);
			// RRQ
			if (message[1] == 1){
				getFile(socketSR, "client/" + fName, CLIENT);
			} else if (message[1] == 2) {	//WRQ
				message = new byte[SIZEB];
				DatagramPacket receivePacket = new DatagramPacket(message, message.length);
				clientRequest(socketSR, receivePacket);
				
				if (receivePacket.getData()[0] == 0 &&
						receivePacket.getData()[1] == 4 &&
						receivePacket.getData()[2] == 0 &&
						receivePacket.getData()[3] == 0)
				sendFile(socketSR, receivePacket.getPort(), "client/" + fName, CLIENT);
			}
		}
		
		scan.close();
	}
	
	protected void createFolder(String name){
        File filedirectory = new File(name);
        if(!filedirectory.exists()){
            try{
                filedirectory.mkdirs();
            }
            catch(SecurityException se){
                se.printStackTrace();
            }
        }
    }
	
	public static void main(String[] args){
		Client cthread = new Client();
		cthread.createFolder("client");
		cthread.getandSend();
	}
}

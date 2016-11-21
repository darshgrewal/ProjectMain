import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.util.Scanner;
import java.net.InetAddress;

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
		while (true){
			try
			{
				IPAddress = InetAddress.getLocalHost();
				break;
			}
			catch(UnknownHostException uhe)
			{
				System.out.println("Unknown host...");
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
			if (choice == 2) {
				while (true) {
					try {
			            FileInputStream test = new FileInputStream(System.getProperty("user.dir") + "/client/" + fName);
			            test.close();
			            break;
		        	} catch (IOException e){
		            	if (e.getMessage().contains("Access is denied")) {
		            		//working error handler for access denied in client
		            		System.out.println("Access is denied. Enter valid filename.");
			        		System.out.print("Enter file name you want to use: ");
			    			fName = scan.next();;
		            	} else {
		            		//working error handler for file not found on client
			        		System.out.println("File not found. Enter valid filename.");
			        		System.out.print("Enter file name you want to use: ");
			    			fName = scan.next();
		            	}
		        	}
				}				
			}

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
				while (!clientRequest(socketSR, receivePacket,"req")) {
					System.out.println("TimeoutOccured");
	        	}
				//clientRequest(socketSR, receivePacket);
				//cAndSendError(socket, "Timeout Occured.", 0, callerId);
				try {
					Utils.checkPacketStructure(receivePacket, Utils.ACK);
					if (receivePacket.getData()[2] == 0 &&
						receivePacket.getData()[3] == 0)
							sendFile(socketSR, receivePacket.getPort(), "client/" + fName, CLIENT);
					else
						cAndSendError(socketSR, "Illegal TFTP operation.", 4, socket);
				} catch (Utils.InvalidPacketException e) {
	            	System.out.println(e.getMessage());
					cAndSendError(socketSR, "Illegal TFTP operation. " + e.getMessage(), 4, socket);
				}
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

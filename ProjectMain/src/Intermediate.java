import java.io.*;
import java.net.*;
  
public class Intermediate implements Runnable{
  
    DatagramSocket receiveSocket, sendReceiveSocket;
    int threads = 0;//number of current threads
  
    public Intermediate()
    {
        try {
            receiveSocket = new DatagramSocket(23);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
      
    public void run()
    {
        System.out.print("Enter (1) for normal operation, enter (1) to lose a packet, enter (3) delay a packet, or enter (4) to duplicate a packet:");
        int choice = scan.nextInt();		

        if ( choice == 1){
          //do nothing
        } else if ( choice == 2 ){
          System.out.print("Choose an integer number for which packet to lose:");
          int chosenPacket = scan.nextInt();
        } else if ( choice == 3 ){
          System.out.print("Choose an integer number for which packet to delay:");
          int chosenPacket = scan.nextInt();
          System.out.print("Choose an integer number for how many milliseconds to delay:");
          int delay = scan.nextInt();
        } else if ( choice == 4) {
          System.out.print("Choose an integer number for which packet to duplicate:");
          int chosenPacket = scan.nextInt();	
        }
      
        while(true){
            try {
                addThread();
                //create packet for recieving
                byte data[] = new byte[100];
                DatagramPacket forwardingPacket = new DatagramPacket(data, data.length);
                  
                // Receive packet from client
                receiveSocket.receive(forwardingPacket);                
                InetAddress clientAddress = forwardingPacket.getAddress();
                int clientPort = forwardingPacket.getPort();
                  
                //Process the received datagram.
                printInfo(forwardingPacket);
                
              
                /* insert statement to check for client sent ack or client sent data or request and chosen packet*/
                if (true) {
                  
                  // Forward packet to server
                  forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), InetAddress.getLocalHost(), 69);

                  //print out packet sent
                  System.out.println("Intermediate: Packet sending:");
                  System.out.println("String: " + new String(forwardingPacket.getData(),0,forwardingPacket.getLength()));
                  System.out.println("Bytes: " + forwardingPacket.getData());       

                  sendReceiveSocket = new DatagramSocket();
                  sendReceiveSocket.send(forwardingPacket);

                  // Receive response from server
                  data = new byte[100];
                  forwardingPacket = new DatagramPacket(data, data.length);

                  sendReceiveSocket.receive(forwardingPacket);

                  //print information
                  printInfo(forwardingPacket);
                  
                  /* insert statement to check for server sent ack or server sent data and chosen packet*/
	                if (true) {
                    // Forward response to client
                    forwardingPacket = new DatagramPacket(forwardingPacket.getData(), forwardingPacket.getLength(), clientAddress, clientPort);
                    sendReceiveSocket.send(forwardingPacket);

                    //print out sent datagram
                    System.out.println("Intermediate: Packet sending:");
                    System.out.println("String: " + new String(forwardingPacket.getData(),0,forwardingPacket.getLength()));
                    System.out.println("Bytes: " + forwardingPacket.getData());
                  }
                  
                }
              
                removeThread();
                stop(sendReceiveSocket);
                  
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
      
    public void printInfo(DatagramPacket x){
          
        // Process the received datagram.
        System.out.println("Intermediate:");
        System.out.println("Host: " + x.getAddress());
        System.out.println("Host port: " + x.getPort());
        int len = x.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: " );
      
        // Form a String from the byte array.
        System.out.println("String: " + new String(x.getData(),0,len));
        System.out.println("Bytes: " + x.getData());
          
    }
     
    public void addThread() {
        threads++;
    }
     
    public void removeThread() {
        threads--;
    }
     
    public void stop(DatagramSocket x){
         
        x.close();
        removeThread();
         
    }
      
    public static void main(String[] args)
    {
        (new Thread(new Intermediate())).start();
    }
  
}

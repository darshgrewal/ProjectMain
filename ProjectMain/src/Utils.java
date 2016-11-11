import java.net.DatagramPacket;
import java.util.Arrays;

public class Utils {
    public static int getBlockNo(DatagramPacket packet) {
        return ((packet.getData()[2] & 0xFF) * 256) + (packet.getData()[3] & 0xFF);
    }

    public static int getBlockNo(byte[] packet) {
        int MSB = packet.length - 2;
        int LSB = packet.length - 1;
        return ((packet[MSB] & 0xFF) * 256) + (packet[LSB] & 0xFF);
    }

    public static void printInfo(DatagramPacket packet) {
        int packetLength = packet.getLength();

        // Process the received datagram.
        System.out.println("\nHost ID: " + packet.getAddress());
        System.out.println("Recieved on port number: " + packet.getPort());
        System.out.println("Length of the packet: " + packetLength);
        // Form a String from the byte array.
        int block = getBlockNo(packet);
        int packType = packet.getData()[1];
        if (packType == 1) {
            System.out.println("Type: Read Request Packet");
        } else if (packType == 2) {
            System.out.println("Type: Write Request Packet");
        } else if (packType == 3) {
            System.out.println("Type: Data Packet");
        } else if (packType == 4) {
            System.out.println("Type: ACK Packet");
        } else if (packType == 5) {
            System.out.println("Type: Error Packet");
        }
        System.out.println("Block Number: " + block);
        System.out.println("Containing " + new String(packet.getData(),0,packetLength));
        System.out.println("Information in byte form: " + Arrays.toString(packet.getData()));

    }


}

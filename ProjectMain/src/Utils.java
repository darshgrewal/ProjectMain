import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static final boolean SEND = true;
    public static final boolean RECEIVE = false;

    public static final byte RRQ = 1;
    public static final byte WRQ = 2;
    public static final byte DATA = 3;
    public static final byte ACK = 4;
    public static final byte ERR = 5;
    public static final byte REQ = 10;

    public static final Map<Byte, String> upCodeTypeMap = new HashMap<>();
    static {
        upCodeTypeMap.put(DATA, "DATA");
        upCodeTypeMap.put(ACK, "ACK");
        upCodeTypeMap.put(ERR, "ERR");
        upCodeTypeMap.put(REQ, "RRQ/WRQ");
        upCodeTypeMap.put(RRQ, "RRQ");
        upCodeTypeMap.put(WRQ, "WRQ");
    }

    public static int getBlockNo(DatagramPacket packet) {
        return ((packet.getData()[2] & 0xFF) * 256) + (packet.getData()[3] & 0xFF);
    }

    public static int getBlockNo(byte[] packet) {
        int MSB;
        int LSB;
        if(packet.length > 4) {
            MSB = 2;
            LSB = 3;
        } else {
            MSB = packet.length - 2;
            LSB = packet.length - 1;
        }

        return ((packet[MSB] & 0xFF) * 256) + (packet[LSB] & 0xFF);
    }

    public static void printInfo(DatagramPacket packet, boolean sendReceive) {
        int packetLength = packet.getLength();

        // Process the received datagram.
        System.out.println("\nHost ID: " + packet.getAddress());
        if(sendReceive == SEND) {
            System.out.println("Sent on port number: " + packet.getPort());
        } else {
            System.out.println("Received on port number: " + packet.getPort());
        }
        System.out.println("Length of the packet: " + packetLength);
        // Form a String from the byte array.
        int block = getBlockNo(packet);
        byte packType = packet.getData()[1];
        System.out.printf("Type: %s Packet\n", upCodeTypeMap.get(packType));

        System.out.println("Block Number: " + block);
        System.out.println("Containing " + new String(packet.getData(),0,packetLength));
        System.out.println("Information in byte form: " + Arrays.toString(Arrays.copyOfRange(packet.getData(), 0, packetLength)));

    }

    //returns the position as integer next to the 0
    public static int getZero(byte[] data, int startPos)
    {
        for (int i = startPos; i < data.length; i++){
            if (data[i] == 0){
                return i;
            }
        }
        return -1;
    }

    //
    public static void checkPacketStructure(DatagramPacket packet, byte expectedType) throws InvalidPacketException{
        checkPacketStructure(packet.getData(), expectedType);
    }

    public static void checkPacketStructure(byte[] data, byte expectedType) throws InvalidPacketException{
        if (data[0] != 0) throw new InvalidPacketException("First byte should be zero.");

        String errorMessage = String.format("Was expecting %s but received %s",
                                            upCodeTypeMap.get(expectedType),
                                            upCodeTypeMap.get(data[1]));

        if (expectedType == REQ) {
            if (data[1] != WRQ && data[1] != RRQ)
                throw new InvalidPacketException(errorMessage);
        }
        else if (data[1] != expectedType) throw new InvalidPacketException(errorMessage);
        switch (data[1]) {
            case RRQ:
            case WRQ:
                int currentIndex = 2;
                int index = getZero(data, currentIndex);
                if (index == -1)
                    throw new InvalidPacketException("Request packet is missing the intermediate and end 0 bytes.");
                if (index == currentIndex)
                    throw new InvalidPacketException("File Name is missing from the request packet");
                currentIndex = index + 1;
                index = getZero(data, currentIndex);
                if (index == -1)
                    throw new InvalidPacketException("Request packet does not end in a 0");
                if (index == currentIndex)
                    throw new InvalidPacketException("Mode is missing from the request packet");
                String mode = new String(Arrays.copyOfRange(data,currentIndex,index)).toLowerCase();
                if(!mode.equals("mail") && !mode.equals("netascii") && !mode.equals("octet"))
                    throw new InvalidPacketException("Mode is neither mail, netascii nor octet.");
                break;
            case DATA:
                if ((data.length < 5) || (data.length > 516))
                    throw new InvalidPacketException("Data packet size is incorrect.");
                break;
            case ACK:
                if (data.length != 4)
                    throw new InvalidPacketException("Ack packet size is incorrect.");
                break;
//            case ERR:
//                if (data.length < 5)
//                    throw new InvalidPacketException("Error packet is too short");
//                if (data[2] != 0 || data[3] > 7)
//                    throw new InvalidPacketException("Error code in the error packet is invalid");
//                if (getZero(data, 4) == -1)
//                    throw new InvalidPacketException("Error packet does not end in a 0");
//                break;
            default:
                throw new InvalidPacketException("Packet type unrecognized.");
        }
    }

    //used to throw invalid exceptions
    static class InvalidPacketException extends Exception
    {
        public InvalidPacketException() {}
        public InvalidPacketException(String IREText)
        {
            super(IREText);
        }
    }
}

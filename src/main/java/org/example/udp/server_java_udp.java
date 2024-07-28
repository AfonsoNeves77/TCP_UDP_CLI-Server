package org.example.udp;
import org.example.anonymizedService.anonymize_Service;

import java.io.IOException;
import java.net.*;

import java.util.HashMap;

/**
 * UDP/IP server that sends back an anonymized message, depending on the client's input.
 * This message may have to be sent in several parts, depending on the buffer length of the datagram packets.
 * In order to correctly run the program, the server port must be passed as a command-line argument.
 * For every message to be sent, the server first informs the client about the number of packets it should receive.
 * Only after this operation, the proper message is sent (in several packets, as needed).
 * Additionally, for every packet sent, an acknowledgment must be received before sending the next one.
 * Transmission will fail after 3 attempts to send the same packet, without any feedback from the client.
 * Any client information stored by the server will be deleted.
 */

public class server_java_udp {
    private DatagramSocket udpSocket;
    private byte[] receiveData = new byte[bufferLength];
    private byte[] sendData = new byte[bufferLength];
    private static int bufferLength = 20;
    private HashMap<String,String> clientMap = new HashMap<>();

    /**
     * Starts the UDPServer, binding it to the specified port
     * @param port UDP port to run the server
     */
    server_java_udp(int port){
        udpSocket = null;

        try{
            udpSocket = new DatagramSocket(port);
            System.out.println("Server listening on port: " + udpSocket.getLocalPort());
        }catch (IOException e) {
            System.err.println("Server could not provide a port. Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Waits for client data (client name, port number and message).
     * On a first client attempt: Saves the message in a map, to map all client's information.
     * On a second client attempt: Receives a keyword and anonymizes the message stored for the corresponding client.
     * 1) Successful case: Handles the message to be anonymized and sends it back, followed by the message "Socket Programming"
     * as many times as the chosen keyword is present in the original message.
     * 2) Unsuccessful case: At any point, if server does not receive an acknowledgment after trying to send the same
     * message for 3 consecutive times, it will print "Result transmission failed. Terminating!".
     */
    public void waitPackets(){
        while (true) {
            try{
                String[] received = receiveMessage();
                if(received == null) {
                    System.out.println("Did not receive valid string from client. Terminating!");
                    continue;
                }

                String hostname        = received[0];
                int remotePort         = Integer.parseInt(received[1]);
                String message         = received[2];
                InetAddress remoteAddr = InetAddress.getByName(hostname);

                String client = remoteAddr.toString() + " - " + remotePort;

                //First registers the client in the map
                if(clientMap.get(client) == null){
                    clientMap.put(client,message);
                }else{
                    //When we already have an entry for that client, it means the message is already registered.
                    //In this case, keyword is received and sent along with the message to be anonymized.
                    anonymize_Service anonymizeService = new anonymize_Service();
                    String[] data = anonymizeService.stringAnonymizer(clientMap.get(client),message);
                    boolean successfulDeliver = true;
                    for(String singleString : data){
                        if(sendMessage(singleString,remoteAddr,remotePort) == -1) {
                            System.out.println("Result transmission failed. Terminating!");
                            successfulDeliver = false;
                            break;
                        }
                    }
                    if(successfulDeliver){
                        int repetitions = Integer.parseInt(data[1]);
                        for(int i = 0; i < repetitions; i++) {
                            if (sendMessage("Socket Programming", remoteAddr, remotePort) == -1) {
                                System.out.println("Result transmission failed. Terminating!");
                                break;
                            }
                        }
                    }
                    clientMap.remove(client);
                }

            }catch(SocketException e){
                System.err.println("Socket error: " + e.getMessage());
            }catch(IOException e) {
                System.err.println("I/O error: " + e.getMessage());
            }
        }
    }

    /**
     * The function first receives a reliable packets (ACKnowledge) and stores it in array. That array contains the message,
     * the sending host and port. First receives total number of fragments to be received. Then, it proceeds
     * to receive and concatenate each fragment until the complete message is reconstructed.
     * @return The reconstructed message if successful and returns array with information
     * or null if an error occurs during the reception or if the received message does not
     * contain the expected fragment information.
     * @throws SocketException In case server waits for a message for more than 500 milliseconds.
     */
    private String[] receiveMessage() throws SocketException {
        String[] received = receiveReliablePacket();
        String message = received[2];

        if (!message.contains("Packets: ")){
            return null;
        }
        // Split the string by colon
        String[] parts = message.split(":");
        if (parts.length < 2){
            return null;
        }
        int numberFragments = Integer.parseInt(parts[1].trim());

        String finalMessage = "";
        udpSocket.setSoTimeout(500);
        for(int i = 0; i < numberFragments; i++) {
            String splitMessage = receiveReliablePacket()[2];
            finalMessage = String.join("", finalMessage, splitMessage);
        }
        received[2] = finalMessage;
        udpSocket.setSoTimeout(0);
        return received;

    }

    /**
     * Stores the received information returned by receivePacket() function
     *  which includes information about the sender's hostname, port, and the message content.
     *  After receiving that information packet, it sends an acknowledgment (ACK) packet back to the client.
     *
     * @return An array containing information about the received packet: hostname, port, and the message content.
     */
    private String[] receiveReliablePacket() {
        String[] received = new String[3];
        try{
            received = receivePacket();
            String hostname = received[0];
            int port = Integer.parseInt(received[1]);
            InetAddress address = InetAddress.getByName(hostname);
            sendPacket(address,port,"ACK");

        }catch(SocketTimeoutException e){
            System.err.println("Timeout reached: " + e.getMessage());
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }
        return received;
    }

    /**
     * Receives a DatagramPacket from the UDP socket, extracts information about the sender's hostname, port,
     * and the message content, and returns this information as an array.
     *
     * @return An array containing information about the received packet: hostname, port, and the message content.
     * @throws IOException If an I/O error occurs during the packet reception.
     */
    private String[] receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        udpSocket.receive(packet);
        String[] received = new String[3];
        received[0] = packet.getAddress().getHostAddress();
        received[1] = Integer.toString(packet.getPort());
        received[2] = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    /**
     * Determining the total number of packets required for sending/reconstructing the original message and then
     * storing an array of chunks of data from the provided message.
     * The process involves transmitting the count of fragments first, followed by the transmission of the fragmented message.
     * Every sending process is made in a reliable way.
     * @param message Message to be sent
     * @param address IP address of the UDP server
     * @param port  Port where UDP server is running
     * @return (-1) if string or number of fragments sending failed; (0) if sending was successful
     * @throws SocketException In case server waits for an ACK for more than 1000 milliseconds.
     */
    private int sendMessage(String message, InetAddress address, int port) throws SocketException {
        //Calculate number of fragments, in case buffer length is lower than the message length
        int messageLength = message.length();
        int numberOfFragments = (int) Math.ceil((double) messageLength / bufferLength);
        String[] fragmentedMessage = divideMessage(message,numberOfFragments);

        if(sendReliablePacket("Packets: " + numberOfFragments,address,port) == -1){
            return -1;
        }
        udpSocket.setSoTimeout(1000);
        for(int i = 0; i < numberOfFragments; i++){
            if(sendReliablePacket(fragmentedMessage[i],address,port) == -1){
                return -1;
            }
        }
        udpSocket.setSoTimeout(0);
        return 0;
    }

    /**
     * Dividing the provided message into fragments, the size of each determined by the buffer size,
     * @param message Message to be sent
     * @param numberOfFragments number of fragments in which the message should be divided
     * @return an array of the fragmented message.
     */
    private String[] divideMessage(String message, int numberOfFragments){
        String[] fragments = new String[numberOfFragments];
        if(numberOfFragments == 1){
            fragments[0] = message;
            return fragments;
        }
        for(int i = 0; i < numberOfFragments; i++){
            int start = i * bufferLength;
            int end = Math.min((i + 1) * bufferLength, message.length());
            fragments[i] = message.substring(start,end);
        }
        return fragments;
    }

    /**
     * Sends the received message to the indicated address and port. If message send is acknowledged by the client the function returns 1
     * if not it retries sending two more times. If message is not acknowledged by the third time, the function exits and returns (-1)
     *  @param message  Message to be sent
     *  @param address  IP address of the UDP client
     *  @param port     Port where UDP client is running
     * @return (-1) if string or number of fragments sending failed; (0) if sending was successful;
     */
    private int sendReliablePacket(String message, InetAddress address, int port) {
        int counter = 0;
        String received = "NONE";
        while (!received.equals("ACK") && counter < 3) {
            counter++;
            try{
                    sendPacket(address,port,message);
                    received = receivePacket()[2];

            }catch(SocketTimeoutException e){
                System.err.println("Timeout reached: " + e.getMessage());
            }catch (IOException e) {
                System.err.println("I/O error: " + e.getMessage());
            }
        }

        //To validate if the message was successfully sent:
        if(counter == 3){
            System.out.println("Failed to send string. Terminating!");
            return -1;
        }
        return 0;
    }

    /**
     * Extracts message bytes and stores them in a byte array. Then sends that information using a DatagramPacket.
     * @param message  Message to be sent
     * @param address  IP address of the UDP client
     * @param port     Port where UDP client is running
     * @throws IOException  if an I/O error occurs.
     */
    private void sendPacket(InetAddress address, int port, String message) throws IOException {
        sendData = message.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
        udpSocket.send(packet);
    }


    /**
     * Creates a UDP DatagramSocket and establish the server to wait for client packets
     * @param args The server's port should be passed here
     **/
    public static void main(String[] args) {
        if (args.length < 1){
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        if (port < 1024 || port > 49151) {
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }

        server_java_udp serverSide = new server_java_udp(port);

        serverSide.waitPackets();
    }
}

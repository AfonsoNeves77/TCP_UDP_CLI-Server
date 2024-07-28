package org.example.udp;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import static java.lang.Integer.parseInt;

/**
 * UDP/IP client that sends a message and a keyword to be anonymized to a UDP server running on a specific port.
 * These message and keyword may have to be sent in several parts, depending on the buffer length of the datagram packets.
 * No arguments are needed to execute the file. However, in order to have a successful result, server data
 * (IP or name, and port number) as well as valid message and keyword (not empty) must be passed by the user upon request.
 * For every message to be sent, the server first informs the client about the number of packets it should receive.
 * Only after this operation, the proper message is sent (in several packets, as needed).
 * Additionally, for every packet sent, an acknowledgment must be received before sending the next one.
 * Transmission will fail after 3 attempts to send the same packet without any feedback from the server. DatagramSocket closes.
 * If the entire process succeeds, the anonymized message is printed on the console, followed by the statement
 * "Socket Programming" as many times as the keyword was found in the message. Client's DatagramSocket is closed.
 */

public class client_java_udp {
    private DatagramSocket udpSocket;
    private byte[] receiveData = new byte[bufferLength];
    private byte[] sendData = new byte[bufferLength];
    private static int bufferLength = 20;

    /**
     * Creates a DatagramSocket and sets its reception timeout
     * @param timeout Timeout set for packet reception
     */
    public client_java_udp(int timeout) {
        try{
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(timeout);
        }catch (SocketException e) {
            System.err.println("Socket error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determining the total number of packets required for sending/reconstructing the original message and then
     * storing an array of chuncks of data from the provided message.
     * The process involves transmitting the count of fragments first, followed by the transmission of the fragmented message.
     * Every sending process is made in a reliable way.
     * @param message Message to be sent
     * @param address IP address of the UDP server
     * @param port    Port where UDP server is running
     * @return (-1) if string or number of fragments sending failed; (0) if sending was successful;
     */

    public int sendMessage(String message, InetAddress address, int port) {
        //Calculate number of fragments, in case buffer length is lower than the message length
        int messageLength = message.length();
        int numberOfFragments = (int) Math.ceil((double) messageLength / bufferLength);
        String[] fragmentedMessage = divideMessage(message,numberOfFragments);

        if(sendReliablePacket("Packets: " + numberOfFragments,address,port) == -1){
            return -1;
        }

        for(int i = 0; i < numberOfFragments; i++){
            if(sendReliablePacket(fragmentedMessage[i],address,port) == -1){
                return -1;
            }
        }
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
     * Sends the received message to the indicated address and port. If message send is acknowledged by the server the function returns 1
     * if not it retries sending two more times. If message is not acknowledged by the third time, the function exits and returns (-1)
     * @param message  Message to be sent
     * @param address  IP address of the UDP server
     * @param port     Port where UDP server is running
     * @return (-1) if string or number of fragments sending failed; (0) if sending was successful;
     */

    private int sendReliablePacket(String message, InetAddress address, int port) {
        int counter = 0;
        String received = "NONE";
        while (!received.equals("ACK") && counter < 3) {
            counter++;
            try {
                sendPacket(message, address, port);
                received = receivePacket();

            }catch(SocketTimeoutException e){
                System.err.println("Timeout reached: " + e.getMessage());
            }catch(IOException e){
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
     * @param address  IP address of the UDP server
     * @param port     Port where UDP server is running
     * @throws IOException  if an I/O error occurs.
     */

    private void sendPacket(String message, InetAddress address, int port) throws IOException {
        sendData = message.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
        udpSocket.send(packet);
    }

    /**
     * Returns a message from the specified host and port by retrieving fragments of the message
     * and reconstructing the original message. The function first receives a reliable packet (ACKnowledge),
     * which includes information about the total number of fragments to be received. Then, it proceeds
     * to receive and concatenate each fragment until the complete message is reconstructed.
     *
     * @param hostname The hostname or IP address of the sender.
     * @param port     The port number on which the message is being received.
     * @return The reconstructed message if successful, or null if an error occurs during the reception
     *         or if the received message does not contain the expected fragment information.
     */


    public String receiveMessage(String hostname, int port){
        String message = receiveReliablePacket(hostname, port);

        if(!message.contains("Packets: ")){
            return null;
        }
        // Split the string by colon
        String[] parts = message.split(":");
        if (parts.length < 2) {
            return null;
        }
        int numberFragments = Integer.parseInt(parts[1].trim());

        String finalMessage = "";

        for(int i = 0; i < numberFragments; i++) {
            String splitMessage = receiveReliablePacket(hostname, port);
            finalMessage = String.join("", finalMessage, splitMessage);
        }

        return finalMessage;

    }

    /**
     * Receives a reliable packet from the specified host and port. The function first receives a packet and then sends
     * an acknowledgment.
     * @param hostname The hostname or IP address of the sender.
     * @param port     The port number on which the message is being received.
     * @return The received message if successful, or null if an error occurs during the reception.
     */
    private String receiveReliablePacket(String hostname, int port) {
        String received = null;
        try{
            InetAddress address = InetAddress.getByName(hostname);
            received = receivePacket();
            sendPacket("ACK",address,port);

        }catch(SocketTimeoutException e){
            System.err.println("Timeout reached: " + e.getMessage());
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }
        return received;
    }

    /**
     *Receives a datagram packet from udpSocket. Constructs a new String message by decoding the receivedData byte array.
     * Returns that received information;
     * @return a string with the received message
     * @throws IOException if an I/O error occurs.
     */
    private String receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        udpSocket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    /**
     * Closes the DatagramSocket
     */
    public void close() {
        udpSocket.close();
    }


    /**
     * The main method for the UDP client application. It prompts the user to enter server details,
     * a string, and a keyword. It then validates the input, including port number and hostname,
     * creates a UDP client, sends messages to the server, and receives and prints the server's responses.
     * The program is interrupted if input data is invalid or any of the sending and receiving processes fail.
     */

    public static void main(String[] args) {
        String hostname = null;
        String portString = null;
        String phrase = null;
        String keyword = null;
        InetAddress address = null;

        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.print("Enter server name or IP address: ");
            hostname = stdin.readLine();
            System.out.print("Enter port: ");
            portString = stdin.readLine();
            System.out.print("Enter string: ");
            phrase = stdin.readLine();
            System.out.print("Enter keyword: ");
            keyword = stdin.readLine();
            address = InetAddress.getByName(hostname);

        } catch (UnknownHostException hostNotFound){
            System.err.println("Host not found: " + hostNotFound.getMessage());
            System.exit(1);
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }

        //Input validation
        if(!inputDataValid(hostname,portString,phrase,keyword)) {
            System.err.println("Invalid input format. Terminating!");
            System.exit(1);
        }
        //Port validation
        int port = parseInt(portString);
        if(port < 1024 || port > 49151){
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }

        client_java_udp client = new client_java_udp(1000);

        if(client.sendMessage(phrase,address,port) == -1){
            client.close();
            System.exit(1);
        }

        if(client.sendMessage(keyword,address,port) == -1) {
            client.close();
            System.exit(1);
        }
        //Receive anonymized message
        String received = client.receiveMessage(hostname,port);
        if(received == null){
            client.close();
            System.exit(1);
        }
        System.out.println(received);

        //Receive number o times client will send "Socket Programming"
        received = client.receiveMessage(hostname,port);
        if(received == null){
            client.close();
            System.exit(1);
        }
        int repeat = Integer.parseInt(received);

        //Receive "Socket Programming" statement
        for(int i = 0; i < repeat; i++){
            received = client.receiveMessage(hostname,port);
            if(received == null){
                client.close();
                System.exit(1);
            }
            System.out.println(received);
        }
        client.close();
    }

    /**
     * Validates the input data by checking if none of the provided strings (hostname, port, phrase, keyword)
     * are empty. Returns true if all inputs are non-empty, indicating valid input; otherwise, returns false.
     * @param hostname The hostname or IP address.
     * @param port     The port number.
     * @param phrase   The string input.
     * @param keyword  The keyword input.
     * @return True if all inputs are non-empty; false otherwise.
     */

    private static boolean inputDataValid(String hostname,String port, String phrase, String keyword){
        return !hostname.isEmpty() && !port.isEmpty() && !phrase.isEmpty() && !keyword.isEmpty();
    }
}

package org.example.tcp;
import java.io.*;
import java.net.*;

/**
 * TCP/IP client that connects to a TCP server on a specific port.
 * It sends a message and the pretended keyword to be anonymized in that message.
 * No arguments are needed to execute the file. However, in order to have a successful result, server data
 * (IP or name, and port number) as well as valid message and keyword (not empty) must be passed by the user upon request.
 */

public class client_java_tcp {
    private Socket socket;
    private final String MESSAGE = "Could not connect to server. Terminating!";

    /**
     * How to use: call method using the server address and port. It requires a previous setup on the server side (The server
     * socket must have been created and be listening for connections on a pre-defined port).
     * Creates a client socket and connects it to the specified host reference (e.g. LocalHost, or destination ipAddress) and port.
     * Prints Message if either server address or port(host parameters) is not found or if it has any I/O issues.
     *
     * @param host Server name
     * @param port Port where server is listening to new connections
     */
    public client_java_tcp(String host, int port) {
        try {
            socket = new Socket(host, port);
        } catch (UnknownHostException e) {
            System.err.println(this.MESSAGE);
        } catch (IOException f) {
            System.out.println(this.MESSAGE);
        }
    }

    /**
     * How to use: insert phrase and keyword that is meant to be sent to the server.
     * Receives a piece of text (phrase) and keyword and performs the following steps:
     * (1.) and (2.) Creates input and output stream for the previously defined socket object.
     * (3.) Handles the data to be sent, separating the phrase from keyword input using a delimiter "--".
     * (4.) Controls the receival of messages from the server side.
     * (5.) Once the message "Transmission Complete" is sent from the server, the method pathway automatically
     * closes the input and output streams, and the host socket.
     *
     * @param phrase    Custom text to be anonymized
     * @param keyword   Keyword to be anonymized
     */
    public void sendData(String phrase, String keyword) {
        try {
            // 1.
            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader in = new BufferedReader(inputStreamReader);

            // 2.
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outputStream, true);

            // 3.
            String dataToSend = phrase + " -- " + keyword;
            out.println(dataToSend);

            // 4.
            String line;
            while ((line = in.readLine()) != null) {

                // 5.
                if (line.equals("Transmission Complete")) {
                    break;
                }
                System.out.println(line);
            }

            System.out.println("Closing client...");
            in.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            System.err.println("Could not fetch result. Terminating!");
        }
    }

    /**
     * This method guides the flow of the whole process as follows:
     * (1.) Creates a BufferedReader object to read incoming byte streams.
     * (2.) Performs the interaction with the user to receive necessary information (server name, port number, phrase
     * and the keyword)
     * (3.) Validates inputs are not blank.
     * (4.) Validates port.
     * (5.) If all parameters are valid and port is within range, proceeds to create a client socket and calls sendData.
     * Well-Known Ports: 0 – 1023
     * Registered Ports: 1024 – 49151. We want to ensure specified port is within this range
     * Dynamic and Private Ports: 49152 – 65535.
     *
     * @param args The arguments will be inserted by the user: serverAddress, port, phrase to be sent, keyword to be anoynimized
     */
    public static void main(String[] args) {

        // 1.
        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {

            // 2.
            // Host input
            System.out.print("Enter server name or IP address: ");
            String hostname = stdin.readLine();

            // Port input
            System.out.print("Enter port: ");
            String portString = stdin.readLine();

            // Phrase
            System.out.print("Enter string: ");
            String phrase = stdin.readLine();

            // Phrase
            System.out.print("Enter keyword: ");
            String keyword = stdin.readLine();

            // 3.
            // Validations
            if (!isInputEmpty(phrase, keyword, hostname, portString)) {
                System.err.println("Invalid input format. Terminating!");
                System.exit(1);
            } else {
                // 4.
                int port = Integer.parseInt(portString);
                if (port < 1024 || port > 49151) {
                    System.err.println("Invalid port number. Terminating!");
                    System.exit(1);

                    // 5.
                } else {
                    client_java_tcp clientSide = new client_java_tcp(hostname, port);
                    if (clientSide.socket != null) {
                        clientSide.sendData(phrase, keyword);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to send expression. Terminating!");
        }
    }

    /**
     * Verifies inputs are not blank.
     *
     * @param phrase    Custom text to be sent
     * @param keyword   Keyword to be anonymized
     * @param localhost Server address name
     * @param port      Server port number (string format)
     * @return Returns true or false
     */
    private static boolean isInputEmpty(String phrase, String keyword, String localhost, String port) {
        return !phrase.isEmpty() && !keyword.isEmpty() && !localhost.isEmpty() && !port.isEmpty();
    }
}

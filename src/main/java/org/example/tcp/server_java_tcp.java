package org.example.tcp;
import org.example.anonymizedService.anonymize_Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP/IP server that sends back an anonymized message, depending on the client's input.
 * In order to correctly run the program, the server port must be passed as a command-line argument.
 */

public class server_java_tcp {

    private ServerSocket socket;

    /**
     * Starts the server side, binding a ServerSocket to the specified port in the command line.
     * @param port Server port
     */
    public server_java_tcp(int port){
        socket = null;
        try {
            socket = new ServerSocket(port);
            System.out.println("Server listening on port: " + port);
        }catch (IOException e){
            System.err.println("Server could not provide a port. Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Waits for a connection from the client side. After establishing a connection, sends back the anonymized String
     * and the message "Socket Programming" as many times as the keyword is found in the received string.
     * (1.) Creates client socket while waiting for connection.
     * (2.) While active, the server listens for connections
     * (3.) When a connection is established it sends out a confirmation that includes the client's address and port.
     * (4.) Creates input and output stream mediums.
     * (5.) Receives input from client, and splits the text from the keyword based on a delimiter "--".
     * (6.) Replaces all instances of keyword with "X" and writes the new message in the socket.
     * Additionally, stores the number of times the word chosen was anonymized.
     * (7.) Writes in the sockets the message "Socket Programming" as many times as the keyword was found.
     * (8.) Once all responses are sent, the server closes the input and output streams and closes the clientSocket.
     * Server socket starts "listening" again on the same port.
     */
    public void connectionToServer(){
        // 1.
        Socket clientSocket;



        // 2.
        while(true){
            try{
                clientSocket = socket.accept();
        // 3.
                System.out.println("Connected to " + clientSocket.getInetAddress() + "  " + clientSocket.getPort());
        // 4.
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);

        // 5.
                String inputLine;
                while((inputLine = in.readLine()) != null) {

                    String[] parts = inputLine.split(" -- ", 2);
                    if (parts.length == 2) {

                        String originalString = parts[0];
                        String keyword = parts[1];
        // 6.
                        anonymize_Service anonimizeService = new anonymize_Service();
                        String[] modifiedData = anonimizeService.stringAnonymizer(originalString,keyword);

                        out.println(modifiedData[0]);
        // 7.
                        int repetitions = Integer.parseInt(modifiedData[1]);
                        stringRepeater(out,repetitions);
                        out.println("Transmission Complete");

                    } else {
                        out.println("Did not receive valid string from client. Terminating");
                    }
                }
        // 8.
                System.out.println("Client exiting...");
                in.close();
                out.close();
                clientSocket.close();

            }catch (IOException e){
                System.err.println("Result transmission failed. Terminating!");
            }
        }

    }

    /**
     * Writes in the socket the phrase "Socket Programming" as many times as the number of occurrences
     * of the word anonymized
     * @param out Object used for writing in the socket
     * @param numberOfReps Number of times the server will send the message "Socket Programming"
     */
    public void stringRepeater(PrintWriter out, int numberOfReps){
        for (int i = 0; i < numberOfReps; i++) {
            out.println("Socket Programming");
        }
    }

    /**
     * This method guides the flow of the whole server process as follows:
     * (1.) Verifies user inserts a valid port (within 1024 and 49151).
     * (2.) Creates server socket and starts listening on specified port
     * @param args Port to be inserted by the user
     */
    public static void main(String[] args) {
        // 1.
        if (args.length < 1){
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        if (port < 1024 || port > 49151) {
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }
        // 2.
            server_java_tcp serverSide = new server_java_tcp(port);
            serverSide.connectionToServer();



    }
}

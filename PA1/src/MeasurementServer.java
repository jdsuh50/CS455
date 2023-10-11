import java.net.*;
import java.io.*;

public class MeasurementServer {
    public static void main(String[] args) {
         
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
         
        try (
            ServerSocket serverSocket =
                new ServerSocket(Integer.parseInt(args[0]));
            Socket clientSocket = serverSocket.accept();     
            PrintWriter out =
                new PrintWriter(clientSocket.getOutputStream(), true);                   
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String msg;
            int numProbes = 0;
            int serverDelay = 0;
            int counter = 0;
            while ((msg = in.readLine()) != null) {
                String[] msgarr = (msg.replaceAll("\n", "").split(" "));
                String protocolPhase = msgarr[0];

                if (protocolPhase.equals("s")) {
                    if (msgarr.length == 5) {
                        out.println("200 OK: Ready");
                        numProbes = Integer.parseInt(msgarr[2]);
                        serverDelay = Integer.parseInt(msgarr[4]);
                        counter = 0;
                    } else {
                        System.err.println("404 ERROR: Invalid Connection Setup Message");
                        System.exit(1);
                    } 
                } else if (protocolPhase.equals("m")) {
                    if (msgarr.length == 3) {
                        counter++;
                        int sequenceNum = Integer.parseInt(msgarr[1]);
                        if (sequenceNum == counter && sequenceNum <= numProbes) {
                            try {
                                Thread.sleep(serverDelay);
                            } catch (InterruptedException e) {
                                System.out.println("Thread interrupted");
                            }
                            out.println(msg);
                        }
                    } else {
                        System.err.println("404 ERROR: Invalid Measurement Message");
                        System.exit(1);
                    } 
                } else if (protocolPhase.equals("t")) {
                    if (msg.equals("t")) {
                        out.println("200 OK: Closing Connection");
                        System.exit(0);
                    } else {
                        System.err.println("404 ERROR: Invalid Connection Termination Message");
                        System.exit(1);
                    } 
                }
            }

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}
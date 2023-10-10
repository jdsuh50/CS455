import java.net.*;
import java.io.*;
import java.time.*;

public class MeasurementClient {

    // helper function 
    public static String payloader(int messageSize) {
        String payload = "";
        for (int i = 0; i < messageSize; i++) {
            payload += "x";
        }
        return payload;
    }

    public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
            System.err.println(
                "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
 
        try (
            Socket socket = new Socket(hostName, portNumber);
            PrintWriter out =
                new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in))
        ) {
            
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                String[] usermsg = userInput.split(" ");
                String measurementType = usermsg[1];
                int numProbes = Integer.parseInt(usermsg[2]);
                int messageSize = Integer.parseInt(usermsg[3]);
                int serverDelay = Integer.parseInt(usermsg[4]);
                String servermsg;

                out.println(userInput);
                System.out.println(userInput);

            // if message is received, start measurement phase
                long totalTime = 0;
                servermsg = in.readLine();
                System.out.println(servermsg);
                if (servermsg.equals("200 OK:Ready")) {
                    for (int i = 1; i < numProbes + 1; i++) {
                        String payload = payloader(messageSize);
                        long sent = System.currentTimeMillis();
                        out.println("m " + i + " " + payload + "\n");
                        System.out.println("echo: " + in.readLine());
                        long received = System.currentTimeMillis();
                        totalTime += (received - sent);
                    }
                    long rtt = totalTime / numProbes;
                    System.out.println(rtt);
                }

                if (servermsg.equals("200 OK: Closing Connection")) {
                    System.exit(0);
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        } 
    }
}


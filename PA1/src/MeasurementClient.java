import java.net.*;
import java.io.*;

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
            userInput = stdIn.readLine();
            String[] usermsg = userInput.split(" ");
            String measurementType = usermsg[1];
            int numProbes = Integer.parseInt(usermsg[2]);
            int messageSize = Integer.parseInt(usermsg[3]);
            String servermsg;

            out.println(userInput);
            System.out.println(userInput);

            // if message is received, start measurement phase
            if (measurementType.equals("rtt")) {

                double totalTime = 0;
                servermsg = in.readLine();
                System.out.println(servermsg);
                if (servermsg.equals("200 OK: Ready")) {
                    for (int i = 1; i < numProbes + 1; i++) {
                        String payload = payloader(messageSize);
                        double sent = System.currentTimeMillis();
                        out.println("m " + i + " " + payload);
                        System.out.println("echo: " + in.readLine());
                        double received = System.currentTimeMillis();
                        double timer = (received - sent);
                        System.out.println(timer);
                        totalTime += timer;
                    }
                }
                double rtt = (totalTime / numProbes);
                System.out.println("Avg rtt: " + rtt);

            } else if (measurementType.equals("tput")) {
                double total = 0;
                servermsg = in.readLine();
                System.out.println(servermsg);
                if (servermsg.equals("200 OK: Ready")) {
                    for (int i = 1; i < numProbes + 1; i++) {
                        String payload = payloader(messageSize);
                        double sent = System.currentTimeMillis();
                        out.println("m " + i + " " + payload);
                        System.out.println("echo: " + in.readLine());
                        double received = System.currentTimeMillis();
                        double timer = (received - sent);
                        double tx = (messageSize/timer);
                        System.out.println(tx);
                        total += tx;
                    }
                    double throughput = (total / numProbes);
                    System.out.println(throughput + " kbps");
                }

            }


            out.println("t");
            servermsg = in.readLine();

            if (servermsg.equals("200 OK: Closing Connection")) {
                socket.close();
            } else {
                System.exit(1);
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


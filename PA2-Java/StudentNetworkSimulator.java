import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator {
  /*
   * Predefined Constants (static member variables):
   *
   * int MAXDATASIZE : the maximum size of the Message data and
   * Packet payload
   *
   * int A : a predefined integer that represents entity A
   * int B : a predefined integer that represents entity B
   *
   * Predefined Member Methods:
   *
   * void stopTimer(int entity):
   * Stops the timer running at "entity" [A or B]
   * void startTimer(int entity, double increment):
   * Starts a timer running at "entity" [A or B], which will expire in
   * "increment" time units, causing the interrupt handler to be
   * called. You should only call this with A.
   * void toLayer3(int callingEntity, Packet p)
   * Puts the packet "p" into the network from "callingEntity" [A or B]
   * void toLayer5(String dataSent)
   * Passes "dataSent" up to layer 5
   * double getTime()
   * Returns the current time in the simulator. Might be useful for
   * debugging.
   * int getTraceLevel()
   * Returns TraceLevel
   * void printEventList()
   * Prints the current event list to stdout. Might be useful for
   * debugging, but probably not.
   *
   *
   * Predefined Classes:
   *
   * Message: Used to encapsulate a message coming from layer 5
   * Constructor:
   * Message(String inputData):
   * creates a new Message containing "inputData"
   * Methods:
   * boolean setData(String inputData):
   * sets an existing Message's data to "inputData"
   * returns true on success, false otherwise
   * String getData():
   * returns the data contained in the message
   * Packet: Used to encapsulate a packet
   * Constructors:
   * Packet (Packet p):
   * creates a new Packet that is a copy of "p"
   * Packet (int seq, int ack, int check, String newPayload)
   * creates a new Packet with a sequence field of "seq", an
   * ack field of "ack", a checksum field of "check", and a
   * payload of "newPayload"
   * Packet (int seq, int ack, int check)
   * chreate a new Packet with a sequence field of "seq", an
   * ack field of "ack", a checksum field of "check", and
   * an empty payload
   * Methods:
   * boolean setSeqnum(int n)
   * sets the Packet's sequence field to "n"
   * returns true on success, false otherwise
   * boolean setAcknum(int n)
   * sets the Packet's ack field to "n"
   * returns true on success, false otherwise
   * boolean setChecksum(int n)
   * sets the Packet's checksum to "n"
   * returns true on success, false otherwise
   * boolean setPayload(String newPayload)
   * sets the Packet's payload to "newPayload"
   * returns true on success, false otherwise
   * int getSeqnum()
   * returns the contents of the Packet's sequence field
   * int getAcknum()
   * returns the contents of the Packet's ack field
   * int getChecksum()
   * returns the checksum of the Packet
   * int getPayload()
   * returns the Packet's payload
   *
   */

  /*
   * Please use the following variables in your routines.
   * int WindowSize : the window size
   * double RxmtInterval : the retransmission timeout
   * int LimitSeqNo : when sequence number reaches this value, it wraps around
   */

  public static final int FirstSeqNo = 0;
  private int WindowSize;
  private double RxmtInterval;
  private int LimitSeqNo;

  // Add any necessary class variables here. Remember, you cannot use
  // these variables to send messages error free! They can only hold
  // state information for A or B.
  // Also add any necessary methods (e.g. checksum of a String)

  // This is the constructor. Don't touch!
  public StudentNetworkSimulator(int numMessages,
      double loss,
      double corrupt,
      double avgDelay,
      int trace,
      int seed,
      int winsize,
      double delay) {
    super(numMessages, loss, corrupt, avgDelay, trace, seed);
    WindowSize = winsize;
    LimitSeqNo = (winsize * 2) - 1; // set appropriately; assumes SR here!
    RxmtInterval = delay;
  }

  private int seqNo;
  private Packet[] sendBuffer;
  private Packet[] recBuffer;
  private int smin;
  private int smax;

  private int lastAck = -1;
  private int expected;
  private int rmin;
  private int rmax;


  private int numSent;
  private int retransmits;
  private int numCorrupted;
  private int numACK;
  private int numtoL5;
  // This routine will be called whenever the upper layer at the sender [A]
  // has a message to send. It is the job of your protocol to insure that
  // the data in such a message is delivered in-order, and correctly, to
  // the receiving upper layer.

  protected int checkSum(Packet pck) {
    int seq = pck.getSeqnum();
    int ack = pck.getAcknum();
    String data = pck.getPayload();
    // calculate cheacksum of this packet, add up all the data with seq number and
    // ACK
    int checksum = seq + ack;
    for (int i = 0; i < data.length(); i++) {
      checksum += (int) data.charAt(i);
    }
    return checksum;
  }

  protected boolean isCorrupted(Packet p) {
    int a = checkSum(p);
    int b = p.getChecksum();
    return (a != b);
  }

  public int next(int num) {
    int nextNum = num + 1;
    if (nextNum > LimitSeqNo) {
      nextNum = FirstSeqNo;
    }
    return nextNum;
  }

  protected void aOutput(Message message) {
    String payload = message.getData();
    Packet packet = new Packet(seqNo, 0, -1, payload);
    int checksum = checkSum(packet);
    packet.setChecksum(checksum);
    seqNo = next(seqNo);

    if (sendBuffer[packet.getSeqnum()] != null) {
      System.out.println("Buffer is already full");
      System.exit(1);
    }

    sendBuffer[packet.getSeqnum()] = packet;
    System.out.println("Sending packet to B:" + packet.toString());
    toLayer3(A, packet);
    numSent++;

    if (smin == packet.getSeqnum()) {
      startTimer(A, RxmtInterval);
    }
  }

  // This routine will be called whenever a packet sent from the B-side
  // (i.e. as a result of a toLayer3() being done by a B-side procedure)
  // arrives at the A-side. "packet" is the (possibly corrupted) packet
  // sent from the B-side.
  protected void aInput(Packet packet) {
    if (isCorrupted(packet)) {
      System.out.println("A recieves corrupted ACK! Ignoring data." + packet.toString());
      numCorrupted++;
      return;
    }

    System.out.println("A received ACK for packet #" + packet.getSeqnum());
    if (packet.getAcknum() <= packet.getSeqnum()) {
      toLayer3(A, sendBuffer[packet.getAcknum()]);
      startTimer(A, RxmtInterval);
    }
    if (packet.getSeqnum() == smin) {
      stopTimer(A);
      smin = next(smin);
      smax = next(smax);
    }
    sendBuffer[packet.getSeqnum()] = null;
    
  }

  // This routine will be called when A's timer expires (thus generating a
  // timer interrupt). You'll probably want to use this routine to control
  // the retransmission of packets. See startTimer() and stopTimer(), above,
  // for how the timer is started and stopped.
  protected void aTimerInterrupt() {
    if (sendBuffer[smin] != null) {
      System.out.println("Retransmitting Packet" + sendBuffer[smin].getSeqnum());
      toLayer3(A, sendBuffer[smin]);
    } else {
      smin = next(smin);
      smax = next(smax);
    }
  }

  // This routine will be called once, before any of your other A-side
  // routines are called. It can be used to do any required
  // initialization (e.g. of member variables you add to control the state
  // of entity A).
  protected void aInit() {
    seqNo = FirstSeqNo;
    sendBuffer = new Packet[LimitSeqNo + 1];
    smin = FirstSeqNo;
    smax = smin + WindowSize - 1;
  }

  // This routine will be called whenever a packet sent from the B-side
  // (i.e. as a result of a toLayer3() being done by an A-side procedure)
  // arrives at the B-side. "packet" is the (possibly corrupted) packet
  // sent from the A-side.
  protected void bInput(Packet packet) {
    if (isCorrupted(packet)) {
      System.out.println("B received corrupted packet - ignoring data");
      numCorrupted++;
      return;
    }
    recBuffer[packet.getSeqnum()] = packet;
    if (packet.getSeqnum() == expected) {
      expected = next(expected);
      
      while (recBuffer[rmin] != null) {
        toLayer5(packet.getPayload());
        numtoL5++;
        recBuffer[rmin] = null;
        rmin = next(rmin);
        rmax = next(rmax);
      }
    } else {
      System.out.println("B received a packet out of order - ignoring data. Expecting #" + expected + " received #"
          + packet.getSeqnum());
    }
    Packet ack = new Packet(packet.getSeqnum(), expected, -1, "");
    int check = checkSum(ack);
    ack.setChecksum(check);

    System.out.println("Sending ACK: " + ack.toString());
    toLayer3(B, ack);
    numACK++;
  }

  // This routine will be called once, before any of your other B-side
  // routines are called. It can be used to do any required
  // initialization (e.g. of member variables you add to control the state
  // of entity B).
  protected void bInit() {
    expected = FirstSeqNo;
    recBuffer = new Packet[LimitSeqNo + 1];
    rmin = FirstSeqNo;
    rmax = rmin + WindowSize - 1;
    
  }

  // Use to print final statistics
  protected void Simulation_done() {
    // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO
    // NOT CHANGE THE FORMAT OF PRINTED OUTPUT
    System.out.println("\n\n===============STATISTICS=======================");
    System.out.println("Number of original packets transmitted by A:" + numSent);
    System.out.println("Number of retransmissions by A:" + retransmits);
    System.out.println("Number of data packets delivered to layer 5 at B:" + numtoL5);
    System.out.println("Number of ACK packets sent by B:" + numACK);
    System.out.println("Number of corrupted packets:" + numCorrupted);
    System.out.println("Ratio of lost packets:" + "<YourVariableHere>");
    System.out.println("Ratio of corrupted packets:" + numCorrupted / numSent);
    System.out.println("Average RTT:");
    System.out.println("Average communication time:");
    System.out.println("==================================================");

    // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
    System.out.println("\nEXTRA:");
    // EXAMPLE GIVEN BELOW
    // System.out.println("Example statistic you want to check e.g. number of ACK
    // packets received by A :" + "<YourVariableHere>");
  }

}

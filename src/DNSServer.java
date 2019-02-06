package MSD;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class DNSServer {

  /**
   * The driving method of the server.
   * @throws IOException
   */
  public static void openAndRunServerSocket() throws IOException {
    DatagramSocket dataSocket = new DatagramSocket(8053);
    byte[] buf = new byte[512];
    DatagramPacket packet = new DatagramPacket(buf, buf.length);
    while(dataSocket.isBound()) {
      dataSocket.receive(packet);
      DNSMessage message = DNSMessage.decodeMessage(buf);
      System.out.println("Request from client:");
      System.out.println(message.toString());

      // checking if message is a query.
      if (message.header.qr == 0){
        byte [] reply = generateReply(message);
        sendReply(dataSocket, packet, reply);
      } else {
        System.out.println("Non-query message received");
      }
    }
  }

  /**
   * Will check if the the requested message/ answer is contained within DSNCache.
   * If not it will recursively request the answer from Google's DNS resolver
   * and then store the message/ answer pair into the Cache hash table.
   * @param message - The DSN message contain the DNS questions.
   * @return A byte array contain the answer message for the client.
   * @throws IOException
   */
  private static byte[] generateReply(final DNSMessage message) throws IOException {
    byte[] reply;
    for (int i = 0; i < message.questions.length; i++){
      if (!DNSCache.contains(message.questions[i])){
        reply = getAnswerFromGoogle(message);
        DNSMessage replyMessage = DNSMessage.decodeMessage(reply);
        System.out.println("Sent response from Google:");
        System.out.println(replyMessage.toString());
        // Error handling
        if (replyMessage.header.rCode != 0){
          System.out.println("RCode: " + replyMessage.header.rCode +
              ". Response from Google sent to client.");
          return reply;
        }
        // Dig does not support sending multiple questions in one request.
        // This feature will be built when i have a tool to test it properly.
        DNSCache.placeRecord(replyMessage.questions[0], replyMessage.answers[0]);
        return reply;
      } else {
        // fetch the answer, create the answer and send it back to the client.
        System.out.println("Sent from cache");
        DNSMessage ret = DNSMessage.buildResponse(message);
        System.out.println(ret.toString());
        return ret.toBytes();
      }
    }
    System.out.println("Reply Generation Error");
    return null;
  }

  /**
   * Sends a requested DNS message to Google's DNS resolver.
   * @param message - The DSN message contain the DNS questions.
   * @return - A byte array contain the answer message for the client.
   * @throws IOException
   */
  private static byte[] getAnswerFromGoogle(final DNSMessage message) throws IOException {
    DatagramSocket dataSocket = new DatagramSocket();
    byte[] addr = new byte[] {8,8,8,8};
    InetAddress address = InetAddress.getByAddress(addr);
    DatagramPacket sendPacket = new DatagramPacket(message.rawData, message.rawData.length, address, 53);
    dataSocket.send(sendPacket);
    byte[] receiveData = new byte[512];
    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    dataSocket.receive(receivePacket);
    dataSocket.close();
    return receiveData;
  }

  /**
   *  Sends a byte array reply to the client
   * @param socket - The server's DatagramSocket.
   * @param packet - The client's received DatagramPacket.
   * @param reply - The response to send back to the client.
   * @throws IOException
   */
  private static void sendReply(final DatagramSocket socket, final DatagramPacket packet, final byte[] reply) throws IOException {
    InetAddress address = packet.getAddress();
    int port = packet.getPort();
    DatagramPacket returnPacket = new DatagramPacket(reply, reply.length, address, port);
    socket.send(returnPacket);
  }
}


package MSD;

import java.io.IOException;

public class Main {

  public static void main(String[] args) {
    try {
      DNSServer.openAndRunServerSocket();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

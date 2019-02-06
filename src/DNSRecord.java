package MSD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;

public class DNSRecord {

  // The URL who’s IP address this response contains.
  // This uses a compressed format.
  String[] name;
  // A two octet code which specifies the type of the query.
  //x.0001 (1 )An A record for the domain name
  //x.0002 (2) A NS record for the domain name
  //x.0005 (5) A CNAME record for the domain name
  //x.0006 (6) A SOA record for the domain name
  //x.000B (11)	A WKS record(s) for the domain name
  //x.000C (12)	A PTR record(s) for the domain name
  //x.000F (15)	A MX record for the domain name
  //x.0021 (33)	A SRV record(s) for the domain name
  //x.001C (28)	An AAAA record(s) for the domain name
  short type;
  // A two octet code that specifies the class of the query.
  // Most likely just 01  for IN, internet
  // 4096:unassigned
  short class_;
  // Specifying the time to live for this Response, measured in seconds.
  // Before this time interval runs out, the result can be cached. After, it should be discarded.
  int ttl;
  // The byte length of the following RDDATA section;
  short rdLength;
  byte[] rdData;
  private Calendar expirationTime;

  /**
   * Acts as the constructor ofr a DNSRecord object
   * @param input - The input stream used to define member variables.
   * @param message - The parent DNSMessage.
   * @return - A DNSRecord contain the defined member variables.
   * @throws IOException
   */
  static DNSRecord decodeRecord(final InputStream input, final DNSMessage message) throws IOException {
    DNSRecord ret = new DNSRecord();
    int firstByte = input.read();
    if ((firstByte >> 6) == 3) {
      short byteNum = (short) (((firstByte << 10) | input.read()));
      ret.name = message.readDomainName(byteNum);
    } else if (firstByte == 0) {
      ret.name = new String[2];
      ret.name[0] = String.valueOf(firstByte);
    } else {
      ret.name = message.readDomainName(input);
    }
    ret.type = DNSMessage.getShort(input);
    ret.class_ = DNSMessage.getShort(input);
    ret.ttl = getInt(input);
    ret.expirationTime = Calendar.getInstance();
    ret.expirationTime.add(Calendar.SECOND, ret.ttl);
    ret.rdLength = DNSMessage.getShort(input);
    ret.rdData = getRdData(input, ret.rdLength);
    return ret;
  }

  /**
   * Reads in an int, four bytes, from and inputStream.
   * @param buf
   * @return The four read in bytes merged into an int.
   * @throws IOException
   */
  private static int getInt (final InputStream buf) throws IOException {
    int a = buf.read();
    int b = buf.read();
    int c = buf.read();
    int d = buf.read();
    return ((a << 24 | b << 16) | (c << 8 | d));
  }

  /**
   * Extracts the rdData member variable from an input stream
   * @param buf - The input stream used to define member variables.
   * @param arraySize
   * @return - A byte array of rdData.
   * @throws IOException
   */
  private static byte[] getRdData (final InputStream buf, final short arraySize) throws IOException {
    byte[] ret = new byte[arraySize];
    for (int i = 0; i < arraySize; ++i){
      ret[i] = (byte) buf.read();
    }
    return ret;
  }

  @Override
  public String toString() {
    String ret = new String();
    for (int i = 0; i < name.length; i++)
      ret += "Name" + i + ":" + name[i] + " ";
    ret = "Type:" + type + " Class:" + class_ + " TTL:" + ttl + " RDLength:" + rdLength + " RDData:";
    for (int i = 0; i < rdLength; i++)
      ret += rdData[i] + ".";
    return ret;
  }

  /**
   *  Converts an int into four bytes.
   * @param i
   * @return An array of four bytes. MSB is contain in index 0.
   */
  private static byte[] intToBytes (final int i){
    byte[] ret = new byte[4];
    ret[0] = (byte) (i >> 24 & 0xff);
    ret[1] = (byte) (i >> 16 & 0xff);
    ret[2] = (byte) (i >> 8 & 0xff);
    ret[3] = (byte) (i & 0xff);
    return ret;
  }

  /**
   * Writes the member variables into a ByteArrayOutputStream
   * @param os - The ByteArrayOutputStream to write out to.
   * @param domainNameLocations - A hash map of domain/ array location pairs.
   * @throws IOException
   */
  void writeBytes(final ByteArrayOutputStream os, final HashMap<String, Integer> domainNameLocations)
      throws IOException {
    DNSMessage.writeDomainName(os, domainNameLocations, name);
    os.write(DNSMessage.shortToBytes(type));
    os.write(DNSMessage.shortToBytes(class_));
    os.write(intToBytes(ttl));
    os.write(DNSMessage.shortToBytes(rdLength));
    for (int i = 0; i < rdLength; i++)
      os.write(rdData[i]);
  }

  /**
   * Used within the DNS Cache to determine if the cached message is valid.
   * @return True if the message is still valid, else false.
   */
  boolean timestampValid(){
    Calendar currentTime = Calendar.getInstance();
    return currentTime.before(expirationTime);
  }
}

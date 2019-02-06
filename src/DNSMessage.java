package MSD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class DNSMessage {
   byte[] rawData;
   DNSHeader header = new DNSHeader();
   DNSQuestion[] questions = null;
   DNSRecord[] answers = null;
   DNSRecord[] authorityRecords = null;
   DNSRecord[] additionalRecords = null;
   HashMap<String,Integer> domainNameLocations = new HashMap<>();


  /**
   * Acts as the constructor for an DNSMessage object as the object is created
   * When a byte array is read in.
   * @param bytes - A byte array taken from the client's input stream.
   * @return - a DNSMessage object
   * @throws IOException
   */
  static DNSMessage decodeMessage(final byte[] bytes) throws IOException {
    DNSMessage ret = new DNSMessage();
    ret.rawData = bytes;
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    ret.header = DNSHeader.decodeHeader(input);
    ret.questions = new DNSQuestion[ret.header.qdCount];
    for (int i = 0; i < ret.questions.length ; i++)
      ret.questions[i] = DNSQuestion.decodeQuestion(input, ret);
    ret.answers = new DNSRecord[ret.header.anCount];
    for (int i = 0; i < ret.header.anCount; i++)
      ret.answers[i] = DNSRecord.decodeRecord(input, ret);
    ret.authorityRecords = new DNSRecord[ret.header.nsCount];
    for (int i = 0; i < ret.header.nsCount; i++)
      ret.authorityRecords[i] = DNSRecord.decodeRecord(input, ret);
    ret.additionalRecords = new DNSRecord[ret.header.arCount];
      for (int i = 0; i < ret.header.arCount; i++)
        ret.additionalRecords[i] = DNSRecord.decodeRecord(input, ret);
    return ret;
  }

  /**
   * Used to read in the domain name from the client's byte array input stream.
   * This should be called when reading a requested domain for the first time.
   * @param input - An input stream of the client's request.
   * @return - A string array containing the domain of the requested record
   * broken parnsed by the "." in the URL.
   * @throws IOException
   */
  String[] readDomainName(final InputStream input) throws IOException {
    ArrayList<char[]> charArr = new ArrayList<>();
    int i = 0;
    while (true) {
      int newByte = input.read();
      if (newByte == 0) {
        String[] ret = new String[charArr.size()];
        for (int j = 0; j < ret.length; j++)
          ret[j] = new String(charArr.get(j));
        return ret;
      }
      char[] string = new char[newByte];
      charArr.add(string);
      for (int j = 0; j < charArr.get(i).length; j++)
        charArr.get(i)[j] = (char) input.read();
      i++;
    }
  }

  /**
   * Used to read the domain name from a previous location in the byte array.
   * @param firstByte - The index of the first byte of the domain name  inside
   * of the client's requested message byte array.
   * @return
   * @throws IOException
   */
  String[] readDomainName(final int firstByte) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(rawData);
    input.reset();
    input.skip(firstByte);
    return readDomainName(input);
  }

  @Override
  public String toString() {
    String ret = new String();
    ret += header.toString() + '\n';
    for (int i = 0; i < questions.length; i++)
      ret += questions[i].toString() + '\n';
    if (answers != null)
      ret += getStringsFromDNSRecord(answers);
    if (authorityRecords != null)
     ret += getStringsFromDNSRecord(authorityRecords);
    if (additionalRecords != null)
     ret += getStringsFromDNSRecord(additionalRecords);
    return ret;
  }

  private String getStringsFromDNSRecord(final DNSRecord[] array) {
    String ret = new String();
    for (int i = 0; i < array.length; i++)
      ret += array[i].toString() + '\n';
    return ret;
  }

  /**
   * Constructs the response DNS message for the client's requested message.
   * @param request - The client's request message
   * @return
   */
  static DNSMessage buildResponse(final DNSMessage request){
    DNSMessage ret = new DNSMessage();
    DNSHeader.buildResponseHeader(request, ret);
    ret.questions = new DNSQuestion[ret.header.qdCount];
    ret.answers = new DNSRecord[ret.header.anCount];
    ret.authorityRecords = new DNSRecord[ret.header.nsCount];
    ret.additionalRecords = new DNSRecord[ret.header.arCount];
    ret.questions = request.questions;
    ret.answers[0] = DNSCache.getRecord(request.questions[0]);
    ret.answers[0].name[0].equals(request.questions[0].qName);
    ret.answers[0].type = request.questions[0].qType;
    ret.answers[0].class_ = request.questions[0].qClass;
    ret.answers[0].ttl = 20532;
    ret.authorityRecords = request.authorityRecords;
    ret.additionalRecords = request.additionalRecords;
    return ret;
  }

  byte[] toBytes() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    header.writeBytes(bos);
    for (int i = 0; i < questions.length; i++)
      questions[i].writeBytes(bos, domainNameLocations);
    for (int i = 0; i < answers.length; i++)
      answers[i].writeBytes(bos, domainNameLocations);
    for (int i = 0; i < authorityRecords.length; i++)
      authorityRecords[i].writeBytes(bos, domainNameLocations);
    if (additionalRecords[0] != null) {
      for (int i = 0; i < additionalRecords.length; i++)
        additionalRecords[i].writeBytes(bos, domainNameLocations);
    }
    return bos.toByteArray();
  }

  /**
   * Writes the domain name in an output stream. To be used while constructing
   * teh reply message.
   * @param os - The ByteArrayOutputStream constructing the reply byte array.
   * @param domainLocations - A hash map of pairs of domains and the location
   * of that domain within the byte array.
   * @param domainPieces - The parts of the domain parsed by ".".
   * @throws IOException
   */
  static void writeDomainName(final ByteArrayOutputStream os, final HashMap<String,Integer> domainLocations, final String[] domainPieces)
      throws IOException {
    if(domainPieces[0].equals("0")){
      os.write(0);
      return;
    }
    String domain = octetsToString(domainPieces);
    if (domainLocations.containsKey(domain)){
      short location = domainLocations.get(domain).shortValue();
      short mask = (short) 49152;
      short firstBytes =(short) (mask | location);
      os.write(shortToBytes(firstBytes));
    } else {
      domainLocations.put(domain, os.size());
      for (int i = 0; i < domainPieces.length; i++){
        os.write(domainPieces[i].length());
        os.write(domainPieces[i].getBytes());
      }
      os.write(0);
    }
  }

  /**
   * Merges an array of string into a single string.
   * @param octets
   * @return A string of the combine octects.
   */
  static String octetsToString(final String[] octets){
    String ret = null;
    for (int i = 0; i < octets.length; i++)
      ret += octets[i];
    return ret;
  }

  /**
   *  Converts a short into two bytes.
   * @param s
   * @return an array of two bytes. MSB is contain in index 0.
   */
  public static byte[] shortToBytes (final short s){
    byte[] ret = new byte[2];
    ret[0] = (byte) (s >> 8 & 0xff);
    ret[1] = (byte) (s & 0xff);
    return ret;
  }

  /**
   * Reads in a short from and inputStream.
   * @param buf
   * @return The two read in bytes merged into a byte.
   * @throws IOException
   */
  public static short getShort (final InputStream buf) throws IOException {
    return (short) ((buf.read() << 8) | buf.read());
  }
}

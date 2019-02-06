package MSD;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DNSHeader {
  // Message id.
  short id;
  // Specifies whether this message is a query (0), or a response (1).
  byte qr;
  // A four bit field that specifies kind of query in this message.
  // 0  Query
  // 1  IQuery
  // 2  Status
  // 3  Available for assignment
  // 4  Notify
  // 5  Update
  byte opCode;
  // Authoritative Answer - this bit is valid in responses, and specifies that the responding name server is an
  // authority for the domain name in question section.
  byte aa;
  // Truncation - specifies that this message was truncated
  byte tc;
  // Recursion Desired - this bit may be set in a query and is copied into the response.
  // If RD is set, it directs the name server to pursue the query recursively.
  // Recursive query support is optional.
  byte rd;
  // Recursion Available - this be is set or cleared in a
  // response, and denotes whether recursive query support is
  // available in the name server.
  byte ra;
  // 0, reserved for future use.
  byte z = 0;
  // Authentic data - Bit indicates in a response that the data included has been verified by the server providing it.
  byte ad;
  // Checking disabled - bit indicates in a query that non-verified data is acceptable to the resolver sending the query.
  byte cd;
  // Response code - this 4 bit field is set as part of responses.
  // 0  No error condition
  // 1  Format error - The name server was unable to interpret the query.
  // 2  Server failure - The name server was unable to process this query due to a
  //    problem with the name server.
  // 3  Name Error - Meaningful only for responses from an authoritative name
  //    server, this code signifies that the domain name referenced in the query does
  //    not exist.
  // 4  Not Implemented - The name server does not support the requested kind of query.
  // 5  Refused - The name server refuses to perform the specified operation for policy reasons.
  //    For example, a name server may not wish to provide the information to the particular requester,
  //     or a name server may not wish to perform a particular operation (e.g., zone transfer) for particular data.
  byte rCode;
  // The number of entries in the question section.
  short qdCount;
  // The number of of resource records in the answer section.
  short anCount;
  // The number of name server resource records in the authority records section.
  short nsCount;
  // The number of resource records in the additional records section.
  short arCount;


  /**
   *
   * @param buf
   * @return
   * @throws IOException
   */
  static DNSHeader decodeHeader(final InputStream buf) throws IOException {
    DNSHeader ret = new DNSHeader();
    ret.id = DNSMessage.getShort(buf);
    int byte3 = buf.read();
    ret.rd = getBit(byte3, 0);
    ret.tc = getBit(byte3, 1);
    ret.aa = getBit(byte3, 2);
    getOpCode(byte3 , ret);
    ret.qr = getBit(byte3, 7);
    int byte4 = buf.read();
    getRCode(byte4, ret);
    ret.cd = getBit(byte4, 4);
    ret.ad = getBit(byte4, 5);
    ret.ra = getBit(byte4, 7);
    ret.qdCount = DNSMessage.getShort(buf);
    ret.anCount = DNSMessage.getShort(buf);
    ret.nsCount= DNSMessage.getShort(buf);
    ret.arCount = DNSMessage.getShort(buf);
  return ret;
  }

  /**
   * Extracts a single bit from a byte determined by the size of a right shift.
   * @param byte_ - The byte to extract a bit from.
   * @param rShift - The size of the right shift.
   * @return A byte with a value of 0 or 1.
   * @throws IOException
   */
  private static byte getBit (final int byte_, final int rShift) throws IOException {
    return (byte) ((byte_ >> rShift) & 0x1);
  }

  /**
   * Defines the member variable opCode.
   * @param byte3 - The third byte in teh header.
   * @param header - The DNSHeader object being defined.
   * @throws IOException
   */
  private static void getOpCode (final int byte3, final DNSHeader header) throws IOException {
    header.opCode = (byte) ((byte3 >> 3) & 0xF);
  }

  /**
   * Defines the member variable rCode.
   * @param byte4 - The fourth byte in teh header.
   * @param header - The DNSHeader object being defined.
   * @throws IOException
   */
  private static void getRCode (final int byte4, final DNSHeader header) throws IOException {
    header.rCode = (byte) (byte4 & 0xF);
  }

  @Override
  public String toString(){
    String ret = "ID:" + id + " QR:" + qr + " OPCode:" + opCode + " AA:" + aa + " TC:" + tc +
        " RD:" + rd + " RA:" + ra + " Z:" + z + " AD:" + ad + " CD:" + cd + " RCode:" + rCode +
        " QCCount:" + qdCount + " ANCount:" + anCount + " NSCount:" + nsCount + " ARCount:" + arCount;
    return ret;
  }

  /**
   * Builds the response message header
   * @param request - The client request message.
   * @param response - The build response message.
   */
  static void buildResponseHeader(final DNSMessage request, DNSMessage response){
    response.header = request.header;
    response.header.qr = 1;
    response.header.ra = 1;
    response.header.anCount = 1;
  }

  /**
   * Write the member variable s to an OutputStream.
   * @param os
   * @throws IOException
   */
  void writeBytes(final OutputStream os) throws IOException {
    os.write(DNSMessage.shortToBytes(id));
    os.write(generate3rdByte());
    os.write(generate4thByte());
    os.write(DNSMessage.shortToBytes(qdCount));
    os.write(DNSMessage.shortToBytes(anCount));
    os.write(DNSMessage.shortToBytes(nsCount));
    os.write(DNSMessage.shortToBytes(arCount));
  }

  /**
   * Generates the third byte of the of the header for response messages.
   * @return a short consisting of the third byte.
   */
  private short generate3rdByte(){
    return (short) ((qr << 7 | opCode << 6) | (aa << 2 | aa << 1) | rd);
  }

  /**
   * Generates the fourth byte of the of the header for response messages.
   * @return a short consisting of the fourth byte.
   */
  private short generate4thByte(){
    return (short) ((ra << 7 | z << 6) | (ad << 5 | cd << 4) | rCode);
  }
}

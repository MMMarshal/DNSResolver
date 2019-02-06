package MSD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class DNSQuestion {
  // Domain questions
  String[] qName;
  // A two octet code which specifies the type of the query.
  //x'0001 (1)	An A record for the domain name
  //x'0002 (2)	A NS record( for the domain name
  //x'0005 (5)	A CNAME record for the domain name
  //x'0006 (6)	A SOA record for the domain name
  //x'000B (11)	A WKS record(s) for the domain name
  //x'000C (12)	A PTR record(s) for the domain name
  //x'000F (15)	A MX record for the domain name
  //x'0021 (33)	A SRV record(s) for the domain name
  //x'001C (28)	An AAAA record(s) for the domain name
  short qType;
  // A  two octet code that specifies the class of the query.
  // Most likely just 01  for IN, internet
  short qClass;


  /**
   * Acts as the constructor ofr a DNSQuestion object
   * @param input - The input stream used to define member variables.
   * @param message - The parent DNSMessage.
   * @return - A DNSQuestion contain the defined member variables.
   * @throws IOException
   */
  static DNSQuestion decodeQuestion(final InputStream input, final DNSMessage message)
      throws IOException {
    DNSQuestion ret = new DNSQuestion();
    ret.qName = message.readDomainName(input);
    ret.qType = DNSMessage.getShort(input);
    ret.qClass = DNSMessage.getShort(input);
    return ret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DNSQuestion that = (DNSQuestion) o;
    return qType == that.qType &&
        qClass == that.qClass &&
        Arrays.equals(qName, that.qName);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(qType, qClass);
    result = 31 * result + Arrays.hashCode(qName);
    return result;
  }

  @Override
  public String toString() {
    String ret = new String();
    for (int i = 0; i < qName.length; i++)
      ret += "Name" + i + ":" + qName[i] + " ";
    ret += "QType:" + qType + " QClass:" + qClass;
    return ret;
  }

  /**
   * Writes the member variables into a ByteArrayOutputStream
   * @param os - The ByteArrayOutputStream to write out to.
   * @param domainNameLocations - A hash map of domain/ array location pairs.
   * @throws IOException
   */
  void writeBytes(final ByteArrayOutputStream os, final HashMap<String,Integer> domainNameLocations)
      throws IOException {
    DNSMessage.writeDomainName(os, domainNameLocations, qName);
    os.write(DNSMessage.shortToBytes(qType));
    os.write(DNSMessage.shortToBytes(qClass));
  }
}

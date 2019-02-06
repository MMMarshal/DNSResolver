package MSD;

import java.util.HashMap;

//A cache to store client requests and answers
public class DNSCache {
  private static HashMap<DNSQuestion, DNSRecord> cache = new HashMap<>();

  static boolean contains (final DNSQuestion question){
    if (cache.containsKey(question)){
      if (cache.get(question).timestampValid())
        return true;
      else
        cache.remove(question);
    }
    return false;
  }

  static DNSRecord getRecord (final DNSQuestion question){
    return cache.get(question);
  }

  static void placeRecord (final DNSQuestion question, final DNSRecord record){
    cache.put(question, record);
  }
}

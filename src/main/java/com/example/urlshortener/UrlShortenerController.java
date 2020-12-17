package com.example.urlshortener;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;

class Key {
  private final Integer clientId;
  private final String longURL;

  public Key(Integer clientId, String longURL) {
    this.clientId = clientId;
    this.longURL = longURL;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Key)) return false;
    return this.clientId.equals(((Key) o).clientId) && this.longURL.equals(((Key) o).longURL);
  }

  @Override
  public int hashCode() {
    return longURL.hashCode() + this.clientId;
  }
}

class LongURLAndStats {
  private final String longURL;
  private Integer count;

  LongURLAndStats(String longURL) {
    this.longURL = longURL;
    this.count = 0;
  }

  String getLongURL(){
    return this.longURL;
  }

  Integer getCount(){
    return this.count;
  }

  void incrementCount(){
    synchronized (this) {
      this.count++;
    }
  }
}

@RestController
public class UrlShortenerController {
  private long nextId = 0;
  private final HashMap<Key, Long> clientIdAndLongUrlToId = new HashMap<>();
  private final HashMap<Long, LongURLAndStats> idToLongURLAndDetails = new HashMap<>();
  private static final String HASH_STR = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int SHORT_URL_LENGTH = 8;

  public String getShortUrlFromId(long id) {
    StringBuilder strId = new StringBuilder();
    while(id > 0) {
      strId.append(HASH_STR.charAt((int)(id % HASH_STR.length())));
      id /= HASH_STR.length();
    }
    strId.reverse();
    return "a".repeat(Math.max(0, SHORT_URL_LENGTH - strId.length())) + strId;
  }

  public Long getIdFromShortUrl(String shortUrl) {
    long id = 0L;
    for (int i=0; i<shortUrl.length(); i++) {
      id *= 62;
      if (shortUrl.charAt(i) >= 'a' && shortUrl.charAt(i) <= 'z')
        id += shortUrl.charAt(i)-'a';
      else if (shortUrl.charAt(i) >= 'A' && shortUrl.charAt(i) <= 'Z')
        id += shortUrl.charAt(i)-'A'+36;
      else if (shortUrl.charAt(i) >= '0' && shortUrl.charAt(i) <= '9')
        id += shortUrl.charAt(i)-'0'+26;
    }
    return id;
  }

  private synchronized void addLongUrl(String longURL, Integer clientId) {
    clientIdAndLongUrlToId.put(new Key(clientId, longURL), nextId);
    idToLongURLAndDetails.put(nextId, new LongURLAndStats(longURL));
    nextId++;
  }

  @GetMapping("/getShortenedURL")
  public String getShortenedURL(@RequestParam(value = "long_url") String longURL,
                                @RequestParam(value = "client_id") Integer clientId) {
    if (!clientIdAndLongUrlToId.containsKey(new Key(clientId, longURL))) {
      addLongUrl(longURL, clientId);
    }
    return getShortUrlFromId(clientIdAndLongUrlToId.get(new Key(clientId, longURL)));
  }

  @GetMapping("/getOriginalURL")
  public String getOriginalURL(@RequestParam(value = "short_url") String shortURL) {
    if (!idToLongURLAndDetails.containsKey(getIdFromShortUrl(shortURL)))
      return "Not Found";
    LongURLAndStats i = idToLongURLAndDetails.get(getIdFromShortUrl(shortURL));
    i.incrementCount();
    return i.getLongURL();
  }

  @GetMapping("/getStatsForShortUrl")
  public Integer getStatsForShortUrl(@RequestParam(value = "short_url") String shortURL) {
    if (!idToLongURLAndDetails.containsKey(getIdFromShortUrl(shortURL)))
      return -1;
    return idToLongURLAndDetails.get(getIdFromShortUrl(shortURL)).getCount();
  }
}

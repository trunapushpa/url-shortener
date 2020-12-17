package com.example.urlshortener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerControllerTest {

  UrlShortenerController urlShortenerController;
  private final static int THREAD_COUNT = 4;

  @BeforeEach
  void setUp() {
    urlShortenerController = new UrlShortenerController();
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void getShortUrlFromId() {
    assertEquals("aaaaaaaa", urlShortenerController.getShortUrlFromId(0));
    assertEquals("aaaaaaab", urlShortenerController.getShortUrlFromId(1));
    assertEquals("aaaaaaaZ", urlShortenerController.getShortUrlFromId(61));
    assertEquals("aaaaaaba", urlShortenerController.getShortUrlFromId(62));
    assertEquals("aaaabaaa", urlShortenerController.getShortUrlFromId(62*62*62));
    assertEquals("aaaakiab", urlShortenerController.getShortUrlFromId(10*62*62*62 + 8*62*62 + 1));
    assertEquals("ZZZZZZZZ", urlShortenerController.getShortUrlFromId(62L*62L*62L*62L*62L*62L*62L*62L - 1));
  }

  @Test
  void getIdFromShortUrl() {
    assertEquals(0, urlShortenerController.getIdFromShortUrl("aaaaaaaa"));
    assertEquals(1, urlShortenerController.getIdFromShortUrl("aaaaaaab"));
    assertEquals(61, urlShortenerController.getIdFromShortUrl("aaaaaaaZ"));
    assertEquals(62, urlShortenerController.getIdFromShortUrl("aaaaaaba"));
    assertEquals(62*62*62, urlShortenerController.getIdFromShortUrl("aaaabaaa"));
    assertEquals(10*62*62*62 + 8*62*62 + 1, urlShortenerController.getIdFromShortUrl("aaaakiab"));
    assertEquals(62L*62L*62L*62L*62L*62L*62L*62L-1, urlShortenerController.getIdFromShortUrl("ZZZZZZZZ"));
  }

  @Test
  void getShortenedURL() {
    assertEquals("aaaaaaaa", urlShortenerController.getShortenedURL("google.com", 1));
    assertEquals("aaaaaaab", urlShortenerController.getShortenedURL("google.com", 2));
    assertEquals("aaaaaaac", urlShortenerController.getShortenedURL("facebook.com", 1));
    assertEquals("aaaaaaad", urlShortenerController.getShortenedURL("youtube.com", 1));
    assertEquals("aaaaaaab", urlShortenerController.getShortenedURL("google.com", 2));
  }

  @Test
  void getOriginalURL() {
    assertEquals("Not Found", urlShortenerController.getOriginalURL("aaaaaaaa"));
    this.getShortenedURL();
    assertEquals("google.com", urlShortenerController.getOriginalURL("aaaaaaaa"));
    assertEquals("youtube.com", urlShortenerController.getOriginalURL("aaaaaaad"));
  }

  @Test
  void getStatsForShortUrl() {
    this.getOriginalURL();
    assertEquals(1, urlShortenerController.getStatsForShortUrl("aaaaaaaa"));
    assertEquals(0, urlShortenerController.getStatsForShortUrl("aaaaaaab"));
    urlShortenerController.getOriginalURL("aaaaaaaa");
    assertEquals(2, urlShortenerController.getStatsForShortUrl("aaaaaaaa"));
  }

  @Test
  void concurrencyTestGetShortenedURL() throws BrokenBarrierException, InterruptedException {
    final CyclicBarrier gate = new CyclicBarrier(63);
    Vector<Thread> V = new Vector<>();
    for (int i=0; i<62; i++) {
      int finalI = i;
      V.add(new Thread(() -> {
        try {
          gate.await();
          urlShortenerController.getShortenedURL("google.com", finalI);
          gate.await();
        } catch (InterruptedException | BrokenBarrierException e) {
          e.printStackTrace();
        }
      }));
    }
    for (int i=0; i<62; i++){
      V.elementAt(i).start();
    }
    gate.await();
    gate.await();
    assertEquals("google.com", urlShortenerController.getOriginalURL(urlShortenerController.getShortUrlFromId(61)));
  }

  @Test
  void concurrencyTestGetOriginalURL() throws BrokenBarrierException, InterruptedException {
    final CyclicBarrier gate = new CyclicBarrier(163);
    urlShortenerController.getShortenedURL("google.com", 0);
    Vector<Thread> V = new Vector<>();
    for (int i=0; i<162; i++) {
      V.add(new Thread(() -> {
        try {
          gate.await();
          urlShortenerController.getOriginalURL(urlShortenerController.getShortUrlFromId(0));
          gate.await();
        } catch (InterruptedException | BrokenBarrierException e) {
          e.printStackTrace();
        }
      }));
    }
    for (int i=0; i<162; i++){
      V.elementAt(i).start();
    }
    gate.await();
    gate.await();
    assertEquals(162, urlShortenerController.getStatsForShortUrl(urlShortenerController.getShortUrlFromId(0)));
  }
}
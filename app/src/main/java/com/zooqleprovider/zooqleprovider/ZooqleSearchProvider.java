package com.zooqleprovider.zooqleprovider;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import com.plunder.provider.search.MovieSearchProvider;
import com.plunder.provider.search.MovieSearchRequest;
import com.plunder.provider.search.SearchResult;
import com.plunder.provider.search.TvSearchProvider;
import com.plunder.provider.search.TvSearchRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// TODO: completely rewrite :D
public class ZooqleSearchProvider implements MovieSearchProvider, TvSearchProvider {
  private final static String TAG = ZooqleSearchProvider.class.getSimpleName();

  private String category;

  public ZooqleSearchProvider(String category) {
    this.category = category;
  }

  private List<SearchResult> performSearch(String query) {
    List<SearchResult> results = new ArrayList<>();
    String url;

    try {
      url = String.format(Locale.getDefault(),
          "https://zooqle.com/search?q=%s%%20category%%3A%s&s=ns&v=t&sd=d",
          URLEncoder.encode(query, "UTF-8"), URLEncoder.encode(category, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "Failed to format URL", e);
      return results;
    }

    Document document;

    try {
      document = Jsoup.connect(url)
          .userAgent(
              "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
          .referrer("http://www.google.com")
          .get();
    } catch (IOException e) {
      Log.e(TAG, "Failed to make request", e);
      return results;
    }

    Elements resultRows = document.select(".table-torrents tr");

    if (resultRows == null || resultRows.size() == 0) {
      Log.d(TAG, "No results found");
      return results;
    }

    String[] querySegments = query.split("\\.|\\s");

    for (int i = 0; i < resultRows.size(); i++) {
      Element row = resultRows.get(i);
      Element nameAnchor = row.select("td:nth-child(2) a").first();
      Element seedsCell = row.select("td:nth-child(6) .prog-l").first();
      Element peersCell = row.select("td:nth-child(6) .prog-r").first();
      Element magnetAnchor = row.select("a[title=\"Magnet link\"]").first();

      if (nameAnchor == null || seedsCell == null || peersCell == null || magnetAnchor == null) {
        continue;
      }

      String name = nameAnchor.text();
      String[] nameSegments = name.split("\\.|\\s");
      boolean matchesQuery = true;

      for (int j = 0; j < querySegments.length; j++) {
        boolean matchFound = false;

        for (int k = 0; k < nameSegments.length; k++) {
          if (querySegments[j].equalsIgnoreCase(nameSegments[k])) {
            matchFound = true;
            break;
          }
        }

        if (!matchFound) {
          matchesQuery = false;
          break;
        }
      }

      if (!matchesQuery) {
        continue;
      }

      String quality = "Unknown";

      if (name.contains("1080p")) {
        quality = "1080p";
      } else if (name.contains("720p")) {
        quality = "720p";
      } else if (name.contains("HDTV")) {
        quality = "HDTV";
      }

      int seeds = -1;

      try {
        String seedsLiteral = seedsCell.text();

        if (!TextUtils.isEmpty(seedsLiteral)) {
          seedsLiteral = seedsLiteral.replace(" K", "000");
          seedsLiteral = seedsLiteral.replace(" M", "000000");
        }

        seeds = Integer.parseInt(seedsLiteral);
      } catch (NumberFormatException e) {
        Log.w(TAG, "Unable to format seeds", e);
      }

      int peers = -1;

      try {
        String peersLiteral = peersCell.text();

        if (!TextUtils.isEmpty(peersLiteral)) {
          peersLiteral = peersLiteral.replace(" K", "000");
          peersLiteral = peersLiteral.replace(" M", "000000");
        }

        peers = Integer.parseInt(peersLiteral);
      } catch (NumberFormatException e) {
        Log.w(TAG, "Unable to format peers", e);
      }

      String magnetUrl = magnetAnchor.attr("href");

      SearchResult result =
          new SearchResult.Builder().name(quality).uri(magnetUrl).peers(peers).seeds(seeds).build();
      results.add(result);
    }

    return results;
  }

  @Override public List<SearchResult> performSearch(@NonNull MovieSearchRequest request) {
    return performSearch(request.name());
  }

  @Override public List<SearchResult> performSearch(@NonNull TvSearchRequest request) {
    return performSearch(request.query());
  }
}
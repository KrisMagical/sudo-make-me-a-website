package com.magiccode.backend.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoEmbedParser {
    private static final Pattern IFRAME_SRC =
            Pattern.compile("<iframe[^>]*?src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://[^\\s)\"'>]+)", Pattern.CASE_INSENSITIVE);

    public record VideoCandidate(String provider, String sourceUrl, String embedUrl) {
    }

    public static List<VideoCandidate> parseAll(String content) {
        if (content == null) content = "";
        LinkedHashSet<String> urls = new LinkedHashSet<>();

        //iframe src
        Matcher m1 = IFRAME_SRC.matcher(content);
        while (m1.find()) {
            String src = m1.group(1);
            if (src != null && !src.isBlank()) urls.add(src.trim());
        }

        Matcher m2 = URL_PATTERN.matcher(content);
        while (m2.find()) {
            String url = m2.group(1);
            if (url != null && !url.isBlank()) urls.add(url.trim());
        }

        List<VideoCandidate> out = new ArrayList<>();
        for (String u : urls) {
            VideoCandidate c = normalize(u);
            if (c != null) out.add(c);
        }
        return out;
    }

    private static VideoCandidate normalize(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

            // YouTube
            if (host.contains("youtube.com")) {
                String v = getQueryParam(uri, "v");
                if (v != null && !v.isBlank()) {
                    String embed = "https://www.youtube.com/embed/" + v;
                    return new VideoCandidate("YOUTUBE", url, embed);
                }
                // already embed?
                if (uri.getPath() != null && uri.getPath().startsWith("/embed/")) {
                    return new VideoCandidate("YOUTUBE", url, url);
                }
            }
            if (host.contains("youtu.be")) {
                String path = uri.getPath(); // /VIDEOID
                if (path != null && path.length() > 1) {
                    String vid = path.substring(1);
                    String embed = "https://www.youtube.com/embed/" + vid;
                    return new VideoCandidate("YOUTUBE", url, embed);
                }
            }

            // Vimeo
            if (host.contains("vimeo.com")) {
                String path = uri.getPath(); // /123456
                if (path != null && path.length() > 1) {
                    String id = path.substring(1).split("/")[0];
                    if (id.matches("\\d+")) {
                        String embed = "https://player.vimeo.com/video/" + id;
                        return new VideoCandidate("VIMEO", url, embed);
                    }
                }
                if (host.contains("player.vimeo.com")) {
                    return new VideoCandidate("VIMEO", url, url);
                }
            }

            // Bilibili
            if (host.contains("bilibili.com")) {
                if (host.equals("player.bilibili.com")) {
                    return new VideoCandidate("BILIBILI", url, url);
                }

                String path = uri.getPath() == null ? "" : uri.getPath();
                if (path.startsWith("/video/")) {
                    String idPart = path.substring("/video/".length()).split("/")[0];

                    // Handle BV format: BV1xx...
                    if (idPart.matches("BV[0-9a-zA-Z]{10}")) {
                        String embed = "https://player.bilibili.com/player.html?bvid=" + idPart;
                        return new VideoCandidate("BILIBILI", url, embed);
                    }

                    // Handle av format: av123456
                    if (idPart.startsWith("av") && idPart.length() > 2) {
                        String aid = idPart.substring(2);
                        if (aid.matches("\\d+")) {
                            String embed = "https://player.bilibili.com/player.html?aid=" + aid;
                            return new VideoCandidate("BILIBILI", url, embed);
                        }
                    }
                }
            }

            if (url.toLowerCase(Locale.ROOT).contains("embed") || url.toLowerCase(Locale.ROOT).contains("player")) {
                return new VideoCandidate("IFRAME", url, url);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getQueryParam(URI uri, String key) {
        String q = uri.getQuery();
        if (q == null || q.isBlank()) return null;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String k = pair.substring(0, eq);
            String v = pair.substring(eq + 1);
            if (k.equals(key)) {
                return URLDecoder.decode(v, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

package com.lre.gitlabintegration.dto.lrescript;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WebFormsTokenParser {

    private static final Pattern HIDDEN_INPUT =
            Pattern.compile("<input[^>]*type=\"hidden\"[^>]*name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"[^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private WebFormsTokenParser() {}

    public static Map<String, String> parse(String html) {
        Map<String, String> tokens = new HashMap<>();
        Matcher m = HIDDEN_INPUT.matcher(html);
        while (m.find()) {
            tokens.put(m.group(1), htmlDecode(m.group(2)));
        }
        return tokens;
    }

    public static Map<String, String> buildDownloadOkForm(Map<String, String> tokens, int resultId) {
        Map<String, String> form = new HashMap<>();

        // Required fields (if present)
        copyIfPresent(tokens, form, "__VIEWSTATE");
        copyIfPresent(tokens, form, "__VIEWSTATEGENERATOR");
        copyIfPresent(tokens, form, "__EVENTVALIDATION");
        copyIfPresent(tokens, form, "__VIEWSTATEENCRYPTED");
        copyIfPresent(tokens, form, "__EVENTTARGET");
        copyIfPresent(tokens, form, "__EVENTARGUMENT");

        // Your JMeter screenshot fields:
        form.put("resultID", String.valueOf(resultId));
        form.put("ctl00$PageContent$btnOk", "OK");

        return form;
    }

    private static void copyIfPresent(Map<String, String> src, Map<String, String> dst, String key) {
        String v = src.get(key);
        if (v != null) dst.put(key, v);
    }

    // Minimal HTML decode for &amp; etc. (VIEWSTATE rarely needs it, but safe)
    private static String htmlDecode(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    public HttpHeaders createHtmlHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(java.util.List.of(MediaType.TEXT_HTML, MediaType.ALL));
        return h;
    }

    public HttpHeaders createFormPostHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(java.util.List.of(MediaType.ALL));
        return h;
    }

}

package com.guftagu.controller;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/link")
@CrossOrigin("*")
public class LinkPreviewController {

    @GetMapping("/preview")
    public ResponseEntity<Map<String, String>> getPreview(@RequestParam String url) {
        Map<String, String> preview = new HashMap<>();
        preview.put("url", url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; GuftaguBot/1.0)")
                    .timeout(5000)
                    .followRedirects(true)
                    .get();

            // Open Graph tags take priority
            String title = getMetaContent(doc, "og:title");
            if (title == null || title.isEmpty()) {
                title = doc.title();
            }

            String description = getMetaContent(doc, "og:description");
            if (description == null || description.isEmpty()) {
                Element descMeta = doc.selectFirst("meta[name=description]");
                if (descMeta != null) {
                    description = descMeta.attr("content");
                }
            }

            String image = getMetaContent(doc, "og:image");
            String siteName = getMetaContent(doc, "og:site_name");

            preview.put("title", title != null ? title : "");
            preview.put("description", description != null ? description : "");
            preview.put("image", image != null ? image : "");
            preview.put("siteName", siteName != null ? siteName : "");

        } catch (Exception e) {
            log.warn("Failed to fetch link preview for URL: {}", url, e);
            preview.put("title", "");
            preview.put("description", "");
            preview.put("image", "");
            preview.put("siteName", "");
        }

        return ResponseEntity.ok(preview);
    }

    private String getMetaContent(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "]");
        if (el != null) {
            return el.attr("content");
        }
        return null;
    }
}

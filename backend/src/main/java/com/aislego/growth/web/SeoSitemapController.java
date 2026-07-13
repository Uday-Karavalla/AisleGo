package com.aislego.growth.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seo")
public class SeoSitemapController {
    private final JdbcTemplate jdbc;
    private final String frontendUrl;

    public SeoSitemapController(JdbcTemplate jdbc,
                                @Value("${aislego.cors.allowed-origin}") String frontendUrl) {
        this.jdbc = jdbc;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        jdbc.query("select b.id from branches b join supermarkets s on s.id=b.supermarket_id " +
                        "where b.active=true and s.active=true and s.status='VERIFIED' order by b.id",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> xml.append("  <url><loc>").append(frontendUrl).append("/stores/")
                        .append(rs.getLong(1)).append("</loc><changefreq>daily</changefreq></url>\n"));
        jdbc.query("select b.id, p.id from branches b join supermarkets s on s.id=b.supermarket_id " +
                        "join products p on p.supermarket_id=s.id where b.active=true and s.active=true " +
                        "and s.status='VERIFIED' and p.active=true order by b.id, p.id",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> xml.append("  <url><loc>").append(frontendUrl).append("/stores/")
                        .append(rs.getLong(1)).append("/products/").append(rs.getLong(2))
                        .append("</loc><changefreq>daily</changefreq></url>\n"));
        return xml.append("</urlset>").toString();
    }
}

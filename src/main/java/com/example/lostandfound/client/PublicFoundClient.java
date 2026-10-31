package com.example.lostandfound.client;

import com.example.lostandfound.config.PublicDataProperties;
import com.example.lostandfound.entity.PublicFoundSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 공공데이터포털 습득물 API 호출
@Component
public class PublicFoundClient {

    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String SUCCESS = "00";

    private final RestClient restClient;
    private final PublicDataProperties properties;

    public PublicFoundClient(RestClient.Builder builder, PublicDataProperties properties) {

        // 외부 서버가 멈춰도 스케줄러 스레드가 묶이지 않게
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();

        this.properties = properties;
    }

    // 습득일 기간의 한 쪽
    public PublicFoundPage fetch(PublicFoundSource source, LocalDate from, LocalDate to, int pageNo, int numOfRows) {
        String xml = restClient.get()
                .uri(uri -> uri.path(pathOf(source))

                        // 키의 +, /, = 까지 인코딩되도록 URI 변수로 넣음
                        .queryParam("serviceKey", "{key}")
                        .queryParam("START_YMD", from.format(BASIC))
                        .queryParam("END_YMD", to.format(BASIC))
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .build(Map.of("key", properties.serviceKey())))
                .retrieve()
                .body(String.class);

        return parse(xml);
    }

    private String pathOf(PublicFoundSource source) {
        return switch (source) {
            case POLICE -> "/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";
            case PORTAL -> "/LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd";
        };
    }

    // 응답 XML -> 한 쪽
    PublicFoundPage parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("공공데이터 응답이 비어 있습니다");
        }

        Element root = readXml(xml).getDocumentElement();

        if (!SUCCESS.equals(textOf(root, "resultCode"))) {

            // 키 오류 등은 다른 모양(returnAuthMsg)으로 옴
            String reason = textOf(root, "returnAuthMsg");

            throw new IllegalStateException("공공데이터 응답 실패: " + (reason != null ? reason : textOf(root, "resultMsg")));
        }

        NodeList items = root.getElementsByTagName("item");

        List<PublicFoundPage.Row> rows = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            rows.add(new PublicFoundPage.Row(
                    textOf(item, "atcId"),
                    textOf(item, "fdSn"),
                    textOf(item, "fdPrdtNm"),
                    textOf(item, "prdtClNm"),
                    textOf(item, "clrNm"),
                    toDate(textOf(item, "fdYmd")),
                    textOf(item, "depPlace"),
                    textOf(item, "fdFilePathImg"),
                    textOf(item, "fdSbjt")
            ));
        }

        String total = textOf(root, "totalCount");

        return new PublicFoundPage(total == null ? 0 : Integer.parseInt(total), rows);
    }

    // 첫 번째 태그의 글자, 없거나 비면 null
    private static String textOf(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);

        if (nodes.getLength() == 0) {
            return null;
        }

        String text = nodes.item(0).getTextContent().strip();
        return text.isEmpty() ? null : text;
    }

    // 경찰서 2018-06-01, 기관 20180601 둘 다
    private static LocalDate toDate(String value) {
        if (value == null) {
            return null;
        }

        return LocalDate.parse(value.replace("-", ""), BASIC);
    }

    // 외부 XML은 DTD를 막아 XXE 차단
    private static Document readXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);

            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("공공데이터 응답을 읽을 수 없습니다", e);
        }
    }
}
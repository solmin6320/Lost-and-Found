package com.example.lostandfound.client;

import com.example.lostandfound.config.PublicDataProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 공공데이터 응답 XML 파싱 단위 테스트(실제 API는 부르지 않음)
class PublicFoundClientTest {

    private final PublicFoundClient client = new PublicFoundClient(
            RestClient.builder(), new PublicDataProperties(false, "test-key", "https://apis.test"));

    @Test
    @DisplayName("정상 응답에서 습득물 칸과 전체 건수를 읽고, 날짜는 두 모양 모두 읽음")
    void parse_success() {
        String xml = """
                <response>
                  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
                  <body>
                    <items>
                      <item>
                        <atcId>F2026103100001</atcId><fdSn>1</fdSn>
                        <fdPrdtNm>검은 지갑</fdPrdtNm><prdtClNm>지갑 &gt; 남성용 지갑</prdtClNm>
                        <clrNm>검정</clrNm><fdYmd>2026-10-30</fdYmd>
                        <depPlace>강남경찰서</depPlace><fdFilePathImg>https://img.test/1.jpg</fdFilePathImg>
                        <fdSbjt>검은 지갑 습득</fdSbjt>
                      </item>
                      <item>
                        <atcId>V2026103100002</atcId><fdSn>2</fdSn>
                        <fdPrdtNm>우산</fdPrdtNm><clrNm> </clrNm><fdYmd>20261029</fdYmd>
                      </item>
                    </items>
                    <totalCount>2</totalCount>
                  </body>
                </response>
                """;

        PublicFoundPage page = client.parse(xml);

        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.rows()).hasSize(2);

        PublicFoundPage.Row first = page.rows().get(0);

        assertThat(first.externalId()).isEqualTo("F2026103100001-1");
        assertThat(first.itemName()).isEqualTo("검은 지갑");
        assertThat(first.categoryName()).isEqualTo("지갑 > 남성용 지갑");
        assertThat(first.foundDate()).isEqualTo(LocalDate.of(2026, 10, 30)); // 경찰서 모양
        assertThat(first.storagePlace()).isEqualTo("강남경찰서");
        assertThat(first.imageUrl()).isEqualTo("https://img.test/1.jpg");

        PublicFoundPage.Row second = page.rows().get(1);

        assertThat(second.foundDate()).isEqualTo(LocalDate.of(2026, 10, 29)); // 기관 모양
        assertThat(second.colorName()).isNull(); // 빈 칸은 null
        assertThat(second.categoryName()).isNull(); // 없는 칸은 null
    }

    @Test
    @DisplayName("결과 코드가 00이 아니면 실패, 키 오류는 다른 모양의 메시지를 읽음")
    void parse_failures() {
        String noData = """
                <response><header><resultCode>03</resultCode><resultMsg>NODATA_ERROR</resultMsg></header></response>
                """;

        String keyError = """
                <OpenAPI_ServiceResponse><cmmMsgHeader>
                  <errMsg>SERVICE ERROR</errMsg><returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>
                </cmmMsgHeader></OpenAPI_ServiceResponse>
                """;

        assertThatThrownBy(() -> client.parse(noData))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NODATA_ERROR");

        assertThatThrownBy(() -> client.parse(keyError))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    @DisplayName("빈 응답은 실패")
    void parse_blank() {
        assertThatThrownBy(() -> client.parse("  "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("DOCTYPE이 든 응답은 읽지 않음(XXE 차단)")
    void parse_blocksDoctype() {

        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <response><header><resultCode>&x;</resultCode></header></response>
                """;

        assertThatThrownBy(() -> client.parse(xxe))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("읽을 수 없습니다");
    }
}
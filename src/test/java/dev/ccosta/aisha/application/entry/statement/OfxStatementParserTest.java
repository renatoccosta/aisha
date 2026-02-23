package dev.ccosta.aisha.application.entry.statement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfxStatementParserTest {

    private final OfxStatementParser parser = new OfxStatementParser();

    @Test
    void shouldParseOfx2CheckingFile() throws IOException {
        List<EntryStatementImportRecord> records = parser.parse(readFixture("src/test/resources/entry/statement-samples/ofx/v2/ofx2-checking-sample.ofx"));

        assertThat(records).hasSize(10);
        EntryStatementImportRecord first = records.getFirst();
        assertThat(first.movementDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(first.settlementDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(first.amount()).isEqualByComparingTo(new BigDecimal("5500.00"));
        assertThat(first.externalId()).isEqualTo("20260201-001");
        assertThat(first.notes()).isEqualTo("Salario mensal");
    }

    @Test
    void shouldParseOfx1CheckingFile() throws IOException {
        List<EntryStatementImportRecord> records = parser.parse(readFixture("src/test/resources/entry/statement-samples/ofx/v1/ofx1-checking-sample.ofx"));

        assertThat(records).hasSize(10);
        assertThat(records.get(1).amount()).isEqualByComparingTo(new BigDecimal("-1800.00"));
        assertThat(records.get(1).settlementDate()).isEqualTo(LocalDate.of(2026, 2, 2));
    }

    @Test
    void shouldParseOfx2CreditCardFileUsingLedgerBalanceDateAsSettlementDate() throws IOException {
        List<EntryStatementImportRecord> records = parser.parse(readFixture("src/test/resources/entry/statement-samples/ofx/v2/ofx2-credit-card-sample.ofx"));

        assertThat(records).hasSize(10);
        assertThat(records)
            .extracting(EntryStatementImportRecord::settlementDate)
            .containsOnly(LocalDate.of(2026, 2, 21));
        assertThat(records.getFirst().movementDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(records.getFirst().description()).isEqualTo("Streaming de video");
        assertThat(records.getFirst().notes()).isEqualTo("Assinatura mensal");
        assertThat(records.getFirst().externalId()).isEqualTo("CC-20260201-001");
    }

    @Test
    void shouldParseOfx1CreditCardFileAndDetectVersionAutomatically() throws IOException {
        List<EntryStatementImportRecord> records = parser.parse(readFixture("src/test/resources/entry/statement-samples/ofx/v1/ofx1-credit-card-sample.ofx"));

        assertThat(records).hasSize(10);
        assertThat(records.get(9).amount()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(records.get(9).settlementDate()).isEqualTo(LocalDate.of(2026, 2, 21));
    }

    @Test
    void shouldDetectOfx2ByBodyEvenWhenHeaderLooksLikeOfx1() throws IOException {
        byte[] ofx2Bytes = readFixture("src/test/resources/entry/statement-samples/ofx/v2/ofx2-checking-sample.ofx");
        String fakeOfx1Header = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102
            SECURITY:NONE
            ENCODING:USASCII
            CHARSET:1252
            COMPRESSION:NONE
            OLDFILEUID:NONE
            NEWFILEUID:NONE

            """;

        byte[] mixedContent = (fakeOfx1Header + new String(ofx2Bytes, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);

        List<EntryStatementImportRecord> records = parser.parse(mixedContent);

        assertThat(records).hasSize(10);
        assertThat(records.getFirst().description()).isEqualTo("Salario mensal");
        assertThat(records.getFirst().settlementDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void shouldDecodeOfxUsingCharsetFromHeaderWhenNotUtf8() {
        String ofx1Content = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102
            SECURITY:NONE
            ENCODING:USASCII
            CHARSET:1252
            COMPRESSION:NONE
            OLDFILEUID:NONE
            NEWFILEUID:NONE

            <OFX>
              <BANKMSGSRSV1>
                <STMTTRNRS>
                  <STMTRS>
                    <BANKTRANLIST>
                      <STMTTRN>
                        <DTPOSTED>20260201000000[-3:BRT]
                        <TRNAMT>-10.00
                        <FITID>id-1
                        <NAME>BB Rende Fácil - Rende Facil
                        <MEMO>Aplicação automática
                      </STMTTRN>
                    </BANKTRANLIST>
                  </STMTRS>
                </STMTTRNRS>
              </BANKMSGSRSV1>
            </OFX>
            """;

        byte[] cp1252Bytes = ofx1Content.getBytes(java.nio.charset.Charset.forName("windows-1252"));

        List<EntryStatementImportRecord> records = parser.parse(cp1252Bytes);

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().description()).isEqualTo("BB Rende Fácil - Rende Facil");
        assertThat(records.getFirst().notes()).isEqualTo("Aplicação automática");
    }

    private byte[] readFixture(String path) throws IOException {
        return Files.readAllBytes(Path.of(path));
    }
}

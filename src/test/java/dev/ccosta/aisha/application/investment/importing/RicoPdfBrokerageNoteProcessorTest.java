package dev.ccosta.aisha.application.investment.importing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class RicoPdfBrokerageNoteProcessorTest {

    private final RicoPdfBrokerageNoteProcessor processor = new RicoPdfBrokerageNoteProcessor();

    @Test
    void shouldParseSingleRicoBrokerageNotePdf() throws IOException {
        byte[] pdf = pdfWithPages(List.of(List.of(
            "NOTA DE NEGOCIAÇÃO",
            "Nr. nota Folha Data pregão",
            "5078534 1 18/11/2020",
            "Rico Investimentos - Grupo XP",
            "C.N.P.J: 02.332.886/0016-82 Carta Patente:",
            "Negócios realizados",
            "Q Negociação C/V Tipo mercado Prazo Especificação do título Obs. (*) Quantidade Preço / Ajuste Valor Operação / Ajuste D/C",
            "1-BOVESPA V FRACIONARIO CIELO          ON NM 70 3,75 262,50 C",
            "1-BOVESPA C VISTA FII XP MALLS          XPML11          CI 200 111,80 22.360,00 D",
            "Resumo dos Negócios Resumo Financeiro",
            "Taxa de liquidação 18,80 D",
            "Total Bovespa / Soma 2,21 D",
            "Total Custos / Despesas 0,00 D",
            "Líquido para 20/11/2020 67.643,76 D"
        )));
        BrokerageNoteProcessingRequest request = request("rico-2020.pdf", pdf);

        List<ParsedBrokerageNote> parsedNotes = processor.process(request);

        assertThat(processor.supports(request)).isTrue();
        assertThat(parsedNotes).hasSize(1);
        ParsedBrokerageNote parsedNote = parsedNotes.getFirst();
        assertThat(parsedNote.brokerageNote().getBrokerName()).isEqualTo("Rico");
        assertThat(parsedNote.brokerageNote().getBrokerCnpj()).isEqualTo("02.332.886/0016-82");
        assertThat(parsedNote.brokerageNote().getNoteNumber()).isEqualTo("5078534");
        assertThat(parsedNote.brokerageNote().getTradeDate()).hasToString("2020-11-18");
        assertThat(parsedNote.brokerageNote().getSettlementDate()).hasToString("2020-11-20");
        assertThat(parsedNote.brokerageNote().getNetAmount()).isEqualByComparingTo("-67643.76");
        assertThat(parsedNote.brokerageNote().getTotalCosts()).isEqualByComparingTo("21.01");
        assertThat(parsedNote.brokerageNote().getNetEntry().getAmount()).isEqualByComparingTo("-67643.76");
        assertThat(parsedNote.operations()).hasSize(2);
        assertThat(parsedNote.operations().getFirst().getOperationType()).isEqualTo(InvestmentOperationType.SELL);
        assertThat(parsedNote.operations().getFirst().getAsset().getName()).isEqualTo("CIELO ON");
        assertThat(parsedNote.operations().getFirst().getGrossAmount()).isEqualByComparingTo("262.50");
        assertThat(parsedNote.operations().get(1).getOperationType()).isEqualTo(InvestmentOperationType.BUY);
        assertThat(parsedNote.operations().get(1).getAsset().getType()).isEqualTo(AssetType.FII);
        assertThat(parsedNote.operations().get(1).getAsset().getTicker()).isEqualTo("XPML11");
    }

    @Test
    void shouldGroupContinuationPagesAndParseMultipleNotesFromSamePdf() throws IOException {
        byte[] pdf = pdfWithPages(List.of(
            List.of(
                "NOTA DE NEGOCIAÇÃO",
                "Nr. nota Folha Data pregão",
                "127127944 1 05/01/2026",
                "RICO CORRETORA DE TITULOS E VALORES MOBILIARIOS S.A.",
                "C.N.P.J: 13.434.335/0001-60 Carta Patente:",
                "Negócios realizados",
                "Q Negociação C/V Tipo mercado Prazo Especificação do título Obs. (*) Quantidade Preço / Ajuste Valor Operação / Ajuste D/C",
                "1-BOVESPA V VISTA BRADESPAR          PN EDJ N1 @ 958 20,23 19.384,14 C",
                "Total Bovespa / Soma CONTINUA..."
            ),
            List.of(
                "NOTA DE NEGOCIAÇÃO",
                "Nr. nota Folha Data pregão",
                "127127944 2 05/01/2026",
                "RICO CORRETORA DE TITULOS E VALORES MOBILIARIOS S.A.",
                "C.N.P.J: 13.434.335/0001-60 Carta Patente:",
                "Negócios realizados",
                "Q Negociação C/V Tipo mercado Prazo Especificação do título Obs. (*) Quantidade Preço / Ajuste Valor Operação / Ajuste D/C",
                "1-BOVESPA C VISTA FII XP MALLS          XPML11          CI @ 1 109,06 109,06 D",
                "Taxa de liquidação 26,54 D",
                "Total Bovespa / Soma 9,00 D",
                "Total Custos / Despesas 671,00 D",
                "Líquido para 07/01/2026 102,93 C"
            ),
            List.of(
                "NOTA DE NEGOCIAÇÃO",
                "Nr. nota Folha Data pregão",
                "127651096 1 12/01/2026",
                "RICO CORRETORA DE TITULOS E VALORES MOBILIARIOS S.A.",
                "C.N.P.J: 13.434.335/0001-60 Carta Patente:",
                "Negócios realizados",
                "Q Negociação C/V Tipo mercado Prazo Especificação do título Obs. (*) Quantidade Preço / Ajuste Valor Operação / Ajuste D/C",
                "1-BOVESPA C VISTA BKR IBOXX HY          DRE @ 1 54,45 54,45 D",
                "Taxa de liquidação 0,10 D",
                "Total Bovespa / Soma 0,03 D",
                "Total Custos / Despesas 2,70 D",
                "Líquido para 14/01/2026 66,12 C"
            )
        ));

        List<ParsedBrokerageNote> parsedNotes = processor.process(request("rico-2026.pdf", pdf));

        assertThat(parsedNotes).hasSize(2);
        assertThat(parsedNotes.getFirst().brokerageNote().getNoteNumber()).isEqualTo("127127944");
        assertThat(parsedNotes.getFirst().brokerageNote().getTotalCosts()).isEqualByComparingTo("706.54");
        assertThat(parsedNotes.getFirst().operations()).hasSize(2);
        assertThat(parsedNotes.getFirst().operations().getFirst().getAsset().getName()).isEqualTo("BRADESPAR PN");
        assertThat(parsedNotes.get(1).brokerageNote().getNoteNumber()).isEqualTo("127651096");
        assertThat(parsedNotes.get(1).brokerageNote().getTotalCosts()).isEqualByComparingTo("2.83");
        assertThat(parsedNotes.get(1).operations()).hasSize(1);
    }

    private BrokerageNoteProcessingRequest request(String fileName, byte[] pdf) {
        return new BrokerageNoteProcessingRequest(10L, fileName, "hash", pdf);
    }

    private byte[] pdfWithPages(List<List<String>> pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (List<String> lines : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                writeLines(document, page, lines);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void writeLines(PDDocument document, PDPage page, List<String> lines) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
            content.setLeading(11);
            content.newLineAtOffset(40, 760);
            for (String line : lines) {
                content.showText(line);
                content.newLine();
            }
            content.endText();
        }
    }
}

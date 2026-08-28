package it.vszdev.gedosanapi.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import it.vszdev.gedosanapi.dto.admin.PrenotazioneAdminResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class PrenotazionePdfService {

    private static final String[] INTESTAZIONI_COLONNE = {"ID", "Nome", "Cognome", "Orario", "Telefono", "Tipo donazione"};

    public byte[] generaPdf(String nomeTrasfusionale, LocalDate data, List<PrenotazioneAdminResponse> prenotazioni) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font fontTitolo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titolo = new Paragraph("Prenotazioni - " + nomeTrasfusionale + " - " + data.format(FormatiData.DATA), fontTitolo);
            titolo.setAlignment(Element.ALIGN_CENTER);
            titolo.setSpacingAfter(20);
            document.add(titolo);

            document.add(costruisciTabella(prenotazioni));
            document.close();
        } catch (com.lowagie.text.DocumentException e) {
            throw new IllegalStateException("Errore nella generazione del PDF", e);
        }

        return output.toByteArray();
    }

    private PdfPTable costruisciTabella(List<PrenotazioneAdminResponse> prenotazioni) {
        PdfPTable tabella = new PdfPTable(INTESTAZIONI_COLONNE.length);
        tabella.setWidthPercentage(100);

        Font fontIntestazione = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        for (String intestazione : INTESTAZIONI_COLONNE) {
            PdfPCell cella = new PdfPCell(new Paragraph(intestazione, fontIntestazione));
            cella.setBackgroundColor(new java.awt.Color(220, 220, 220));
            tabella.addCell(cella);
        }

        Font fontRiga = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (PrenotazioneAdminResponse p : prenotazioni) {
            tabella.addCell(new Paragraph(String.valueOf(p.id()), fontRiga));
            tabella.addCell(new Paragraph(p.nomeDonatore(), fontRiga));
            tabella.addCell(new Paragraph(p.cognomeDonatore(), fontRiga));
            tabella.addCell(new Paragraph(p.orarioPrenotazione().format(FormatiData.ORARIO), fontRiga));
            tabella.addCell(new Paragraph(p.cellulareDonatore(), fontRiga));
            tabella.addCell(new Paragraph(p.tipoDonazione().name(), fontRiga));
        }

        return tabella;
    }
}

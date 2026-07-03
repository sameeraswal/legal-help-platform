package com.legalhelp.petition.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfGenerator {

    public byte[] generate(String title, String bodyText, String disclaimer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font disclaimerFont = new Font(Font.HELVETICA, 9, Font.ITALIC);

            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph(" "));

            for (String paragraph : bodyText.split("\\n\\n")) {
                document.add(new Paragraph(paragraph.trim(), bodyFont));
                document.add(new Paragraph(" "));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph(disclaimer, disclaimerFont));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }
}

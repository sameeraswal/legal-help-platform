package com.legalhelp.petition.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class DocxGenerator {

    public byte[] generate(String title, String bodyText, String disclaimer) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            for (String paragraph : bodyText.split("\\n\\n")) {
                XWPFParagraph body = document.createParagraph();
                XWPFRun run = body.createRun();
                run.setText(paragraph.trim());
                run.setFontSize(11);
            }

            XWPFParagraph disclaimerPara = document.createParagraph();
            XWPFRun disclaimerRun = disclaimerPara.createRun();
            disclaimerRun.setText(disclaimer);
            disclaimerRun.setItalic(true);
            disclaimerRun.setFontSize(9);

            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate DOCX", e);
        }
    }
}

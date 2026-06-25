package com.app.godo.services.files;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class PdfProcessingService {

    public String extractText(MultipartFile pdfFile) {
        try (InputStream inputStream = pdfFile.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Extracted {} characters from PDF '{}'",
                    text.length(), pdfFile.getOriginalFilename());

            return text.trim();

        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to process PDF file", e);
        }
    }

    // Extracts text from raw PDF bytes (e.g., downloaded from MinIO).
    //
    // Used when we need to re-index a PDF that's already stored in MinIO.
    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Extracted {} characters from PDF bytes", text.length());
            return text.trim();

        } catch (IOException e) {
            log.error("Failed to extract text from PDF bytes: {}", e.getMessage());
            throw new RuntimeException("Failed to process PDF file", e);
        }
    }

    public boolean isValidPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        boolean isCorrectType = "application/pdf".equals(contentType);
        boolean hasCorrectExtension = filename != null && filename.toLowerCase().endsWith(".pdf");

        return isCorrectType && hasCorrectExtension;
    }
}

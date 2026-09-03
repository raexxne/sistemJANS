package my.gov.jans.access.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import my.gov.jans.access.domain.Permohonan;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermohonanServicePdfTest {

    @Test
    void shouldIncludeStaffNoteInApplicationPdf() throws IOException {
        PermohonanService service = new PermohonanService(null, null, null);
        service.publicUrl = "https://example.test";

        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000001");
        permohonan.setApplicantName("Ali Bin Abu");
        permohonan.setStaffNote("Catatan petugas untuk semakan sebelum keputusan");
        permohonan.setDirectorNote("Catatan pengarah untuk keputusan akhir");

        // FIX: pdfPermohonanInternal() telah dibuang (Isu #3 - kod bertindih).
        // Guna pdfPermohonan() sahaja, yang kini terus jana PDF dengan
        // includeInternalNotes = true (sama seperti pdfPermohonanInternal() dahulu).
        byte[] pdfBytes = service.pdfPermohonan(permohonan);
        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String text = extractor.getTextFromPage(1);
        reader.close();

        assertTrue(text.contains("Catatan Petugas"));
        assertTrue(text.contains("Catatan petugas untuk semakan sebelum keputusan"));
    }

    @Test
    void shouldNotIncludeInternalNotesInPublicPassPdf() throws IOException {
        PermohonanService service = new PermohonanService(null, null, null);
        service.publicUrl = "https://example.test";

        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000002");
        permohonan.setApplicantName("Siti Binti Ali");
        permohonan.setStaffNote("Catatan dalaman untuk petugas");
        permohonan.setDirectorNote("Catatan dalaman untuk pengarah");

        byte[] pdfBytes = service.pdfPasTanpaQR(permohonan);
        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String text = extractor.getTextFromPage(1);
        reader.close();

        assertFalse(text.contains("Catatan Petugas"));
        assertFalse(text.contains("Catatan Pengarah"));
        assertFalse(text.contains("Catatan dalaman untuk petugas"));
        assertFalse(text.contains("Catatan dalaman untuk pengarah"));
    }
}
package my.gov.jans.access.service;

import my.gov.jans.access.domain.*;
import my.gov.jans.access.repo.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import java.awt.Color;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

@Service
public class PermohonanService {
    private final PermohonanRepository repo;
    private final PenggunaRepository pengguna;
    private final EmailService emailService;
    @Value("${app.public-url}")
    String publicUrl;

    public PermohonanService(PermohonanRepository r, PenggunaRepository p, EmailService e) {
        repo = r;
        pengguna = p;
        emailService = e;
    }

    @Transactional
    public Permohonan cipta(Permohonan p) {
        p.setId(null);
        p.setReviewedBy(null);
        p.setDecidedBy(null);
        p.setPassToken(null);
        p.setDecisionAt(null);
        p.setStaffNote(null);
        p.setDirectorNote(null);
        p.setStatus(StatusPermohonan.DIHANTAR);

        if (p.getPelawat().isEmpty()) {
            throw new IllegalArgumentException("Maklumat sekurang-kurangnya seorang pelawat diperlukan");
        }
        if (p.getEmailWakil() == null || p.getEmailWakil().isBlank()) {
            throw new IllegalArgumentException("E-mel untuk dihubungi diperlukan");
        }

        // Alias: jika phoneMobile diisi tetapi phone kosong (legacy)
        if ((p.getPhone() == null || p.getPhone().isBlank())
                && p.getPhoneMobile() != null && !p.getPhoneMobile().isBlank()) {
            p.setPhone(p.getPhoneMobile());
        }
        // Alias sebaliknya: data lama hanya ada phone
        if ((p.getPhoneMobile() == null || p.getPhoneMobile().isBlank())
                && p.getPhone() != null && !p.getPhone().isBlank()) {
            p.setPhoneMobile(p.getPhone());
        }

        // Tarikh permohonan automatik jika tidak diisi
        if (p.getApplicationDate() == null) {
            p.setApplicationDate(LocalDate.now());
        }

        p.setNomborPermohonan(janaNomborPermohonanSeterusnya());
        Permohonan disimpan;
        try {
            disimpan = repo.save(p);
        } catch (DataIntegrityViolationException e) {
            // Nombor perlanggaran jarang berlaku (contoh: permintaan serentak) - jana
            // semula sekali sahaja
            p.setNomborPermohonan(janaNomborPermohonanSeterusnya());
            disimpan = repo.save(p);
        }
        hantarEmelPengesahanPermohonan(disimpan);
        return disimpan;
    }

    private String janaNomborPermohonanSeterusnya() {
        String prefix = "JAS-" + Year.now().getValue() + "-";
        long seterusnya = repo.findFirstByNomborPermohonanStartingWithOrderByNomborPermohonanDesc(prefix)
                .map(p -> Objects.requireNonNull(p).getNomborPermohonan())
                .map(no -> {
                    try {
                        return Long.parseLong(no.substring(prefix.length())) + 1;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(1L);
        return prefix + String.format("%06d", seterusnya);
    }

    private void hantarEmelPengesahanPermohonan(Permohonan p) {
        if (p.getEmailWakil() == null || p.getEmailWakil().isBlank())
            return;
        try {
            String isi = "<p>Tuan/Puan,</p>"
                    + "<p>Permohonan akses anda telah <b>berjaya dihantar</b> dan kini menunggu semakan pihak JAS.</p>"
                    + "<p><b>No. Permohonan:</b> " + p.getNomborPermohonan() + "</p>"
                    + "<p>Sila simpan nombor permohonan ini untuk semakan status permohonan anda pada bila-bila masa.</p>"
                    + "<p>Terima kasih.</p>";
            emailService.hantarEmail(
                    p.getEmailWakil(),
                    "Permohonan Berjaya Dihantar - " + p.getNomborPermohonan(),
                    isi,
                    null,
                    null);
        } catch (Exception e) {
            // Kegagalan hantar e-mel pengesahan tidak boleh menggagalkan permohonan yang
            // telah berjaya disimpan
        }
    }

    public void hantarEmelKegagalanPermohonan(String email, String sebab) {
        if (email == null || email.isBlank())
            return;
        try {
            String isi = "<p>Tuan/Puan,</p>"
                    + "<p>Permohonan akses anda <b>tidak berjaya dihantar</b>.</p>"
                    + "<p><b>Sebab:</b> " + (sebab == null || sebab.isBlank() ? "Ralat sistem" : sebab) + "</p>"
                    + "<p>Sila cuba semula atau hubungi pihak JAS jika masalah berterusan.</p>"
                    + "<p>Terima kasih.</p>";
            emailService.hantarEmail(
                    email,
                    "Permohonan Tidak Berjaya Dihantar - JAS",
                    isi,
                    null,
                    null);
        } catch (Exception e) {
            // Kegagalan hantar e-mel notis tidak boleh mengganggu respons ralat asal kepada
            // pemohon
        }
    }

    public Permohonan cari(String no) {
        String nomborPermohonan = Objects.requireNonNull(no, "Nombor permohonan diperlukan").trim()
                .toUpperCase(Locale.ROOT);
        return repo.findByNomborPermohonan(nomborPermohonan)
                .orElseThrow(() -> new IllegalArgumentException("Nombor permohonan tidak ditemui"));
    }

    public Permohonan cari(Long id) {
        return repo.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Permohonan tidak ditemui"));
    }

    public List<Permohonan> senarai(StatusPermohonan s) {
        return repo.findByStatusOrderByCreatedAtDesc(s);
    }

    public List<Permohonan> senaraiSemua() {
        return repo.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional
    public Permohonan hantarPengarah(Long id, String catatan, String email) {
        Permohonan p = repo.findById(Objects.requireNonNull(id)).orElseThrow();
        if (p.getStatus() == StatusPermohonan.MENUNGGU_PENGARAH)
            return p;
        if (p.getStatus() != StatusPermohonan.DIHANTAR && p.getStatus() != StatusPermohonan.SEMAKAN_STAFF)
            throw new IllegalStateException("Status tidak sah");
        p.setStaffNote(catatan);
        p.setReviewedBy(pengguna.findByEmail(email).orElseThrow());
        p.setStatus(StatusPermohonan.MENUNGGU_PENGARAH);
        repo.save(p);

        String linkLogin = (publicUrl == null || publicUrl.isBlank())
                ? "/login.html"
                : publicUrl + "/login.html";
        String isi = "<p>Tuan/Puan,</p>"
                + "<p>Satu permohonan baru telah dihantar untuk semakan dan keputusan.</p>"
                + "<p><b>No. Permohonan:</b> " + p.getNomborPermohonan() + "</p>"
                + "<p>Sila semak butiran permohonan dan pilih sama ada untuk <b>meluluskan</b> atau <b>menolak</b> permohonan tersebut.</p>"
                + "<p>Sila log masuk ke sistem untuk meneruskan urusan ini. "
                + "<a href=\"" + linkLogin + "\">Klik sini untuk log masuk</a>.</p>"
                + "<p>Terima kasih.</p>";

        List<Pengguna> pengarahList = pengguna.findByRole(Role.PENGARAH);
        for (Pengguna pengarah : pengarahList) {
            if (pengarah.getEmail() != null && !pengarah.getEmail().isBlank()) {
                emailService.hantarEmail(
                        pengarah.getEmail(),
                        "Permohonan baru diterima - " + p.getNomborPermohonan(),
                        isi,
                        null,
                        null);
            }
        }

        return p;
    }

    @Transactional
    public void padamPermohonanStaf(Long id) {
        Permohonan p = repo.findById(Objects.requireNonNull(id)).orElseThrow();
        if (p.getStatus() != StatusPermohonan.DIHANTAR && p.getStatus() != StatusPermohonan.SEMAKAN_STAFF)
            throw new IllegalStateException("Hanya permohonan belum dihantar kepada pengarah boleh dipadam");
        repo.delete(p);
    }

    @Transactional
    public Permohonan keputusan(Long id, boolean lulus, String catatan, String email) {
        Permohonan p = repo.findById(Objects.requireNonNull(id)).orElseThrow();
        if (p.getStatus() != StatusPermohonan.MENUNGGU_PENGARAH)
            throw new IllegalStateException("Permohonan belum dihantar kepada pengarah");
        p.setDirectorNote(catatan);
        p.setDecidedBy(pengguna.findByEmail(email).orElseThrow());
        p.setDecisionAt(LocalDateTime.now());

        // Keputusan pengarah terus muktamad: lulus terus mengeluarkan pas (tiada lagi
        // langkah berasingan oleh petugas), tolak terus menamatkan permohonan.
        String subjek;
        String isi;
        String linkSemak = (publicUrl == null || publicUrl.isBlank())
                ? "/semak.html?no=" + p.getNomborPermohonan()
                : publicUrl + "/semak.html?no=" + p.getNomborPermohonan();

        if (lulus) {
            p.setPassToken(token());
            p.setStatus(StatusPermohonan.PAS_DIKELUARKAN);
            subjek = "Permohonan Diluluskan - " + p.getNomborPermohonan();
            isi = "<p>Tuan/Puan,</p>"
                    + "<p>Permohonan akses anda dengan nombor <b>" + p.getNomborPermohonan() + "</b> "
                    + "telah <b>diluluskan</b> oleh pihak pengarah dan pas kebenaran telah dikeluarkan.</p>"
                    + "<p>Sila semak status permohonan untuk memilih tindakan seterusnya (cetak PDF atau tunjuk QR). "
                    + "<a href=\"" + linkSemak + "\">Klik sini</a> untuk semak permohonan anda.</p>"
                    + "<p>Terima kasih.</p>";
        } else {
            p.setStatus(StatusPermohonan.DITOLAK);
            subjek = "Permohonan Ditolak - " + p.getNomborPermohonan();
            String sebabPenolakan = catatan == null || catatan.isBlank()
                    ? "Tiada sebab diberikan"
                    : catatan;
            isi = "<p>Tuan/Puan,</p>"
                    + "<p>Permohonan akses anda dengan nombor <b>" + p.getNomborPermohonan() + "</b> "
                    + "adalah <b>tidak diluluskan</b> oleh pihak pengarah.</p>"
                    + "<p><b>Sebab tidak diluluskan:</b> " + sebabPenolakan + "</p>"
                    + "<p>Untuk sebarang pertanyaan lanjut, sila hubungi pihak JAS.</p>"
                    + "<p>Terima kasih.</p>";
        }

        repo.save(p);

        emailService.hantarEmail(
                p.getEmailWakil(),
                subjek,
                isi,
                null,
                null);

        return p;
    }

    // FIX Isu #3: pdfPermohonan() dahulu cuma wrapper kosong yang panggil
    // pdfPermohonanInternal() — kedua-dua method buat perkara yang SAMA.
    // Digabung jadi satu method sahaja untuk buang kod bertindih.
    // NOTA: jika pdfPermohonanInternal() turut dipanggil terus di
    // PermohonanController.java atau tempat lain, gantikan rujukan itu
    // kepada pdfPermohonan() sebelum deploy.
    public byte[] pdfPermohonan(Permohonan p) {
        return pdf(p, "BUTIRAN PERMOHONAN KEBENARAN MASUK", false, true);
    }

    public byte[] pdfPas(Permohonan p) {
        return pdf(p, "PAS KEBENARAN MASUK JAS", true, false);
    }

    public byte[] pdfPasTanpaQR(Permohonan p) {
        return pdf(p, "PAS KEBENARAN MASUK JAS", false, false);
    }

    public byte[] qrCodeImage(Permohonan p) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(publicUrl + "/sah.html?token=" + p.getPassToken(),
                    BarcodeFormat.QR_CODE, 400, 400);
            ByteArrayOutputStream qr = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", qr);
            return qr.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("QR tidak dapat dijana", e);
        }
    }

    public Permohonan cariToken(String token) {
        return repo.findByPassToken(token).orElseThrow(() -> new IllegalArgumentException("Pas tidak ditemui"));
    }

    /** Format masa 24j → 12j AM/PM */
    private String formatMasa(LocalTime t) {
        if (t == null)
            return "-";
        int h = t.getHour();
        int m = t.getMinute();
        String period = h >= 12 ? "PM" : "AM";
        int h12 = h % 12;
        if (h12 == 0)
            h12 = 12;
        return String.format("%02d:%02d %s", h12, m, period);
    }

    private byte[] pdf(Permohonan p, String tajuk, boolean includeQr, boolean includeInternalNotes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document d = new Document(PageSize.A4, 50, 50, 40, 40);
            PdfWriter.getInstance(d, out);
            d.open();

            Color navy = new Color(0, 70, 127);
            Color lightBlue = new Color(220, 235, 248);
            Color white = Color.WHITE;

            Font fOrgBig = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, navy);
            Font fOrgSub = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, navy);
            Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, white);
            Font fLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 30, 30));
            Font fValue = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font fSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font fFooter = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY);
            Font fSigName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

            // ── HEADER ──────────────────────────────────────────────────────
            PdfPTable header = new PdfPTable(new float[] { 1f, 3f });
            header.setWidthPercentage(100);
            header.setSpacingAfter(4);

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setPadding(6);

            try (InputStream ls = getClass().getResourceAsStream("/static/images/logo.png")) {
                if (ls != null) {
                    Image logo = Image.getInstance(ls.readAllBytes());
                    logo.scaleToFit(70, 70);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    logoCell.addElement(logo);
                } else {
                    Paragraph ph = new Paragraph("JAS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, navy));
                    ph.setAlignment(Element.ALIGN_CENTER);
                    logoCell.addElement(ph);
                }
            } catch (Exception ignored) {
                Paragraph ph = new Paragraph("JAS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, navy));
                ph.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(ph);
            }
            header.addCell(logoCell);

            PdfPCell orgCell = new PdfPCell();
            orgCell.setBorder(Rectangle.NO_BORDER);
            orgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            orgCell.setPaddingLeft(10);

            Paragraph orgName = new Paragraph("JABATAN AIR SABAH (JAS)", fOrgBig);
            orgName.setAlignment(Element.ALIGN_LEFT);
            Paragraph orgMin = new Paragraph("Kementerian Kemudahan Asas Dan Utiliti, Sabah", fOrgSub);
            orgMin.setAlignment(Element.ALIGN_LEFT);
            Paragraph orgWeb = new Paragraph("www.jans.sabah.gov.my", fOrgSub);
            orgWeb.setAlignment(Element.ALIGN_LEFT);
            orgCell.addElement(orgName);
            orgCell.addElement(orgMin);
            orgCell.addElement(orgWeb);
            header.addCell(orgCell);
            d.add(header);

            LineSeparator sep = new LineSeparator(2f, 100f, navy, Element.ALIGN_CENTER, -2f);
            d.add(new Chunk(sep));

            Paragraph title = new Paragraph(tajuk, fTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(12);
            title.setSpacingAfter(12);
            d.add(title);

            // ── JADUAL BUTIRAN (selaras mohon.html) ──────────────────────────
            PdfPTable tbl = new PdfPTable(new float[] { 2.2f, 3.8f });
            tbl.setWidthPercentage(100);
            tbl.setSpacingAfter(16);

            PdfPCell hc = new PdfPCell(new Phrase("MAKLUMAT PERMOHONAN", fHead));
            hc.setColspan(2);
            hc.setBackgroundColor(navy);
            hc.setPadding(7);
            hc.setBorder(Rectangle.NO_BORDER);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(hc);

            addRow(tbl, "No. Permohonan", p.getNomborPermohonan(), fLabel, fValue, lightBlue, white);
            addRow(tbl, "E-mel Untuk Dihubungi (Wakil)", p.getEmailWakil(), fLabel, fValue, lightBlue, white);
            addRow(tbl, "No. Telefon (Pejabat)", p.getPhoneOffice(), fLabel, fValue, lightBlue, white);
            addRow(tbl, "Kementerian / Jabatan / Agensi / Universiti / Syarikat",
                    p.getOrganisation(), fLabel, fValue, lightBlue, white);
            addRow(tbl, "Tarikh Permohonan",
                    p.getApplicationDate() != null ? p.getApplicationDate().toString() : "-",
                    fLabel, fValue, lightBlue, white);
            addRow(tbl, "Tarikh Lawatan",
                    p.getVisitDate() != null ? p.getVisitDate().toString() : "-",
                    fLabel, fValue, lightBlue, white);
            addRow(tbl, "Masa Lawatan", formatMasa(p.getVisitTime()), fLabel, fValue, lightBlue, white);
            addRow(tbl, "Jenis Lokasi",
                    p.getLocationType() != null ? p.getLocationType().toString() : "-",
                    fLabel, fValue, lightBlue, white);
            addRow(tbl, "Nama / Alamat Lokasi", p.getLocationName(), fLabel, fValue, lightBlue, white);
            addRow(tbl, "Tujuan Lawatan", p.getPurpose(), fLabel, fValue, lightBlue, white);
            if (includeInternalNotes && p.getStatus() != StatusPermohonan.MENUNGGU_PENGARAH) {
                addRow(tbl, "Status",
                        p.getStatus() != null ? p.getStatus().toString().replace("_", " ") : "-",
                        fLabel, fValue, lightBlue, white);
            }
            if (includeInternalNotes && p.getStaffNote() != null && !p.getStaffNote().isBlank())
                addRow(tbl, "Catatan Petugas", p.getStaffNote(), fLabel, fValue, lightBlue, white);
            if (includeInternalNotes && p.getDirectorNote() != null && !p.getDirectorNote().isBlank())
                addRow(tbl, "Catatan Pengarah", p.getDirectorNote(), fLabel, fValue, lightBlue, white);

            d.add(tbl);

            if (!p.getPelawat().isEmpty()) {
                Paragraph tajukPelawat = new Paragraph("MAKLUMAT PELAWAT", fTitle);
                tajukPelawat.setSpacingBefore(4);
                tajukPelawat.setSpacingAfter(6);
                d.add(tajukPelawat);

                for (int index = 0; index < p.getPelawat().size(); index++) {
                    Pelawat pelawat = p.getPelawat().get(index);
                    PdfPTable jadualPelawat = new PdfPTable(new float[] { 2.2f, 3.8f });
                    jadualPelawat.setWidthPercentage(100);
                    jadualPelawat.setSpacingAfter(10);

                    PdfPCell tajukPelawatCell = new PdfPCell(new Phrase("Pelawat " + (index + 1), fHead));
                    tajukPelawatCell.setColspan(2);
                    tajukPelawatCell.setBackgroundColor(navy);
                    tajukPelawatCell.setPadding(7);
                    tajukPelawatCell.setBorder(Rectangle.NO_BORDER);
                    jadualPelawat.addCell(tajukPelawatCell);

                    addRow(jadualPelawat, "Nama Penuh", pelawat.getNamaPenuh(), fLabel, fValue, lightBlue, white);
                    addRow(jadualPelawat, "No. Kad Pengenalan", pelawat.getNoKadPengenalan(), fLabel, fValue, lightBlue,
                            white);
                    addRow(jadualPelawat, "E-mel", pelawat.getEmail(), fLabel, fValue, lightBlue, white);
                    addRow(jadualPelawat, "No. Telefon (Bimbit)", pelawat.getNoTelefonBimbit(), fLabel, fValue,
                            lightBlue, white);
                    addRow(jadualPelawat, "Jawatan", pelawat.getJawatan(), fLabel, fValue, lightBlue, white);
                    addRow(jadualPelawat, "No. Pendaftaran Kenderaan", pelawat.getNoPendaftaranKenderaan(), fLabel,
                            fValue, lightBlue, white);
                    d.add(jadualPelawat);
                }
            }

            // ── KOD QR ──────────────────────────────────────────────────────
            if (includeQr) {
                BitMatrix matrix = new MultiFormatWriter().encode(
                        publicUrl + "/sah.html?token=" + p.getPassToken(),
                        BarcodeFormat.QR_CODE, 200, 200);
                ByteArrayOutputStream qrBuf = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(matrix, "PNG", qrBuf);

                Image qrImg = Image.getInstance(qrBuf.toByteArray());
                qrImg.scaleToFit(110, 110);

                PdfPTable qrTbl = new PdfPTable(new float[] { 1f, 2.5f });
                qrTbl.setWidthPercentage(65);
                qrTbl.setHorizontalAlignment(Element.ALIGN_CENTER);
                qrTbl.setSpacingAfter(16);

                PdfPCell qrImgCell = new PdfPCell(qrImg);
                qrImgCell.setPadding(6);
                qrImgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qrImgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                qrTbl.addCell(qrImgCell);

                PdfPCell qrTextCell = new PdfPCell();
                qrTextCell.setBorder(Rectangle.NO_BORDER);
                qrTextCell.setPaddingLeft(10);
                qrTextCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                qrTextCell.addElement(new Paragraph("Imbas kod QR ini untuk mengesahkan kesahihan pas.", fValue));
                qrTextCell.addElement(new Paragraph("Scan QR code to verify pass authenticity.", fSmall));
                qrTbl.addCell(qrTextCell);

                d.add(qrTbl);
            }

            // ── TANDATANGAN ─────────────────────────────────────────────────
            boolean sudahDiluluskan = p.getStatus() == StatusPermohonan.DILULUSKAN
                    || p.getStatus() == StatusPermohonan.PAS_DIKELUARKAN;

            if (sudahDiluluskan) {
                PdfPTable sigTbl = new PdfPTable(new float[] { 2f, 1f });
                sigTbl.setWidthPercentage(100);
                sigTbl.setSpacingBefore(10);
                sigTbl.setSpacingAfter(10);

                PdfPCell spacerCell = new PdfPCell();
                spacerCell.setBorder(Rectangle.NO_BORDER);
                spacerCell.setPadding(0);
                sigTbl.addCell(spacerCell);

                PdfPCell dirSig = new PdfPCell();
                dirSig.setBorder(Rectangle.NO_BORDER);
                dirSig.setPadding(10);
                dirSig.setHorizontalAlignment(Element.ALIGN_CENTER);

                Paragraph dilulusLabel = new Paragraph("Diluluskan oleh:", fLabel);
                dilulusLabel.setAlignment(Element.ALIGN_CENTER);
                dirSig.addElement(dilulusLabel);

                try (InputStream ss = getClass().getResourceAsStream("/static/images/tandatangan-pengarah.png")) {
                    if (ss != null) {
                        Image sig = Image.getInstance(ss.readAllBytes());
                        sig.scaleToFit(150, 55);
                        sig.setAlignment(Element.ALIGN_CENTER);
                        dirSig.addElement(sig);
                    } else {
                        Paragraph blank = new Paragraph("\n\n\n\n", fValue);
                        blank.setAlignment(Element.ALIGN_CENTER);
                        dirSig.addElement(blank);
                    }
                } catch (Exception ignored) {
                    Paragraph blank = new Paragraph("\n\n\n\n", fValue);
                    blank.setAlignment(Element.ALIGN_CENTER);
                    dirSig.addElement(blank);
                }

                Paragraph underline = new Paragraph("_________________________", fValue);
                underline.setAlignment(Element.ALIGN_CENTER);
                dirSig.addElement(underline);

                String dirName = (p.getDecidedBy() != null && p.getDecidedBy().getName() != null)
                        ? p.getDecidedBy().getName()
                        : "Pengarah";
                Paragraph dirNamePar = new Paragraph(dirName, fSigName);
                dirNamePar.setAlignment(Element.ALIGN_CENTER);
                dirSig.addElement(dirNamePar);

                Paragraph dirPos = new Paragraph("Pengarah, JAS", fSmall);
                dirPos.setAlignment(Element.ALIGN_CENTER);
                dirSig.addElement(dirPos);

                Paragraph dirDate = new Paragraph(
                        "Tarikh: " + (p.getDecisionAt() != null ? p.getDecisionAt().toLocalDate().toString() : ""),
                        fSmall);
                dirDate.setAlignment(Element.ALIGN_CENTER);
                dirSig.addElement(dirDate);

                sigTbl.addCell(dirSig);
                d.add(sigTbl);
            }

            d.add(new Chunk(new LineSeparator(1f, 100f, navy, Element.ALIGN_CENTER, -2f)));

            Paragraph footer = new Paragraph(
                    "Dokumen ini dijana secara automatik oleh Sistem Akses JAS. "
                            + "Sebarang pemalsuan adalah kesalahan jenayah di bawah Akta Komputer 1997.",
                    fFooter);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(4);
            d.add(footer);

            d.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF tidak dapat dijana", e);
        }
    }

    private void addRow(PdfPTable tbl, String label, String value,
            Font fLabel, Font fValue, Color bgLabel, Color bgValue) {
        PdfPCell lc = new PdfPCell(new Phrase(label, fLabel));
        lc.setBackgroundColor(bgLabel);
        lc.setPadding(7);
        tbl.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value != null && !value.isBlank() ? value : "-", fValue));
        vc.setBackgroundColor(bgValue);
        vc.setPadding(7);
        tbl.addCell(vc);
    }

    private String token() {
        byte[] b = new byte[24];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public void hantarEmailHubungi(String emailPengirim, String nama, String subjek, String isi) {
        String penerimaSokongan = "jansofficial86@gmail.com";
        String subjekLengkap = "[Pertanyaan JAS] " + subjek + " - daripada " + nama;
        emailService.hantarEmail(penerimaSokongan, subjekLengkap, isi, null, null);
        String isiAck = "<p>Terima kasih, <b>" + nama + "</b>.</p>"
                + "<p>Mesej anda telah kami terima dan akan ditangani dalam masa 2 hari bekerja.</p>"
                + "<p>Jika pertanyaan mendesak, sila hubungi kami terus di jansofficial86@gmail.com.</p>"
                + "<p>Salam hormat,<br>Pasukan JAS</p>";
        emailService.hantarEmail(emailPengirim, "Pengesahan penerimaan mesej - JAS", isiAck, null, null);
    }

    public void hantarEmailAduan(String emailPengadu, String nama, String rujukan, String isi) {
        String penerimaSokongan = "jansofficial86@gmail.com";
        String subjekAdmin = "[Aduan JAS] Rujukan: " + rujukan;
        emailService.hantarEmail(penerimaSokongan, subjekAdmin, isi, null, null);
        String isiAck = "<p>Terima kasih, <b>" + nama + "</b>.</p>"
                + "<p>Aduan anda telah kami terima dengan nombor rujukan: <b>" + rujukan + "</b>.</p>"
                + "<p>Penyiasatan akan dijalankan dalam masa 3 hari bekerja. "
                + "Sila simpan nombor rujukan ini untuk semakan susulan.</p>"
                + "<p>Salam hormat,<br>Pasukan JAS</p>";
        emailService.hantarEmail(emailPengadu, "Pengesahan aduan " + rujukan + " - JAS", isiAck, null, null);
    }
}
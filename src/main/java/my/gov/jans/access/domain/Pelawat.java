package my.gov.jans.access.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "application_visitors")
public class Pelawat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nama_penuh", nullable = false)
    private String namaPenuh;

    @Column(name = "no_kad_pengenalan")
    private String noKadPengenalan;

    @Column(name = "jenis_pengenalan_diri")
    private String jenisPengenalanDiri;

    private String warganegara;

    @Column(name = "no_permit_kerja")
    private String noPermitKerja;

    @Column(nullable = false)
    private String email;

    @Column(name = "no_telefon_bimbit", nullable = false)
    private String noTelefonBimbit;

    @Column(nullable = false)
    private String jawatan;

    @Column(name = "no_pendaftaran_kenderaan")
    private String noPendaftaranKenderaan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnore
    private Permohonan permohonan;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNamaPenuh() {
        return namaPenuh;
    }

    public void setNamaPenuh(String namaPenuh) {
        this.namaPenuh = namaPenuh;
    }

    public String getNoKadPengenalan() {
        return noKadPengenalan;
    }

    public void setNoKadPengenalan(String noKadPengenalan) {
        this.noKadPengenalan = noKadPengenalan;
    }

    public String getJenisPengenalanDiri() {
        return jenisPengenalanDiri;
    }

    public void setJenisPengenalanDiri(String jenisPengenalanDiri) {
        this.jenisPengenalanDiri = jenisPengenalanDiri;
    }

    public String getWarganegara() {
        return warganegara;
    }

    public void setWarganegara(String warganegara) {
        this.warganegara = warganegara;
    }

    public String getNoPermitKerja() {
        return noPermitKerja;
    }

    public void setNoPermitKerja(String noPermitKerja) {
        this.noPermitKerja = noPermitKerja;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNoTelefonBimbit() {
        return noTelefonBimbit;
    }

    public void setNoTelefonBimbit(String noTelefonBimbit) {
        this.noTelefonBimbit = noTelefonBimbit;
    }

    public String getJawatan() {
        return jawatan;
    }

    public void setJawatan(String jawatan) {
        this.jawatan = jawatan;
    }

    public String getNoPendaftaranKenderaan() {
        return noPendaftaranKenderaan;
    }

    public void setNoPendaftaranKenderaan(String noPendaftaranKenderaan) {
        this.noPendaftaranKenderaan = noPendaftaranKenderaan;
    }

    public Permohonan getPermohonan() {
        return permohonan;
    }

    public void setPermohonan(Permohonan permohonan) {
        this.permohonan = permohonan;
    }
}
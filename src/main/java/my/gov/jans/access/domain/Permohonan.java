package my.gov.jans.access.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
public class Permohonan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_no")
    private String nomborPermohonan;

    @Column(name = "email_wakil")
    private String emailWakil;

    @Column(name = "phone_office")
    private String phoneOffice;

    @Column(name = "application_date")
    private LocalDate applicationDate;

    private String organisation;
    private LocalDate visitDate;
    private LocalTime visitTime;

    @Enumerated(EnumType.STRING)
    private JenisLokasi locationType;

    private String locationName;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Enumerated(EnumType.STRING)
    private StatusPermohonan status;

    @Column(columnDefinition = "TEXT")
    private String staffNote;

    @Column(columnDefinition = "TEXT")
    private String directorNote;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Pengguna reviewedBy;

    @ManyToOne
    @JoinColumn(name = "decided_by")
    private Pengguna decidedBy;

    private LocalDateTime decisionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String passToken;

    @OneToMany(mappedBy = "permohonan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<Pelawat> pelawat = new ArrayList<>();

    @PrePersist
    void baru() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void kemasKini() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getNomborPermohonan() {
        return nomborPermohonan;
    }

    public void setNomborPermohonan(String v) {
        nomborPermohonan = v;
    }

    public String getApplicantName() {
        return pelawat.isEmpty() ? null : pelawat.get(0).getNamaPenuh();
    }

    public String getIcNo() {
        return pelawat.isEmpty() ? null : pelawat.get(0).getNoKadPengenalan();
    }

    public String getPhoneMobile() {
        return getPhone();
    }

    public String getJawatanGred() {
        return pelawat.isEmpty() ? null : pelawat.get(0).getJawatan();
    }

    public String getVehicleNo() {
        return pelawat.isEmpty() ? null : pelawat.get(0).getNoPendaftaranKenderaan();
    }

    public String getEmail() {
        return emailWakil != null ? emailWakil : (pelawat.isEmpty() ? null : pelawat.get(0).getEmail());
    }

    public String getEmailWakil() {
        return emailWakil;
    }

    public void setEmailWakil(String v) {
        emailWakil = v;
    }

    public String getPhoneOffice() {
        return phoneOffice;
    }

    public void setPhoneOffice(String v) {
        phoneOffice = v;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate v) {
        applicationDate = v;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String v) {
        organisation = v;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate v) {
        visitDate = v;
    }

    public LocalTime getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(LocalTime v) {
        visitTime = v;
    }

    public JenisLokasi getLocationType() {
        return locationType;
    }

    public void setLocationType(JenisLokasi v) {
        locationType = v;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String v) {
        locationName = v;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String v) {
        purpose = v;
    }

    public StatusPermohonan getStatus() {
        return status;
    }

    public void setStatus(StatusPermohonan v) {
        status = v;
    }

    public String getStaffNote() {
        return staffNote;
    }

    public void setStaffNote(String v) {
        staffNote = v;
    }

    public String getDirectorNote() {
        return directorNote;
    }

    public void setDirectorNote(String v) {
        directorNote = v;
    }

    public Pengguna getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Pengguna v) {
        reviewedBy = v;
    }

    public Pengguna getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Pengguna v) {
        decidedBy = v;
    }

    public LocalDateTime getDecisionAt() {
        return decisionAt;
    }

    public void setDecisionAt(LocalDateTime v) {
        decisionAt = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }

    public String getPassToken() {
        return passToken;
    }

    public void setPassToken(String v) {
        passToken = v;
    }

    public List<Pelawat> getPelawat() {
        return pelawat;
    }

    public void setPelawat(List<Pelawat> pelawat) {
        this.pelawat.clear();
        if (pelawat != null) {
            pelawat.forEach(this::tambahPelawat);
        }
    }

    public void tambahPelawat(Pelawat pelawat) {
        pelawat.setPermohonan(this);
        this.pelawat.add(pelawat);
    }

    public String getPhone() {
        return pelawat.isEmpty() ? null : pelawat.get(0).getNoTelefonBimbit();
    }

    public void setPhone(String v) {
        // Telefon pemohon disimpan dalam application_visitors.
    }
}
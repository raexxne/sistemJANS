package my.gov.jans.access.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "lokasi")
public class Lokasi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private JenisLokasi type;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String daerah;

    public Lokasi() {
    }

    public Lokasi(JenisLokasi type, String name, String daerah) {
        this.type = type;
        this.name = name;
        this.daerah = daerah;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JenisLokasi getType() {
        return type;
    }

    public void setType(JenisLokasi type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDaerah() {
        return daerah;
    }

    public void setDaerah(String daerah) {
        this.daerah = daerah;
    }
}

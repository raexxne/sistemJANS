package my.gov.jans.access.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "penyelia_loji")
public class PenyeliaLoji {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Pengguna pengguna;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "penyelia_loji_daerah", joinColumns = @JoinColumn(name = "penyelia_loji_id"))
    @Column(name = "daerah", nullable = false)
    private List<String> daerahSeliaan = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pengguna getPengguna() {
        return pengguna;
    }

    public void setPengguna(Pengguna pengguna) {
        this.pengguna = pengguna;
    }

    public List<String> getDaerahSeliaan() {
        return daerahSeliaan;
    }

    public void setDaerahSeliaan(List<String> daerahSeliaan) {
        this.daerahSeliaan.clear();
        if (daerahSeliaan != null) {
            this.daerahSeliaan.addAll(daerahSeliaan);
        }
    }
}

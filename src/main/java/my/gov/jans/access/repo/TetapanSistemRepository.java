package my.gov.jans.access.repo;

import my.gov.jans.access.domain.TetapanSistem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TetapanSistemRepository extends JpaRepository<TetapanSistem, Long> {
    List<TetapanSistem> findAllByCategoryOrderByIdAsc(String category);

    Optional<TetapanSistem> findByCategoryAndSettingKey(String category, String settingKey);

    boolean existsByCategoryAndSettingKey(String category, String settingKey);
}

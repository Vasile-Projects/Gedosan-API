package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.LogModificaDonatore;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogModificaDonatoreRepository extends JpaRepository<LogModificaDonatore, Long> {

    @EntityGraph(attributePaths = {"admin"})
    List<LogModificaDonatore> findAllByDonatore_IdOrderByTimestampDescIdDesc(Long idDonatore);
}

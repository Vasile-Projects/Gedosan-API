package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.OrarioValido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;

public interface OrarioValidoRepository extends JpaRepository<OrarioValido, LocalTime> {

    List<OrarioValido> findAllByOrderByOrarioAsc();
}

package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.VariazioneApertura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VariazioneAperturaRepository extends JpaRepository<VariazioneApertura, Long> {

    // Più variazioni per stessa data sono ammesse: vince l'ultima inserita, da cui l'ordine per id desc.
    Optional<VariazioneApertura> findFirstByTrasfusionale_IdAndDataVariazioneOrderByIdDesc(Long idTrasfusionale, LocalDate data);

    List<VariazioneApertura> findAllByTrasfusionale_IdOrderByDataVariazioneDesc(Long idTrasfusionale);

    List<VariazioneApertura> findAllByOrderByDataVariazioneDesc();
}

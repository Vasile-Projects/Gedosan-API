package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.FestivoRicorrente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivoRicorrenteRepository extends JpaRepository<FestivoRicorrente, Integer> {

    boolean existsByMeseAndGiorno(Integer mese, Integer giorno);
}

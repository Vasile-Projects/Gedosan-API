-- Difesa in profondità contro una doppia prenotazione dello stesso donatore sullo stesso slot:
-- oltre ai controlli applicativi (lock sul donatore + validaIntervallo), il DB rifiuta il duplicato.
-- L'indice non-unique diventa ridondante (lo UNIQUE fornisce lo stesso indice, e continua a coprire
-- la FK fk_prenotazioni_donatore che inizia con id_donatore).
-- Già applicato a mano in produzione (Flyway è disabilitato lì): questa migrazione allinea solo gli
-- ambienti gestiti da Flyway (sviluppo locale).
ALTER TABLE `prenotazioni`
    DROP INDEX `idx_prenotazioni_donatore_slot`,
    ADD CONSTRAINT `uq_prenotazioni_donatore_slot` UNIQUE (`id_donatore`, `id_slot`);

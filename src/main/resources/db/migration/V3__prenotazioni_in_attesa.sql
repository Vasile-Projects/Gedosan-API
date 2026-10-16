-- Prenotazioni in attesa di conferma via email. La POST pubblica non crea più donatore e prenotazione:
-- salva qui una copia dei dati del modulo e occupa il posto fino a `scadenza`. Donatore e prenotazione
-- nascono solo quando il link di conferma viene usato, così chi non possiede l'email non può legare un
-- codice fiscale a un indirizzo né prenotare a nome di altri.
-- Nessuna FK verso `donatori`: il collegamento avviene alla conferma. Le righe scadute vanno cancellate
-- fisicamente dal job di scadenza (contengono dati personali e la FK sullo slot è RESTRICT).
CREATE TABLE `prenotazioni_in_attesa` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `id_slot` bigint unsigned NOT NULL,
  `nome` varchar(60) NOT NULL,
  `cognome` varchar(120) NOT NULL,
  `data_nascita` date NOT NULL,
  `sesso` char(1) NOT NULL,
  `codice_fiscale` char(16) NOT NULL,
  `email` varchar(255) NOT NULL,
  `cellulare` varchar(15) NOT NULL,
  `tipo_donazione` char(2) NOT NULL DEFAULT 'SI',
  -- SHA-256 esadecimale del token inviato per email: il token in chiaro non viene mai salvato.
  `token_hash` char(64) NOT NULL,
  `scadenza` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_attesa_token_hash` (`token_hash`),
  -- Una sola attesa per codice fiscale: una nuova richiesta sostituisce la precedente.
  UNIQUE KEY `uq_attesa_codice_fiscale` (`codice_fiscale`),
  -- Conteggio dei posti occupati (confermate + attese non scadute) e job di scadenza.
  KEY `idx_attesa_slot_scadenza` (`id_slot`, `scadenza`),
  KEY `idx_attesa_scadenza` (`scadenza`),
  CONSTRAINT `fk_attesa_slot` FOREIGN KEY (`id_slot`) REFERENCES `slot_giornalieri` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_attesa_sesso` CHECK ((`sesso` in (_utf8mb4'M',_utf8mb4'F'))),
  CONSTRAINT `chk_attesa_cellulare` CHECK (regexp_like(`cellulare`,_utf8mb4'^\\+39[0-9]{9,10}$')),
  CONSTRAINT `chk_attesa_tipo_donazione` CHECK ((`tipo_donazione` = _utf8mb4'SI'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

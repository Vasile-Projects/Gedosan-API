-- Bot Telegram: collegamento tra una chat e un donatore già registrato, verificato con un codice inviato
-- all'email registrata. Una volta collegata, la chat prenota senza ripassare dal link via email.

-- Codice monouso in attesa di verifica: uno per chat (una nuova richiesta sostituisce la precedente).
-- Si salva solo l'hash del codice; dopo `max tentativi` errati il codice non vale più.
CREATE TABLE `codici_collegamento` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `chat_id` bigint NOT NULL,
  `id_donatore` bigint unsigned NOT NULL,
  `codice_hash` char(64) NOT NULL,
  `tentativi` int NOT NULL DEFAULT '0',
  `scadenza` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_codici_chat` (`chat_id`),
  KEY `idx_codici_donatore_created` (`id_donatore`, `created_at`),
  KEY `idx_codici_scadenza` (`scadenza`),
  CONSTRAINT `fk_codici_donatore` FOREIGN KEY (`id_donatore`) REFERENCES `donatori` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Chat collegata a un donatore fino a `scadenza` (6 mesi). Un donatore può avere più chat collegate,
-- una chat un solo donatore.
CREATE TABLE `collegamenti_telegram` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `chat_id` bigint NOT NULL,
  `id_donatore` bigint unsigned NOT NULL,
  `scadenza` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_collegamenti_chat` (`chat_id`),
  KEY `idx_collegamenti_scadenza` (`scadenza`),
  CONSTRAINT `fk_collegamenti_donatore` FOREIGN KEY (`id_donatore`) REFERENCES `donatori` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Canale da cui è nata la prenotazione. NULL per le prenotazioni precedenti a questa migrazione.
ALTER TABLE `prenotazioni`
  ADD COLUMN `canale` varchar(10) DEFAULT NULL,
  ADD COLUMN `promemoria_inviato_at` datetime DEFAULT NULL,
  ADD CONSTRAINT `chk_prenotazioni_canale` CHECK ((`canale` in (_utf8mb4'SITO',_utf8mb4'ADMIN',_utf8mb4'TELEGRAM')));

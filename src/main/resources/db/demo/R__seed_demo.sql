-- Dati DEMO per sviluppo/test locale. NON caricato in produzione (vedi spring.flyway.locations).
-- Migrazione "repeatable": rieseguita a ogni avvio se cambia il contenuto. Tutti gli insert sono idempotenti.
-- Valori inventati, nessun dato reale.

-- Admin demo -> login: username "demo", password "demo1234"
INSERT INTO `admin` (`username`, `password_hash`, `nome`, `cognome`)
VALUES ('demo', '$2a$10$nItHItyhaeG7rk9z87bU1uymeFlYPciZ1A8bAk9YYwlzZSWV4pAmK', 'Demo', 'Admin')
ON DUPLICATE KEY UPDATE
  `password_hash` = VALUES(`password_hash`),
  `nome` = VALUES(`nome`),
  `cognome` = VALUES(`cognome`);

-- Fasce orarie prenotabili
INSERT IGNORE INTO `orari_validi` (`ora`) VALUES
  ('08:00:00'), ('08:30:00'), ('09:00:00'), ('09:30:00'), ('10:00:00'), ('10:30:00'), ('11:00:00');

-- Centri trasfusionali (chiave unica: telefono)
INSERT INTO `trasfusionali` (`nome`, `citta`, `indirizzo`, `civico`, `telefono`)
VALUES ('Centro Trasfusionale Demo Nord', 'Roma', 'Via Alfa', 10, '0600000001')
ON DUPLICATE KEY UPDATE `nome` = VALUES(`nome`), `indirizzo` = VALUES(`indirizzo`), `civico` = VALUES(`civico`);

INSERT INTO `trasfusionali` (`nome`, `citta`, `indirizzo`, `civico`, `telefono`)
VALUES ('Centro Trasfusionale Demo Sud', 'Roma', 'Via Beta', 5, '0600000002')
ON DUPLICATE KEY UPDATE `nome` = VALUES(`nome`), `indirizzo` = VALUES(`indirizzo`), `civico` = VALUES(`civico`);

-- Festivi nazionali italiani a data fissa (chiave unica: mese+giorno)
INSERT IGNORE INTO `festivi_ricorrenti` (`mese`, `giorno`, `descrizione`) VALUES
  (1, 1, 'Capodanno'),
  (1, 6, 'Epifania'),
  (4, 25, 'Festa della Liberazione'),
  (5, 1, 'Festa del Lavoro'),
  (6, 2, 'Festa della Repubblica'),
  (8, 15, 'Ferragosto'),
  (11, 1, 'Ognissanti'),
  (12, 8, 'Immacolata Concezione'),
  (12, 25, 'Natale'),
  (12, 26, 'Santo Stefano');

-- Gli slot giornalieri e i donatori vengono creati dall'applicazione durante il flusso di prenotazione.

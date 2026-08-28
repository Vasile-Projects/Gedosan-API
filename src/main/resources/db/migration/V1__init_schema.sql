-- Schema iniziale Gedosan (allineato alle entity JPA, ddl-auto=validate)

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `admin` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(60) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `nome` varchar(60) NOT NULL,
  `cognome` varchar(60) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `donatori` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nome` varchar(60) NOT NULL,
  `cognome` varchar(120) NOT NULL,
  `data_nascita` date NOT NULL,
  `sesso` char(1) NOT NULL,
  `codice_fiscale` char(16) NOT NULL,
  `email` varchar(255) NOT NULL,
  `cellulare` varchar(15) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `uq_donatori_codice_fiscale` (`codice_fiscale`),
  CONSTRAINT `chk_donatori_cellulare` CHECK (regexp_like(`cellulare`,_utf8mb4'^\\+39[0-9]{9,10}$')),
  CONSTRAINT `chk_donatori_sesso` CHECK ((`sesso` in (_utf8mb4'M',_utf8mb4'F')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `festivi_ricorrenti` (
  `id` int NOT NULL AUTO_INCREMENT,
  `mese` int NOT NULL,
  `giorno` int NOT NULL,
  `descrizione` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_festivi_mese_giorno` (`mese`,`giorno`),
  CONSTRAINT `chk_festivi_giorno` CHECK ((`giorno` between 1 and 31)),
  CONSTRAINT `chk_festivi_mese` CHECK ((`mese` between 1 and 12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `log_email` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `id_prenotazione` bigint unsigned NOT NULL,
  `esito` varchar(3) NOT NULL,
  `messaggio_errore` varchar(500) DEFAULT NULL,
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_log_email_esito` CHECK ((`esito` in (_utf8mb4'OK',_utf8mb4'KO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `log_modifiche_prenotazioni` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `admin_id` bigint unsigned NOT NULL,
  `azione` varchar(20) NOT NULL,
  `id_prenotazione` bigint unsigned NOT NULL,
  `id_slot_vecchio` bigint unsigned NOT NULL,
  `id_slot_nuovo` bigint unsigned DEFAULT NULL,
  `donatore_nome` varchar(60) DEFAULT NULL,
  `donatore_cognome` varchar(120) DEFAULT NULL,
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_log_admin` (`admin_id`),
  CONSTRAINT `fk_log_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_log_azione` CHECK ((`azione` in (_utf8mb4'CANCELLAZIONE',_utf8mb4'RIPROGRAMMAZIONE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `orari_validi` (
  `ora` time NOT NULL,
  PRIMARY KEY (`ora`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `trasfusionali` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nome` varchar(255) NOT NULL,
  `citta` char(4) NOT NULL DEFAULT 'Roma',
  `indirizzo` varchar(255) NOT NULL,
  `civico` int NOT NULL,
  `telefono` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `telefono` (`telefono`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `slot_giornalieri` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `id_centro` bigint unsigned NOT NULL,
  `ora` time NOT NULL,
  `data` date NOT NULL,
  `posti_occupati` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_slot_centro_ora_data` (`id_centro`,`ora`,`data`),
  KEY `fk_slot_ora` (`ora`),
  CONSTRAINT `fk_slot_centro` FOREIGN KEY (`id_centro`) REFERENCES `trasfusionali` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_slot_ora` FOREIGN KEY (`ora`) REFERENCES `orari_validi` (`ora`) ON DELETE RESTRICT,
  CONSTRAINT `chk_slot_posti` CHECK ((`posti_occupati` between 0 and 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `prenotazioni` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `id_donatore` bigint unsigned NOT NULL,
  `id_slot` bigint unsigned NOT NULL,
  `tipo_donazione` char(2) NOT NULL DEFAULT 'SI',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_prenotazioni_slot` (`id_slot`),
  KEY `idx_prenotazioni_donatore_slot` (`id_donatore`,`id_slot`),
  CONSTRAINT `fk_prenotazioni_donatore` FOREIGN KEY (`id_donatore`) REFERENCES `donatori` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_prenotazioni_slot` FOREIGN KEY (`id_slot`) REFERENCES `slot_giornalieri` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_prenotazioni_tipo_donazione` CHECK ((`tipo_donazione` = _utf8mb4'SI'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `variazioni_apertura` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `id_centro` bigint unsigned NOT NULL,
  `data` date NOT NULL,
  `aperto` tinyint(1) NOT NULL,
  `motivo` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_variazioni_apertura_data` (`id_centro`,`data`),
  CONSTRAINT `fk_variazioni_apertura` FOREIGN KEY (`id_centro`) REFERENCES `trasfusionali` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

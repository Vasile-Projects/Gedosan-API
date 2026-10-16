-- Audit delle modifiche all'anagrafica dei donatori fatte dall'area admin. Si registra QUALI campi sono
-- cambiati, non i valori vecchi/nuovi, per non accumulare copie di dati personali nel log.
CREATE TABLE `log_modifiche_donatori` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `admin_id` bigint unsigned NOT NULL,
  `id_donatore` bigint unsigned NOT NULL,
  -- Nomi dei campi separati da virgola, es. "email,cellulare".
  `campi_modificati` varchar(255) NOT NULL,
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_logdon_admin` (`admin_id`),
  KEY `idx_logdon_donatore` (`id_donatore`),
  CONSTRAINT `fk_logdon_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_logdon_donatore` FOREIGN KEY (`id_donatore`) REFERENCES `donatori` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

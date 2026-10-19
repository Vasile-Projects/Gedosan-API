# Bot Telegram con n8n

Il bot è un workflow n8n che usa gli endpoint `/api/bot/**` dell'API. Questa cartella contiene il compose di n8n e il workflow da importare.

```
Telegram ──webhook (token segreto)──▶ n8n ──rete interna, X-Bot-Key──▶ gedosan-api ──▶ MySQL
                                       └── AI Agent (Gemini o Claude) per le domande libere
```

## Come funziona il workflow

| Messaggio | Ramo | Passa dall'AI? |
|---|---|---|
| `/start`, `/aiuto`, `/collega` | testo fisso | no |
| codice fiscale + email | `POST /api/bot/collegamento/richiesta` (risposta sempre uguale) | no |
| 6 cifre | `POST /api/bot/collegamento/verifica` | no |
| CF, email o numeri dentro altre frasi | avviso privacy | no |
| `/esci` | `DELETE /api/bot/collegamento/{chatId}` | no |
| pulsanti (orario, Conferma, Annulla) | riepilogo e `POST /api/bot/chat/{chatId}/prenotazioni` | no |
| tutto il resto | AI Agent con gli strumenti `centri`, `giorni_chiusi`, `orari_liberi`, `mio_stato`, `mie_prenotazioni` | sì |

- Il `chatId` lo prende il nodo **Normalizza** dall'aggiornamento di Telegram. n8n accetta solo gli aggiornamenti con il token segreto registrato sul webhook, quindi nessuno può spacciarsi per un'altra chat. Gli strumenti personali inseriscono il `chatId` da soli; il modello sceglie solo centro, mese e giorno.
- **L'AI non prenota.** Quando ha un centro e un giorno con posti liberi chiude la risposta con `PROPONI_ORARI|<id centro>|<data>`. I pulsanti li costruisce il codice, e la prenotazione parte solo dal tocco su **Conferma**.
- Modello: **Google Gemini** (piano gratuito): `gemini-2.5-flash` nel nodo **Modello AI**, con `gemini-3-flash-preview` nel nodo **Modello di riserva**, usato in automatico se il primo dà errore (per esempio quota giornaliera esaurita). La quota è per progetto e per modello, condivisa da tutti gli utenti del bot. Per passare a Claude si sostituisce solo quel nodo con *Anthropic Chat Model* (`claude-opus-5-5`, thinking adaptive, effort `low`): istruzioni, strumenti e memoria restano uguali. Nel piano gratuito Google può usare i messaggi per migliorare i modelli; qui è accettabile perché CF, email e codici non arrivano mai all'AI.
- La memoria della chat (ultimi 10 scambi) sta nella RAM di n8n e si azzera al riavvio.

## Messa in produzione

1. **Bot.** Su Telegram, con @BotFather: `/newbot`, poi salva il token. Con `/setcommands` imposta:
   ```
   start - Cosa posso fare
   collega - Collega i tuoi dati di donatore
   esci - Scollega questa chat
   ```
2. **API.** Crea il secret GitHub `BOT_API_KEY` (`openssl rand -base64 32`) e la variabile `BOT_TELEGRAM_URL` (`https://t.me/<nome del bot>`), poi fai il merge su `main`.
3. **n8n.** Sul server:
   ```bash
   cd deploy/n8n && cp .env.example .env   # inserisci TELEGRAM_BOT_TOKEN
   docker compose up -d
   ```
4. **Esposizione.**
   - In Nginx Proxy Manager crea un proxy host `n8n.vszdev.it` → `<server>:8085`, con Websockets attivi.
   - Nel tunnel Cloudflare aggiungi l'hostname pubblico `n8n.vszdev.it` con **Path** `webhook/.*`, verso NPM come per gli altri sottodomini. Da internet si raggiunge così solo `/webhook/…`.
   - L'editor si apre dalla LAN su `http://<server>:8085`.
5. **Credenziali in n8n.** Dall'editor crea le tre credenziali (il nome non conta: dopo l'import si assegnano nodo per nodo):
   - **Telegram - Gedosan bot** (Telegram API): il token del bot;
   - **Gedosan API - X-Bot-Key** (Header Auth): nome `X-Bot-Key`, valore uguale a `BOT_API_KEY`;
   - **Google Gemini(PaLM) API**: la chiave creata su aistudio.google.com.
6. **Workflow.** Importa `gedosan-bot.workflow.json`, controlla che ogni nodo abbia la sua credenziale e attivalo. All'attivazione n8n registra da solo il webhook su Telegram.

## Limiti consapevoli

- Per i messaggi con pulsanti il token del bot sta anche in una variabile d'ambiente, perché il nodo Telegram non costruisce tastiere con un numero variabile di bottoni. Questo richiede `N8N_BLOCK_ENV_ACCESS_IN_NODE=false`, e per questo la chiave di cifratura di n8n non è passata come variabile.
- Non c'è un limite di messaggi verso l'AI per singola chat: l'API limita solo le proprie chiamate. Con Gemini gratuito il tetto lo mettono i limiti del piano; con Claude conviene impostare un tetto mensile nella console Anthropic.
- Il canale si sostituisce cambiando solo trigger e nodi di invio: per esempio WhatsApp Business al posto di Telegram, con lo stesso agente e gli stessi strumenti.

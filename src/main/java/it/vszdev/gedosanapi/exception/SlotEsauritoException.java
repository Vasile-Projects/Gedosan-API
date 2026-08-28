package it.vszdev.gedosanapi.exception;

public class SlotEsauritoException extends RuntimeException {

    public SlotEsauritoException() {
        super("Lo slot selezionato non è più disponibile. Scegli un altro orario.");
    }
}

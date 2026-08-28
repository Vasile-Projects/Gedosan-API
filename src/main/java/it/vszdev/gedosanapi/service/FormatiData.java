package it.vszdev.gedosanapi.service;

import java.time.format.DateTimeFormatter;

final class FormatiData {

    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter ORARIO = DateTimeFormatter.ofPattern("HH:mm");

    private FormatiData() {
    }
}

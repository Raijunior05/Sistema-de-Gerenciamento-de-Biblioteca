package br.univasf.bibliotech.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Formatacao de datas no padrao brasileiro usado no prototipo (dd/MM/yyyy). */
public final class Datas {

    public static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Datas() {
    }

    public static String formatar(LocalDate data) {
        return data == null ? "-" : data.format(BR);
    }

    public static LocalDate converter(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return LocalDate.parse(texto.trim(), BR);
    }

    /**
     * Monta a data a partir dos tres campos separados do prototipo
     * (Dia, Mes, Ano na tela de cadastro).
     */
    public static LocalDate de(String dia, String mes, String ano) {
        if (dia == null || mes == null || ano == null
                || dia.isBlank() || mes.isBlank() || ano.isBlank()) {
            return null;
        }
        return LocalDate.of(
                Integer.parseInt(ano.trim()),
                Integer.parseInt(mes.trim()),
                Integer.parseInt(dia.trim()));
    }
}

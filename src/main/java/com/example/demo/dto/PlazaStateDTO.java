package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar al frontend el estado de cada plaza en un mapeo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlazaStateDTO {
    private Long plazaId;
    private String codigo;         // "A1", "B2", ...
    private String estado;         // "ROJO", "VERDE", "AZUL", "BLANCO"
    private boolean ocupada;       // true si está ocupada actualmente
    private boolean borde;         // candidada a AZUL si unsure
    // opcionales: fila/columna para posicionamiento en grid
    private Integer fila;
    private Integer columna;
}

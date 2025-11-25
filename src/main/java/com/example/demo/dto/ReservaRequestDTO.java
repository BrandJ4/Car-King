package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recibir los datos necesarios para crear una nueva Reserva y Boleta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor

public class ReservaRequestDTO {
    private Long plazaId;
    private String horaIngreso; // Formato ISO: "YYYY-MM-DDTHH:MM:SS"
    private String horaSalida;  // Formato ISO: "YYYY-MM-DDTHH:MM:SS" (puede ser null)
    private boolean unsure;
    private String nombreConductor;
    private String placaVehiculo;
    private String metodoPago; // "Efectivo", "Tarjeta", "Yape" (solo si no es unsure)
}


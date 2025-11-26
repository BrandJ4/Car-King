package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Plaza plaza;

    private LocalDateTime horaIngreso;
    private LocalDateTime horaSalida;
    
    // Nuevos campos para el estado de la reserva y el pago
    private boolean activa; // true si el coche está en la cochera
    private boolean pagado; // true si ya se pagó (anticipado o al salir)
    private String metodoPago; // Efectivo_Pendiente, Tarjeta, Yape
    private boolean unsure; // Si el conductor no estaba seguro de su salida (pago al salir)
}
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Reserva reserva;

    private double monto;
    private LocalDateTime fechaEmision;
    private String metodoPago; // efectivo, tarjeta, Yape, etc.
}

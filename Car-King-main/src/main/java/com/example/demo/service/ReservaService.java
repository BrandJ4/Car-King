package com.example.demo.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.entity.Reserva;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {
    private final ReservaRepository reservaRepository;

    public List<Reserva> obtenerTodas() {
        return reservaRepository.findAll();
    }

    public Reserva guardar(Reserva reserva) {
        return reservaRepository.save(reserva);
    }
}

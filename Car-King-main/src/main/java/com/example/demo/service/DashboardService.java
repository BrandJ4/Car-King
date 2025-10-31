package com.example.demo.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.repository.BoletaRepository;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ReservaRepository reservaRepository;
    private final BoletaRepository boletaRepository;

    public long totalReservas() {
        return reservaRepository.count();
    }

    public long totalBoletas() {
        return boletaRepository.count();
    }
}

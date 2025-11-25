package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Cochera;
import com.example.demo.repository.CocheraRepository;

import lombok.RequiredArgsConstructor;
import lombok.NonNull; // <-- ¡AÑADIDO!

@Service
@RequiredArgsConstructor
@Transactional
public class CocheraService {

    private final CocheraRepository cocheraRepository;

    public List<Cochera> listarTodas() {
        return cocheraRepository.findAll();
    }

    // Fija la advertencia de 'Long' (Línea 26)
    public Optional<Cochera> obtenerPorId(@NonNull Long id) {
        return cocheraRepository.findById(id);
    }

    // Fija la advertencia de 'Cochera' (Línea 30)
    public Cochera guardar(@NonNull Cochera c) {
        return cocheraRepository.save(c);
    }

    public void eliminar(@NonNull Long id) {
        cocheraRepository.deleteById(id);
    }
}

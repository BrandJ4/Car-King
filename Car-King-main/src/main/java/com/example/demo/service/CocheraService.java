package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Cochera;
import com.example.demo.repository.CocheraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CocheraService {

    private final CocheraRepository cocheraRepository;

    public List<Cochera> listarTodas() {
        return cocheraRepository.findAll();
    }

    public Optional<Cochera> obtenerPorId(Long id) {
        return cocheraRepository.findById(id);
    }

    public Cochera guardar(Cochera c) {
        return cocheraRepository.save(c);
    }

    public void eliminar(Long id) {
        cocheraRepository.deleteById(id);
    }
}

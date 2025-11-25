package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.NonNull; // <-- ¡AÑADIDO!

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // Aplicamos @NonNull (solucionando líneas 22, 39, 43, etc.)
    public Usuario crear(@NonNull Usuario u) {
        return usuarioRepository.save(u);
    }

    public Optional<Usuario> findById(@NonNull Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario findByNombre(@NonNull String nombre) {
        return usuarioRepository.findByNombre(nombre);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }
}
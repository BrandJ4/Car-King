package com.example.demo.util;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.Reserva;
import com.example.demo.entity.Plaza;

public class ColorUtils {

    public enum Estado { ROJO, VERDE, AZUL }

    /**
     * Determina si dos intervalos de tiempo se intersectan.
     * Nota: si uno de los extremos (salida) es null se trata como indefinido/abierto.
     */
    public static boolean intervalosSeCruzan(LocalDateTime aInicio, LocalDateTime aFin,
                                            LocalDateTime bInicio, LocalDateTime bFin) {
        // si alguna entrada es null consideramos cruce conservadoramente
        if (aInicio == null || bInicio == null) return true;
        if (aFin == null && bFin == null) return true;
        // Normalizamos: si fin == null lo tratamos como infinito (cruce si la otra empieza después del inicio)
        if (aFin == null && bFin != null) {
            return !bFin.isBefore(aInicio);
        }
        if (bFin == null && aFin != null) {
            return !aFin.isBefore(bInicio);
        }
        if (aFin == null && bFin == null) return true;

        // interval overlap standard: (startA <= endB) and (startB <= endA)
        return !aInicio.isAfter(bFin) && !bInicio.isAfter(aFin);
    }

    /**
     * Revisar si hay reservas que cruce con el nuevo intervalo.
     * Si existe cruce -> ROJO
     * Si no hay cruces y plaza es borde y unsure==true -> AZUL
     * Si no hay cruces -> VERDE
     */
    public static Estado calcularEstadoPlaza(Plaza plaza,
                                            List<Reserva> reservasExistentes,
                                            LocalDateTime nuevoIngreso,
                                            LocalDateTime nuevaSalida,
                                            boolean unsure) {
        for (Reserva r : reservasExistentes) {
            LocalDateTime rInicio = r.getHoraIngreso();
            LocalDateTime rFin = r.getHoraSalida();
            if (intervalosSeCruzan(rInicio, rFin, nuevoIngreso, nuevaSalida)) {
                return Estado.ROJO;
            }
        }

        if (unsure && plaza != null && plaza.isOcupada() == false && plaza.isBorde()) {
            return Estado.AZUL;
        }
        return Estado.VERDE;
    }
}

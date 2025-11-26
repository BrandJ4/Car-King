// Variable para almacenar la plaza seleccionada
let selectedPlaza = null;
const PRECIO_POR_HORA = 5.0; // Consistente con el backend

document.addEventListener("DOMContentLoaded", () => {
    const cocheraId = sessionStorage.getItem("cocheraId");
    const horaIngreso = sessionStorage.getItem("horaIngreso");
    const horaSalida = sessionStorage.getItem("horaSalida");
    const unsure = sessionStorage.getItem("unsure") === 'true';

    // Construir la URL con las horas y el estado 'unsure'
    const params = new URLSearchParams({
        horaIngreso: getCurrentDateTimeString(horaIngreso),
        horaSalida: horaSalida ? getCurrentDateTimeString(horaSalida) : '',
        unsure: unsure
    });

    fetch(`/api/cocheras/${cocheraId}/plazas?${params.toString()}`)
        .then(r => r.json())
        .then(data => {
            const cont = document.getElementById("mapeo");
            const maxColumna = data.reduce((max, p) => Math.max(max, p.columna || 0), 0);
            cont.style.gridTemplateColumns = `repeat(${maxColumna}, 50px)`;
            
            data.forEach(plaza => {
                const div = document.createElement("div");
                div.classList.add("plaza");
                div.classList.add(plaza.estado.toLowerCase());
                div.textContent = plaza.codigo;
                div.dataset.plazaId = plaza.plazaId;
                div.dataset.estado = plaza.estado;
                div.dataset.codigo = plaza.codigo;
                
                if (plaza.fila && plaza.columna) {
                    div.style.gridRowStart = plaza.fila;
                    div.style.gridColumnStart = plaza.columna;
                }

                if (plaza.estado === 'VERDE' || plaza.estado === 'AZUL') {
                    div.onclick = () => seleccionar(div, plaza);
                    div.style.cursor = 'pointer';
                } else {
                    div.style.cursor = 'not-allowed';
                }

                cont.appendChild(div);
            });
        })
        .catch(err => {
             document.getElementById("mapeo").innerHTML = '<p style="color: red;">Error al cargar las plazas.</p>';
             console.error(err);
        });
});

/**
 * Convierte la hora ingresada (HH:MM) a un formato completo de fecha y hora ISO (YYYY-MM-DDTHH:MM:SS)
 */
function getCurrentDateTimeString(timeStr) {
    if (!timeStr) return '';
    const now = new Date();
    const [hours, minutes] = timeStr.split(':').map(Number);
    now.setHours(hours, minutes, 0, 0);
    return now.toISOString().slice(0, 19);
}

function seleccionar(div, plazaData) {
    document.querySelectorAll(".plaza").forEach(p => p.classList.remove("seleccionado"));
    div.classList.add("seleccionado");
    selectedPlaza = plazaData;

    document.getElementById('plazaSeleccionadaDisplay').textContent = plazaData.codigo;
    document.getElementById('plazaEstadoDisplay').textContent = plazaData.estado;
    
    const confirmacionPagoDiv = document.getElementById('confirmacionPago');
    confirmacionPagoDiv.style.display = 'block';

    const unsure = sessionStorage.getItem("unsure") === 'true';
    const horaIngreso = sessionStorage.getItem("horaIngreso");
    const horaSalida = sessionStorage.getItem("horaSalida");
    
    document.getElementById('rangoHoras').textContent = `${horaIngreso} a ${horaSalida || 'Indefinido'}`;

    const seccionPago = document.getElementById('seccionPago');
    const pagoPosterior = document.getElementById('pagoPosterior');
    const btnConfirmar = document.getElementById('btnConfirmar');

    if (unsure || !horaSalida) {
        seccionPago.style.display = 'none';
        pagoPosterior.style.display = 'block';
        btnConfirmar.textContent = 'Confirmar Ingreso (Pago a la Salida)';
    } else {
        seccionPago.style.display = 'block';
        pagoPosterior.style.display = 'none';
        btnConfirmar.textContent = 'Confirmar Reserva y Pagar';

        const inicio = getCurrentDateTimeString(horaIngreso);
        const fin = getCurrentDateTimeString(horaSalida);
        
        const diffMs = new Date(fin).getTime() - new Date(inicio).getTime();
        const diffHours = Math.ceil(diffMs / (1000 * 60 * 60));
        const totalPagar = diffHours > 0 ? diffHours * PRECIO_POR_HORA : PRECIO_POR_HORA;
        
        document.getElementById('totalPagar').textContent = `S/. ${totalPagar.toFixed(2)}`;
    }
}

function cancelarSeleccion() {
    selectedPlaza = null;
    document.querySelectorAll(".plaza").forEach(p => p.classList.remove("seleccionado"));
    document.getElementById('confirmacionPago').style.display = 'none';

    document.getElementById('plazaSeleccionadaDisplay').textContent = 'Ninguna';
    document.getElementById('plazaEstadoDisplay').textContent = '---';
}

function confirmarReserva() {
    if (!selectedPlaza) {
        alert("Por favor, seleccione una plaza.");
        return;
    }
    
    const nombre = sessionStorage.getItem("nombreConductor");
    const placa = sessionStorage.getItem("placaVehiculo");
    const horaIngreso = sessionStorage.getItem("horaIngreso");
    const horaSalida = sessionStorage.getItem("horaSalida");
    const unsure = sessionStorage.getItem("unsure") === 'true';
    const metodoPago = document.getElementById('metodoPago').value;
    
    const horaIngresoISO = getCurrentDateTimeString(horaIngreso);
    const horaSalidaISO = unsure ? null : getCurrentDateTimeString(horaSalida); 

    const reservaData = {
        plazaId: selectedPlaza.plazaId,
        horaIngreso: horaIngresoISO,
        horaSalida: horaSalidaISO,
        unsure: unsure,
        nombreConductor: nombre,
        placaVehiculo: placa,
        metodoPago: metodoPago 
    };

    fetch('/api/reservar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reservaData)
    })
    .then(async response => {
        if (!response.ok) {
            const errorText = await response.text(); 
            try {
                const errorJson = JSON.parse(errorText);
                throw new Error(errorJson.message || errorText);
            } catch (e) {
                throw new Error(errorText || 'Error desconocido al confirmar la reserva.');
            }
        }
        return response.json();
    })
    .then(reserva => {
        document.getElementById('mapeo').style.display = 'none';
        document.getElementById('titulo').style.display = 'none';
        document.getElementById('confirmacionPago').style.display = 'none';
        document.getElementById('reservaExitosa').style.display = 'block';

        const detalleBoleta = document.getElementById('detalleBoleta');
        detalleBoleta.innerHTML = `
            <h3>¡Reserva Exitosa!</h3>
            <p><strong>Cochera:</strong> ${sessionStorage.getItem("cocheraId")}</p>
            <p><strong>Casilla:</strong> ${selectedPlaza.codigo}</p>
            <p><strong>Conductor:</strong> ${nombre}</p>
            <p><strong>Vehículo:</strong> ${placa}</p>
            <p><strong>Ingreso:</strong> ${horaIngreso}</p>
            ${horaSalida ? `<p><strong>Salida Estimada:</strong> ${horaSalida}</p>` : `<p><strong>Pago:</strong> Se realizará al salir</p>`}
        `;
    })
    .catch(error => {
        alert("ERROR EN RESERVA: " + error.message);
        console.error("Error al confirmar reserva:", error);
    });
}

function descargarBoleta() {
    alert("Función de descarga en desarrollo.");
}

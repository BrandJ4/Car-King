// Variables Globales
const cocheraId = sessionStorage.getItem('cocheraId');
const cocheraNombre = sessionStorage.getItem('cocheraNombre');
let reservaEnCheckout = null;

const PRECIO_POR_HORA = 5.0; // Consistente con el backend

document.addEventListener("DOMContentLoaded", () => {
    // Verificación de sesión e inicialización de UI
    if (!cocheraId) {
        window.location.href = '/recepcion/login';
        return;
    }
    
    document.getElementById('cocheraNombreDisplay').textContent = cocheraNombre;
    document.getElementById('nombreRecepcionista').textContent = `Usuario: ${sessionStorage.getItem('nombreUsuario') || 'Recepcionista'}`;
    
    // Carga inicial
    mostrarSeccion('mapeo'); 
});


// --- Funciones de Ayuda ---

/** Convierte la hora ingresada (HH:MM) a un formato completo de fecha y hora ISO (YYYY-MM-DDTHH:MM:SS) */
function getNowDateTimeString(timeStr) {
    if (!timeStr) return '';
    const now = new Date();
    const [hours, minutes] = timeStr.split(':').map(Number);
    now.setHours(hours, minutes, 0, 0);
    // Formato ISO 8601: YYYY-MM-DDTHH:MM:SS
    return now.toISOString().slice(0, 19);
}

function cerrarSesion() {
    sessionStorage.clear();
    window.location.href = '/recepcion/login';
}

function toggleSubmenu(id) {
    const sub = document.getElementById(id);
    sub.style.display = sub.style.display === 'block' ? 'none' : 'block';
}


// --- Navegación y Renderizado de Vistas ---

function mostrarSeccion(seccion) {
    // Desactivar todas las secciones y botones
    document.querySelectorAll('.tab-content').forEach(div => div.classList.remove('active'));
    document.querySelectorAll('.nav-menu button').forEach(btn => btn.classList.remove('active'));
    
    let targetDivId = '';
    
    if (seccion === 'pagosPendientes') {
        targetDivId = 'seccion-pagosPendientes';
        cargarReservasActivas(); 
    } else if (seccion === 'mapeo') {
        targetDivId = 'seccion-mapeo';
        cargarPlazas(); 
    } else if (seccion === 'dashboard') {
        targetDivId = 'seccion-dashboard';
    } else {
        targetDivId = `seccion-${seccion}`; 
    }
    
    // Mostrar la sección objetivo
    const targetDiv = document.getElementById(targetDivId);
    if (targetDiv) {
        targetDiv.classList.add('active');
    }
    
    // Activar el botón clicado
    const activeBtn = document.querySelector(`[onclick="mostrarSeccion('${seccion}')"]`);
    if (activeBtn) activeBtn.classList.add('active');
}

function renderPlazas(plazas) {
    // Targets
    const visualGrid = document.getElementById('recepcionMapeoGrid');
    const managementList = document.getElementById('managementList');
    const selectPlaza = document.getElementById('ci_plaza_id');
    
    // Clear previous content
    visualGrid.innerHTML = '';
    managementList.innerHTML = '';
    if (selectPlaza) selectPlaza.innerHTML = ''; 

    // Find max column for dynamic CSS grid display
    const maxColumna = plazas.reduce((max, p) => Math.max(max, p.columna || 0), 0);
    if (maxColumna > 0) {
        visualGrid.style.display = 'grid';
        visualGrid.style.gridTemplateColumns = `repeat(${maxColumna}, 60px)`;
        visualGrid.style.gap = '15px';
    } else {
         visualGrid.innerHTML = '<p>No hay plazas definidas para esta cochera.</p>';
    }
    
    plazas.forEach(p => {
        // --- 1. Visual Map Grid Element ---
        const mapItem = document.createElement('div');
        mapItem.classList.add("plaza"); 
        mapItem.classList.add(p.ocupada ? 'rojo' : 'verde'); // Usa rojo/verde
        mapItem.textContent = p.codigo;
        
        // Posicionamiento basado en fila/columna de la entidad
        mapItem.style.gridRowStart = p.fila;
        mapItem.style.gridColumnStart = p.columna;
        mapItem.style.cursor = 'default'; // El mapa es solo visual
        
        recepcionMapeoGrid.appendChild(mapItem);

        // --- 2. Management List Button ---
        const managementButton = document.createElement('button');
        managementButton.innerHTML = `${p.codigo}: ${p.ocupada ? 'Ocupado' : 'Libre'}`;
        managementButton.classList.add('role-button');
        managementButton.style.backgroundColor = p.ocupada ? '#dc3545' : '#1e7e34'; // Rojo o Verde oscuro
        managementButton.onclick = () => toggleOcupacion(p.id, p.ocupada);
        
        managementList.appendChild(managementButton);

        // --- 3. Check-in Select Options ---
        if (!p.ocupada && selectPlaza) {
            const option = document.createElement('option');
            option.value = p.id;
            option.textContent = p.codigo;
            selectPlaza.appendChild(option);
        }
    });
    
    // Actualizar nombre de la cochera
    document.getElementById('cocheraNombreMap').textContent = cocheraNombre;
}


function renderReservasActivas(reservas) {
    const reservasActivasContainer = document.getElementById('reservasActivasContainer');
    reservasActivasContainer.innerHTML = '';
    
    if (reservas.length === 0) {
        reservasActivasContainer.innerHTML = '<p>No hay vehículos con pago pendiente (Checkout).</p>';
        return;
    }
    
    const tableHtml = `
        <table class="reservas-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Placa</th>
                    <th>Conductor</th>
                    <th>Casilla</th>
                    <th>Ingreso</th>
                    <th>Acción</th>
                </tr>
            </thead>
            <tbody>
                ${reservas.map(r => {
                    const conductorNombre = r.usuario ? r.usuario.nombreCompleto || r.usuario.nombre : 'N/A';
                    const placa = r.usuario ? r.usuario.placaVehiculo || 'N/A' : 'N/A';
                    const horaIngresoLocal = new Date(r.horaIngreso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
                    
                    return `
                        <tr>
                            <td>${r.id}</td>
                            <td>${placa}</td>
                            <td>${conductorNombre}</td>
                            <td>${r.plaza.codigo}</td>
                            <td>${horaIngresoLocal}</td>
                            <td><button onclick="iniciarCheckout(${r.id}, '${r.plaza.codigo}', '${conductorNombre}', '${horaIngresoLocal}')" class="role-button recepcionista">Finalizar y Pagar</button></td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
    reservasActivasContainer.innerHTML = tableHtml;
}


// --- Data Fetching ---

function cargarPlazas() {
    fetch(`/api/plazas/${cocheraId}`)
      .then(r => r.json())
      .then(data => renderPlazas(data))
      .catch(err => {
        document.getElementById('plazasContainer').innerHTML = '<p>Error cargando plazas.</p>';
        console.error(err);
      });
}

function cargarReservasActivas() {
    fetch(`/api/cocheras/${cocheraId}/reservas/activas`)
        .then(r => r.json())
        .then(data => renderReservasActivas(data))
        .catch(err => {
            document.getElementById('reservasActivasContainer').innerHTML = '<p>Error cargando reservas activas.</p>';
            console.error(err);
        });
}


// --- Action Handlers ---

function toggleOcupacion(id, isCurrentlyOccupied) {
    const nuevoEstado = !isCurrentlyOccupied;
    fetch(`/api/plazas/${id}/estado?ocupada=${nuevoEstado}`, { method: 'POST' })
        .then(r => {
            if (r.ok) {
                cargarPlazas(); 
                if (!nuevoEstado) {
                     cargarReservasActivas();
                }
            } else {
                alert('Error actualizando plaza');
            }
        });
}

// Modal Control
function openCheckinModal() {
    document.getElementById('ci_ingreso').value = new Date().toTimeString().substring(0, 5);
    document.getElementById('modal-checkin').style.display = 'block';
}

function closeCheckinModal() {
    document.getElementById('modal-checkin').style.display = 'none';
}

function closeCheckoutModal() {
    document.getElementById('checkoutModal').style.display = 'none';
    reservaEnCheckout = null;
}


function realizarCheckin() {
    const nombre = document.getElementById('ci_nombre').value;
    const placa = document.getElementById('ci_placa').value;
    const plazaSelect = document.getElementById('ci_plaza_id');
    const plazaId = plazaSelect.value;
    const ingreso = document.getElementById('ci_ingreso').value;

    if (!nombre || !placa || !plazaId || !ingreso) {
        alert("Complete todos los campos de registro.");
        return;
    }
    
    const ingresoISO = getNowDateTimeString(ingreso);
    const codigoPlaza = plazaSelect.options[plazaSelect.selectedIndex].text;

    const reservaData = {
        plazaId: Number(plazaId),
        horaIngreso: ingresoISO,
        horaSalida: null, 
        unsure: true, 
        nombreConductor: nombre,
        placaVehiculo: placa,
        metodoPago: null
    };

    fetch('/api/recepcion/checkin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reservaData)
    })
    .then(r => {
        if (!r.ok) throw new Error('Error al registrar check-in.');
        return r.json();
    })
    .then(() => {
        alert(`Registro exitoso para la plaza ${codigoPlaza}. Pago pendiente.`);
        closeCheckinModal();
        cargarPlazas(); 
        cargarReservasActivas(); 
    })
    .catch(error => {
        console.error(error);
        alert('Error en el registro: ' + error.message);
    });
}


function iniciarCheckout(reservaId, codigoPlaza, conductorNombre, horaIngresoLocal) {
    reservaEnCheckout = reservaId;
    const ahora = new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    
    const checkoutDetailsDiv = document.getElementById('checkoutDetails');
    checkoutDetailsDiv.innerHTML = `
        <p><strong>Reserva #ID:</strong> ${reservaId}</p>
        <p><strong>Conductor:</strong> ${conductorNombre}</p>
        <p><strong>Casilla:</strong> ${codigoPlaza}</p>
        <p><strong>Ingreso:</strong> ${horaIngresoLocal}</p>
        <p><strong>Salida (Ahora):</strong> ${ahora}</p>
        <p style="font-weight: bold;">El monto final se calculará al confirmar.</p>
    `;

    document.getElementById('checkoutModal').style.display = 'block';
}

function confirmarCheckout() {
    if (!reservaEnCheckout) return;

    const metodoPago = document.getElementById('checkoutMetodoPago').value;
    
    fetch(`/api/recepcion/checkout/${reservaEnCheckout}?metodoPago=${metodoPago}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(r => {
        if (!r.ok) throw new Error('Error al finalizar el pago.');
        return r.json();
    })
    .then(boleta => {
        alert(`Checkout exitoso. Total pagado: S/. ${boleta.monto.toFixed(2)}. Plaza liberada.`);
        closeCheckoutModal();
        cargarReservasActivas(); 
        cargarPlazas(); 
    })
    .catch(error => {
        console.error(error);
        alert('Error al procesar el checkout: ' + error.message);
    });
}
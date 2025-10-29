document.addEventListener("DOMContentLoaded", () => {
    const cocheraId = sessionStorage.getItem("cocheraId");
    const ingreso = sessionStorage.getItem("horaIngreso");
    const salida = sessionStorage.getItem("horaSalida");
    const unsure = sessionStorage.getItem("unsure");

    fetch(`/api/cocheras/${cocheraId}/plazas?horaIngreso=${ingreso}&horaSalida=${salida}&unsure=${unsure}`)
        .then(r => r.json())
        .then(data => {
            const cont = document.getElementById("mapeo");
            data.forEach(plaza => {
                const div = document.createElement("div");
                div.classList.add("plaza");
                div.classList.add(plaza.estado.toLowerCase());
                div.textContent = plaza.codigo;
                div.onclick = () => seleccionar(div);
                cont.appendChild(div);
            });
        });
});

function seleccionar(div) {
    document.querySelectorAll(".plaza").forEach(p => p.classList.remove("seleccionado"));
    div.classList.add("seleccionado");
}

function confirmar() {
    alert("Reserva confirmada con éxito");
}

document.addEventListener("DOMContentLoaded", () => {
    fetch("/api/dashboard/stats")
        .then(r => r.json())
        .then(data => {
            const ctx1 = document.getElementById("chart1");
            new Chart(ctx1, {
                type: "pie",
                data: {
                    labels: ["Con app", "Sin app"],
                    datasets: [{ data: [data.conApp, data.sinApp], backgroundColor: ["#36a2eb", "#ff6384"] }]
                }
            });

            const ctx2 = document.getElementById("chart2");
            new Chart(ctx2, {
                type: "bar",
                data: {
                    labels: data.dias,
                    datasets: [{ label: "Ingresos diarios", data: data.ingresos, backgroundColor: "#4bc0c0" }]
                }
            });
        });
});

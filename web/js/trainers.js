const trainerForm = document.getElementById("trainerForm");
const trainersTable = document.getElementById("trainersTable");
const trainerMessage = document.getElementById("trainerMessage");

async function loadTrainers() {
    try {
        const response = await fetch("/api/trainers");
        const trainers = await response.json();

        trainersTable.innerHTML = "";

        trainers.forEach(trainer => {
            trainersTable.innerHTML += `
                <tr>
                    <td>${trainer.id}</td>
                    <td>${trainer.name}</td>
                    <td>${trainer.email}</td>
                    <td>${trainer.phone}</td>
                    <td>${trainer.specialization}</td>
                </tr>
            `;
        });
    } catch (error) {
        trainerMessage.textContent = "Failed to load trainers.";
    }
}

trainerForm.addEventListener("submit", async function(event) {
    event.preventDefault();

    const trainer = {
        name: document.getElementById("name").value,
        email: document.getElementById("email").value,
        phone: document.getElementById("phone").value,
        specialization: document.getElementById("specialization").value
    };

    try {
        const response = await fetch("/api/trainers", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(trainer)
        });

        const result = await response.json();
        trainerMessage.textContent = result.message;

        if (result.success) {
            trainerForm.reset();
            loadTrainers();
        }
    } catch (error) {
        trainerMessage.textContent = "Server connection failed.";
    }
});

loadTrainers();
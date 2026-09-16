document.getElementById("loginForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const message = document.getElementById("loginMessage");

    try {
        const response = await fetch("/api/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email: email, password: password })
        });

        const result = await response.json();

        message.textContent = result.message;
        message.style.color = result.success ? "green" : "red";

        if (result.success) {
            window.location.href = "dashboard.html";
        }
    } catch (error) {
        console.error(error);
        message.textContent = "Server connection failed.";
        message.style.color = "red";
    }
});
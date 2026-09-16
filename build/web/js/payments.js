const paymentForm = document.getElementById("paymentForm");
const paymentTable = document.getElementById("paymentTable");
const paymentMessage = document.getElementById("paymentMessage");

async function loadPayments() {
    try {
        const response = await fetch("/api/payments");
        const payments = await response.json();

        paymentTable.innerHTML = "";

        payments.forEach(payment => {
            paymentTable.innerHTML += `
                <tr>
                    <td>${payment.id}</td>
                    <td>${payment.member_id}</td>
                    <td>₹${payment.amount}</td>
                    <td>${payment.payment_date}</td>
                    <td>${payment.payment_method}</td>
                    <td>${payment.status}</td>
                </tr>
            `;
        });
    } catch (error) {
        paymentMessage.textContent = "Failed to load payments.";
    }
}

paymentForm.addEventListener("submit", async function(event) {
    event.preventDefault();

    const payment = {
        member_id: document.getElementById("memberId").value,
        amount: document.getElementById("amount").value,
        payment_date: document.getElementById("paymentDate").value,
        payment_method: document.getElementById("paymentMethod").value,
        status: document.getElementById("status").value
    };

    try {
        const response = await fetch("/api/payments", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payment)
        });

        const result = await response.json();
        paymentMessage.textContent = result.message;

        if (result.success) {
            paymentForm.reset();
            loadPayments();
        }
    } catch (error) {
        paymentMessage.textContent = "Server connection failed.";
    }
});

loadPayments();
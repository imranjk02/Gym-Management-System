const membershipForm = document.getElementById("membershipForm");
const membershipsTable = document.getElementById("membershipsTable");
const membershipMessage = document.getElementById("membershipMessage");

async function loadMemberships() {
    try {
        const response = await fetch("/api/memberships");
        const memberships = await response.json();

        membershipsTable.innerHTML = "";

        memberships.forEach(membership => {
            membershipsTable.innerHTML += `
                <tr>
                    <td>${membership.id}</td>
                    <td>${membership.member_id}</td>
                    <td>${membership.plan_name}</td>
                    <td>${membership.start_date}</td>
                    <td>${membership.end_date}</td>
                    <td>₹${membership.amount}</td>
                </tr>
            `;
        });
    } catch (error) {
        membershipMessage.textContent = "Failed to load memberships.";
    }
}

membershipForm.addEventListener("submit", async function(event) {
    event.preventDefault();

    const membership = {
        member_id: document.getElementById("memberId").value,
        plan_name: document.getElementById("planName").value,
        start_date: document.getElementById("startDate").value,
        end_date: document.getElementById("endDate").value,
        amount: document.getElementById("amount").value
    };

    try {
        const response = await fetch("/api/memberships", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(membership)
        });

        const result = await response.json();
        membershipMessage.textContent = result.message;

        if (result.success) {
            membershipForm.reset();
            loadMemberships();
        }
    } catch (error) {
        membershipMessage.textContent = "Server connection failed.";
    }
});

loadMemberships();
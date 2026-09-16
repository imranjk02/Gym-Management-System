const memberForm = document.getElementById("memberForm");
const membersTable = document.getElementById("membersTable");
const memberMessage = document.getElementById("memberMessage");

async function loadMembers() {
    try {
        const response = await fetch("/api/members");
        const members = await response.json();

        membersTable.innerHTML = "";

        members.forEach(member => {
            membersTable.innerHTML += `
                <tr>
                    <td>${member.id}</td>
                    <td>${member.name}</td>
                    <td>${member.email}</td>
                    <td>${member.phone}</td>
                    <td>${member.gender}</td>
                    <td>${member.joining_date}</td>
                    <td>${member.membership_type}</td>
                </tr>
            `;
        });
    } catch (error) {
        memberMessage.textContent = "Failed to load members.";
    }
}

memberForm.addEventListener("submit", async event => {
    event.preventDefault();

    const member = {
        name: document.getElementById("name").value.trim(),
        email: document.getElementById("email").value.trim(),
        phone: document.getElementById("phone").value.trim(),
        gender: document.getElementById("gender").value,
        joining_date: document.getElementById("joiningDate").value,
        membership_type: document.getElementById("membershipType").value
    };

    try {
        const response = await fetch("/api/members", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(member)
        });

        const result = await response.json();

        memberMessage.textContent = result.message;

        if (result.success) {
            memberForm.reset();
            loadMembers();
        }
    } catch (error) {
        memberMessage.textContent = "Failed to add member.";
    }
});

loadMembers();
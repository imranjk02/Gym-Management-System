async function loadStats() {
    try {
        const response = await fetch("/api/stats");
        const stats = await response.json();

        document.getElementById("totalMembers").textContent = stats.members;
        document.getElementById("totalTrainers").textContent = stats.trainers;
        document.getElementById("totalMemberships").textContent = stats.memberships;
        document.getElementById("totalPayments").textContent = "₹" + Number(stats.payments).toLocaleString("en-IN");
    } catch (error) {
        console.error("Failed to load dashboard statistics:", error);
    }
}

loadStats();
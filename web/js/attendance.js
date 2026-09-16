const attendanceForm = document.getElementById("attendanceForm");
const attendanceTable = document.getElementById("attendanceTable");
const attendanceMessage = document.getElementById("attendanceMessage");

async function loadAttendance() {
    try {
        const response = await fetch("/api/attendance");
        const attendance = await response.json();

        attendanceTable.innerHTML = "";

        attendance.forEach(record => {
            attendanceTable.innerHTML += `
                <tr>
                    <td>${record.id}</td>
                    <td>${record.member_id}</td>
                    <td>${record.attendance_date}</td>
                    <td>${record.check_in}</td>
                    <td>${record.check_out || "-"}</td>
                </tr>
            `;
        });
    } catch (error) {
        attendanceMessage.textContent = "Failed to load attendance.";
    }
}

attendanceForm.addEventListener("submit", async function(event) {
    event.preventDefault();

    const attendance = {
        member_id: document.getElementById("memberId").value,
        attendance_date: document.getElementById("attendanceDate").value,
        check_in: document.getElementById("checkIn").value,
        check_out: document.getElementById("checkOut").value
    };

    try {
        const response = await fetch("/api/attendance", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(attendance)
        });

        const result = await response.json();
        attendanceMessage.textContent = result.message;

        if (result.success) {
            attendanceForm.reset();
            loadAttendance();
        }
    } catch (error) {
        attendanceMessage.textContent = "Server connection failed.";
    }
});

loadAttendance();
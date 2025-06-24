const chatWindow = document.getElementById('chat-window');
const chatForm = document.getElementById('chat-form');
const userInput = document.getElementById('user-input');
const logoutBtn = document.getElementById('logout-btn');
const clearBtn = document.getElementById('clear-btn');

function appendMessage(text, sender) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${sender}`;
    msgDiv.textContent = text;
    chatWindow.appendChild(msgDiv);
    chatWindow.scrollTop = chatWindow.scrollHeight;
}

// Load chat history on page load
async function loadHistory() {
    try {
        const res = await fetch('/history');
        if (res.status === 401 || res.redirected) {
            window.location.href = '/login';
            return;
        }
        const data = await res.json();
        chatWindow.innerHTML = '';
        data.history.forEach(msg => {
            appendMessage(msg.message, msg.sender);
        });
    } catch (err) {
        appendMessage('Could not load chat history.', 'bot');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadHistory();
});

chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const message = userInput.value.trim();
    if (!message) return;
    appendMessage(message, 'user');
    userInput.value = '';
    try {
        const response = await fetch('/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message }),
        });
        if (response.status === 401 || response.redirected) {
            window.location.href = '/login';
            return;
        }
        if (!response.ok) throw new Error('Network error');
        const data = await response.json();
        appendMessage(data.response, 'bot');
    } catch (err) {
        appendMessage('Sorry, there was an error connecting to the server.', 'bot');
    }
});

logoutBtn.onclick = () => {
    window.location.href = '/logout';
};

clearBtn.onclick = async () => {
    clearBtn.disabled = true;
    try {
        const res = await fetch('/clear_history', { method: 'POST' });
        if (res.status === 401 || res.redirected) {
            window.location.href = '/login';
            return;
        }
        const data = await res.json();
        if (data.success) {
            chatWindow.innerHTML = '';
        } else {
            appendMessage('Could not clear chat history.', 'bot');
        }
    } catch (err) {
        appendMessage('Could not clear chat history.', 'bot');
    }
    clearBtn.disabled = false;
    loadHistory();
}; 
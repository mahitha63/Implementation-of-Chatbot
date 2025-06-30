const chatWindow = document.getElementById('chat-window');
const chatForm = document.getElementById('chat-form');
const userInput = document.getElementById('user-input');
const logoutBtn = document.getElementById('logout-btn');
const clearBtn = document.getElementById('clear-btn');
const exportBtn = document.getElementById('export-btn');
const emojiBtn = document.getElementById('emoji-btn');
const minimizeBtn = document.getElementById('minimize-btn');
const chatContainer = document.querySelector('.chat-container');
const chatWindowDiv = document.getElementById('chat-window');
const chatFormDiv = document.getElementById('chat-form');
const quickRepliesDiv2 = document.getElementById('quick-replies');
const notifAudio = document.getElementById('notif-audio');

const quickReplies = [
    "Hello!",
    "Who are you?",
    "Tell me a joke",
    "What can you do?",
    "Thank you",
    "Bye"
];

let typingIndicator = null;
let emojiPanel = null;
const emojis = ['😀','😂','😍','😎','😊','😉','😢','😡','👍','🙏','🎉','❤️','🔥','🤖','😇','🥳','😅','😜','😱','😏','😬'];

let minimized = false;
minimizeBtn.onclick = () => {
    minimized = !minimized;
    if (minimized) {
        chatWindowDiv.style.display = 'none';
        chatFormDiv.style.display = 'none';
        quickRepliesDiv2.style.display = 'none';
        minimizeBtn.textContent = '▔';
        minimizeBtn.title = 'Maximize chat';
    } else {
        chatWindowDiv.style.display = '';
        chatFormDiv.style.display = '';
        quickRepliesDiv2.style.display = '';
        minimizeBtn.textContent = '▁';
        minimizeBtn.title = 'Minimize chat';
    }
};

function showTypingIndicator() {
    if (!typingIndicator) {
        typingIndicator = document.createElement('div');
        typingIndicator.className = 'typing-indicator';
        typingIndicator.textContent = 'Bot is typing…';
        chatWindow.appendChild(typingIndicator);
        chatWindow.scrollTop = chatWindow.scrollHeight;
    }
}
function hideTypingIndicator() {
    if (typingIndicator) {
        chatWindow.removeChild(typingIndicator);
        typingIndicator = null;
    }
}

function playNotif() {
    if (notifAudio) {
        notifAudio.currentTime = 0;
        notifAudio.play();
    }
}

function appendMessage(text, sender, timestamp = null, playSound = false) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${sender}`;
    msgDiv.textContent = text;
    // Add timestamp
    const timeDiv = document.createElement('div');
    timeDiv.className = 'msg-timestamp';
    let timeStr = '';
    if (timestamp) {
        // Use timestamp from history (assume ISO or similar)
        const d = new Date(timestamp);
        timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else {
        // Use current time
        const d = new Date();
        timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    timeDiv.textContent = timeStr;
    msgDiv.appendChild(timeDiv);
    chatWindow.appendChild(msgDiv);
    chatWindow.scrollTop = chatWindow.scrollHeight;
    if (playSound && sender === 'bot') playNotif();
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
            appendMessage(msg.message, msg.sender, msg.timestamp);
        });
    } catch (err) {
        appendMessage('Could not load chat history.', 'bot');
    }
}

function renderQuickReplies() {
    quickRepliesDiv2.innerHTML = '';
    quickReplies.forEach(q => {
        const btn = document.createElement('button');
        btn.className = 'quick-reply-btn';
        btn.textContent = q;
        btn.onclick = () => {
            userInput.value = q;
            chatForm.dispatchEvent(new Event('submit', { cancelable: true }));
        };
        quickRepliesDiv2.appendChild(btn);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    loadHistory();
    renderQuickReplies();
});

chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const message = userInput.value.trim();
    if (!message) return;
    appendMessage(message, 'user');
    userInput.value = '';
    showTypingIndicator();
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
        hideTypingIndicator();
        appendMessage(data.response, 'bot', null, true);
    } catch (err) {
        hideTypingIndicator();
        appendMessage('Sorry, there was an error connecting to the server.', 'bot', null, true);
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

exportBtn.onclick = async () => {
    exportBtn.disabled = true;
    try {
        const res = await fetch('/history');
        if (res.status === 401 || res.redirected) {
            window.location.href = '/login';
            return;
        }
        const data = await res.json();
        let text = '';
        data.history.forEach(msg => {
            const d = msg.timestamp ? new Date(msg.timestamp) : new Date();
            const timeStr = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            text += `[${timeStr}] ${msg.sender}: ${msg.message}\n`;
        });
        const blob = new Blob([text], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'chat_history.txt';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    } catch (err) {
        appendMessage('Could not export chat history.', 'bot');
    }
    exportBtn.disabled = false;
};

emojiBtn.onclick = (e) => {
    e.preventDefault();
    if (emojiPanel) {
        emojiPanel.remove();
        emojiPanel = null;
        return;
    }
    emojiPanel = document.createElement('div');
    emojiPanel.className = 'emoji-panel';
    emojis.forEach(emoji => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'emoji-choice';
        btn.textContent = emoji;
        btn.onclick = (ev) => {
            ev.preventDefault();
            insertAtCursor(userInput, emoji);
            emojiPanel.remove();
            emojiPanel = null;
            userInput.focus();
        };
        emojiPanel.appendChild(btn);
    });
    emojiBtn.parentNode.insertBefore(emojiPanel, emojiBtn.nextSibling);
};

document.addEventListener('click', (e) => {
    if (emojiPanel && !emojiPanel.contains(e.target) && e.target !== emojiBtn) {
        emojiPanel.remove();
        emojiPanel = null;
    }
});

function insertAtCursor(input, text) {
    const start = input.selectionStart;
    const end = input.selectionEnd;
    const before = input.value.substring(0, start);
    const after = input.value.substring(end);
    input.value = before + text + after;
    input.selectionStart = input.selectionEnd = start + text.length;
} 
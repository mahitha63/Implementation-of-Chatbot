# Intent-Based AI Chatbot

A modern, full-featured intent-based chatbot web app with authentication, per-user chat history, dark mode, and a beautiful UI. Built with Java (HTTP server), vanilla HTML/CSS/JS, and a simple JSON-based intent system.

---

## Features

- **User Authentication**: Secure login for each user (demo user: `user` / `password`)
- **Per-User Chat History**: Each user's chat is saved and can be cleared
- **Intent-Based Responses**: Classic pattern-matching NLP with a rich, extensible `intents.json`
- **Modern UI**: Responsive, mobile-friendly, and visually appealing
- **Dark Mode**: Toggle dark/light mode on both login and chat pages
- **Beautiful Backgrounds**: Gradients, images, and SVG design elements
- **Quick Replies & Emoji Support**: Fast message buttons and emoji picker
- **Easy Customization**: Add new intents, responses, or UI tweaks easily

---

## Setup

1. **Clone the repository**
2. **Install Java** (JDK 11+ recommended)
3. **Download dependencies**
   - Place `json-20230227.jar` (org.json) in the project root (already included)
4. **Run the app**
   ```bash
   javac -cp json-20230227.jar App.java
   java -cp .;json-20230227.jar App
   ```
   (On Linux/Mac, use `:` instead of `;` in the classpath)
5. **Open your browser**
   - Go to [http://localhost:8080](http://localhost:8080)

---

## Usage

- **Login** with the default user (`user` / `password`)
- **Chat** with the bot about a wide range of topics (see `intents.json`)
- **Clear Chat** to remove your chat history
- **Export Chat** to download your conversation as a text file
- **Toggle Dark Mode** for a different look
- **Logout** to end your session

---

## Customization

- **Add/Modify Intents**: Edit `intents.json` to add new topics, patterns, and responses
- **Change UI**: Edit files in `static/` for HTML, CSS, and JS tweaks
- **Change Backgrounds/Colors**: Adjust gradients, images, or SVGs in the HTML/CSS
- **Improve Responses**: Add more patterns for better intent recognition

---

## File Structure

```
App.java                # Main Java backend (HTTP server, intent logic, authentication)
intents.json            # All chatbot intents, patterns, and responses
static/
  index.html            # Main chat UI
  login.html            # Login page
  script.js             # Frontend logic (chat, emoji, export, etc.)
  styles.css            # All styles (light/dark mode, layout, etc.)
json-20230227.jar       # org.json library for JSON parsing
chatbot.db              # (Unused, legacy or for future use)
```

---

## Tech Stack

- **Backend**: Java (com.sun.net.httpserver), org.json
- **Frontend**: HTML, CSS, JavaScript (no frameworks)

---

## License

MIT (or specify your own)

---

If you need a `requirements.txt`, it is only relevant for the legacy Python version. This project runs entirely on Java for the backend.
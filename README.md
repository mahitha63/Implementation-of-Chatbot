# Intent-Based AI Chatbot

A modern, full-featured intent-based chatbot web app with authentication, per-user chat history, dark mode, and a beautiful UI. Built with Flask, SQLite, scikit-learn, and vanilla HTML/CSS/JS (no React).

---

## Features
- **User Authentication**: Secure login for each user
- **Per-User Chat History**: Each user's chat is saved and can be cleared
- **Intent-Based Responses**: Classic NLP (TF-IDF + LogisticRegression) with a rich, extensible `intents.json`
- **Modern UI**: Responsive, mobile-friendly, and visually appealing
- **Dark Mode**:  Dark/light mode on both login and chat pages
- **Beautiful Backgrounds**: Gradients, images, and SVG design elements
- **Easy Customization**: Add new intents, responses, or UI tweaks easily

---

## Setup
1. **Clone the repository**
2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```
3. **Run the app**
   ```bash
   python app.py
   ```
4. **Open your browser**
   - Go to [http://localhost:5000](http://localhost:5000)

---

## Usage
- **Login** with the default user (`user` / `password`) or register a new account (if registration is enabled).
- **Chat** with the bot about a wide range of topics (see `intents.json`).
- **Clear Chat** to remove your chat history.
- **Toggle Dark Mode** for a different look.
- **Logout** to end your session.

---

## Customization
- **Add/Modify Intents**: Edit `intents.json` to add new topics, patterns, and responses.
- **Change UI**: Edit files in `static/` for HTML, CSS, and JS tweaks.
- **Change Backgrounds/Colors**: Adjust gradients, images, or SVGs in the HTML/CSS.
- **Model Improvements**: Add more patterns for better intent recognition, or swap in a more advanced NLP model if desired.

---

## Tech Stack
- **Backend**: Flask, SQLite, scikit-learn
- **Frontend**: HTML, CSS, JavaScript (no frameworks)

---

## License
MIT (or specify your own) 
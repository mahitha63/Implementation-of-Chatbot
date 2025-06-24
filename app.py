import os
import json
import sqlite3
import re
from flask import Flask, request, jsonify, session, redirect, url_for, send_from_directory
from werkzeug.security import generate_password_hash, check_password_hash
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
import random

app = Flask(__name__, static_folder='static')
app.secret_key = 'supersecretkey'  # Change in production
DB_PATH = 'chatbot.db'

# --- Database Setup ---
def init_db():
    with sqlite3.connect(DB_PATH) as conn:
        c = conn.cursor()
        c.execute('''CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            password TEXT
        )''')
        c.execute('''CREATE TABLE IF NOT EXISTS history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT,
            sender TEXT,
            message TEXT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )''')
        conn.commit()
        # Create demo user if not exists
        c.execute('SELECT * FROM users WHERE username=?', ('user',))
        if not c.fetchone():
            c.execute('INSERT INTO users (username, password) VALUES (?, ?)',
                      ('user', generate_password_hash('password')))
            conn.commit()
init_db()

# --- Preprocessing ---
def preprocess(text):
    text = text.lower()
    text = re.sub(r'[^a-z0-9\s]', '', text)
    return text

# --- Intent Model Setup ---
with open('intents.json', 'r') as f:
    intents = json.load(f)
patterns = []
tags = []
for intent in intents:
    for pattern in intent['patterns']:
        patterns.append(preprocess(pattern))
        tags.append(intent['tag'])
vectorizer = TfidfVectorizer()
x = vectorizer.fit_transform(patterns)
clf = LogisticRegression(max_iter=1000)
clf.fit(x, tags)

def predict_intent(text):
    text = preprocess(text)
    X = vectorizer.transform([text])
    probs = clf.predict_proba(X)[0]
    max_prob = max(probs)
    tag = clf.classes_[probs.argmax()]
    if max_prob < 0.1:
        tag = 'fallback'
    return tag

def get_response(tag):
    for intent in intents:
        if intent['tag'] == tag:
            return random.choice(intent['responses'])
    return "Sorry, I didn't understand that."

# --- Auth Helpers ---
def is_logged_in():
    return session.get('logged_in', False)

def login_required(f):
    from functools import wraps
    @wraps(f)
    def decorated(*args, **kwargs):
        if not is_logged_in():
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated

# --- Routes ---
@app.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')
    if not username or not password:
        return jsonify({'success': False, 'error': 'Username and password required.'})
    with sqlite3.connect(DB_PATH) as conn:
        c = conn.cursor()
        try:
            c.execute('INSERT INTO users (username, password) VALUES (?, ?)',
                      (username, generate_password_hash(password)))
            conn.commit()
            return jsonify({'success': True})
        except sqlite3.IntegrityError:
            return jsonify({'success': False, 'error': 'Username already exists.'})

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        data = request.get_json() if request.is_json else request.form
        username = data.get('username')
        password = data.get('password')
        with sqlite3.connect(DB_PATH) as conn:
            c = conn.cursor()
            c.execute('SELECT password FROM users WHERE username=?', (username,))
            row = c.fetchone()
            if row and check_password_hash(row[0], password):
                session['logged_in'] = True
                session['username'] = username
                return jsonify({'success': True})
            else:
                return jsonify({'success': False, 'error': 'Invalid credentials'})
    return send_from_directory('static', 'login.html')

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('login'))

@app.route('/')
@login_required
def index():
    return send_from_directory('static', 'index.html')

@app.route('/<path:path>')
@login_required
def static_files(path):
    return send_from_directory('static', path)

@app.route('/chat', methods=['POST'])
@login_required
def chat():
    data = request.get_json()
    user_message = data.get('message', '')
    username = session.get('username')
    if not user_message:
        return jsonify({'response': "Please enter a message."})
    tag = predict_intent(user_message)
    bot_reply = get_response(tag)
    # Save user message and bot reply
    with sqlite3.connect(DB_PATH) as conn:
        c = conn.cursor()
        c.execute("INSERT INTO history (username, sender, message) VALUES (?, ?, ?)",
                  (username, 'user', user_message))
        c.execute("INSERT INTO history (username, sender, message) VALUES (?, ?, ?)",
                  (username, 'bot', bot_reply))
        conn.commit()
    return jsonify({'response': bot_reply})

@app.route('/history', methods=['GET'])
@login_required
def history():
    username = session.get('username')
    with sqlite3.connect(DB_PATH) as conn:
        c = conn.cursor()
        c.execute("SELECT sender, message, timestamp FROM history WHERE username=? ORDER BY id ASC", (username,))
        rows = c.fetchall()
    history = [{'sender': row[0], 'message': row[1], 'timestamp': row[2]} for row in rows]
    return jsonify({'history': history})

@app.route('/clear_history', methods=['POST'])
@login_required
def clear_history():
    username = session.get('username')
    with sqlite3.connect(DB_PATH) as conn:
        c = conn.cursor()
        c.execute("DELETE FROM history WHERE username=?", (username,))
        conn.commit()
    return jsonify({'success': True})

if __name__ == '__main__':
    app.run(debug=True) 
from flask import Blueprint, jsonify, request
from db import get_db_connection

auth_bp = Blueprint("auth_bp", __name__)


@auth_bp.route("/login", methods=["POST"])
def login():
    data = request.get_json()

    username = data.get("username")
    password = data.get("password")

    if not username or not password:
        return jsonify({
            "message": "Username dan password wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT id, username, role
        FROM users
        WHERE username = %s
        AND password = %s
    """, (username, password))

    user = cur.fetchone()

    cur.close()
    conn.close()

    if not user:
        return jsonify({
            "message": "Username atau password salah"
        }), 401

    return jsonify({
        "message": "Login berhasil",
        "user": {
            "id": user[0],
            "username": user[1],
            "role": user[2]
        }
    })
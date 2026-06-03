from flask import Blueprint, jsonify, request
from db import get_db_connection

user_bp = Blueprint("user_bp", __name__)


@user_bp.route("/", methods=["GET"])
def get_users():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT id, name, username, role, is_active, created_at
        FROM users
        ORDER BY id DESC
    """)

    rows = cur.fetchall()

    result = []
    for row in rows:
        result.append({
            "id": row[0],
            "name": row[1],
            "username": row[2],
            "role": row[3],
            "is_active": row[4],
            "created_at": str(row[5])
        })

    cur.close()
    conn.close()

    return jsonify(result)


@user_bp.route("/", methods=["POST"])
def create_user():
    data = request.get_json()

    name = data.get("name")
    username = data.get("username")
    password = data.get("password")
    role = data.get("role", "CASHIER")

    if not name or not username or not password:
        return jsonify({
            "message": "Nama, username, dan password wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    try:
        cur.execute("""
            INSERT INTO users (name, username, password, role, is_active)
            VALUES (%s, %s, %s, %s, TRUE)
            RETURNING id
        """, (name, username, password, role))

        user_id = cur.fetchone()[0]
        conn.commit()

        return jsonify({
            "message": "User berhasil dibuat",
            "user_id": user_id
        }), 201

    except Exception as e:
        conn.rollback()
        return jsonify({
            "message": f"Gagal membuat user: {str(e)}"
        }), 500

    finally:
        cur.close()
        conn.close()


@user_bp.route("/<int:user_id>", methods=["PUT"])
def update_user(user_id):
    data = request.get_json()

    name = data.get("name")
    username = data.get("username")
    role = data.get("role")

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        UPDATE users
        SET name = %s,
            username = %s,
            role = %s
        WHERE id = %s
        RETURNING id
    """, (name, username, role, user_id))

    updated = cur.fetchone()

    if not updated:
        conn.rollback()
        cur.close()
        conn.close()
        return jsonify({"message": "User tidak ditemukan"}), 404

    conn.commit()
    cur.close()
    conn.close()

    return jsonify({
        "message": "User berhasil diupdate"
    })


@user_bp.route("/<int:user_id>/reset-password", methods=["PUT"])
def reset_password(user_id):
    data = request.get_json()
    password = data.get("password")

    if not password:
        return jsonify({
            "message": "Password baru wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        UPDATE users
        SET password = %s
        WHERE id = %s
        RETURNING id
    """, (password, user_id))

    updated = cur.fetchone()

    if not updated:
        conn.rollback()
        cur.close()
        conn.close()
        return jsonify({"message": "User tidak ditemukan"}), 404

    conn.commit()
    cur.close()
    conn.close()

    return jsonify({
        "message": "Password berhasil direset"
    })


@user_bp.route("/<int:user_id>/toggle-active", methods=["PUT"])
def toggle_active_user(user_id):
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        UPDATE users
        SET is_active = NOT is_active
        WHERE id = %s
        RETURNING is_active
    """, (user_id,))

    updated = cur.fetchone()

    if not updated:
        conn.rollback()
        cur.close()
        conn.close()
        return jsonify({"message": "User tidak ditemukan"}), 404

    conn.commit()
    cur.close()
    conn.close()

    return jsonify({
        "message": "Status user berhasil diubah",
        "is_active": updated[0]
    })
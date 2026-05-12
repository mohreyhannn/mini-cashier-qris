from flask import Blueprint, jsonify, request
from db import get_db_connection

category_bp = Blueprint("category_bp", __name__)


@category_bp.route("/", methods=["GET"])
def get_categories():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("SELECT id, name, created_at FROM categories ORDER BY id ASC")
    rows = cur.fetchall()

    categories = []

    for row in rows:
        categories.append({
            "id": row[0],
            "name": row[1],
            "created_at": str(row[2])
        })

    cur.close()
    conn.close()

    return jsonify(categories)


@category_bp.route("/", methods=["POST"])
def create_category():
    data = request.get_json()
    name = data.get("name")

    if not name:
        return jsonify({
            "message": "Nama kategori wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute(
        "INSERT INTO categories (name) VALUES (%s) RETURNING id, name, created_at",
        (name,)
    )

    new_category = cur.fetchone()
    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": "Kategori berhasil ditambahkan",
        "data": {
            "id": new_category[0],
            "name": new_category[1],
            "created_at": str(new_category[2])
        }
    }), 201
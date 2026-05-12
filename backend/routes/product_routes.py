from flask import Blueprint, jsonify, request
from db import get_db_connection

product_bp = Blueprint("product_bp", __name__)


# GET ALL PRODUCTS
@product_bp.route("/", methods=["GET"])
def get_products():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT 
            products.id,
            products.name,
            products.price,
            categories.name as category_name,
            products.created_at

        FROM products
        JOIN categories
        ON products.category_id = categories.id

        ORDER BY products.id ASC
    """)

    rows = cur.fetchall()

    products = []

    for row in rows:
        products.append({
            "id": row[0],
            "name": row[1],
            "price": row[2],
            "category": row[3],
            "created_at": str(row[4])
        })

    cur.close()
    conn.close()

    return jsonify(products)


# CREATE PRODUCT
@product_bp.route("/", methods=["POST"])
def create_product():
    data = request.get_json()

    category_id = data.get("category_id")
    name = data.get("name")
    price = data.get("price")

    if not category_id or not name or not price:
        return jsonify({
            "message": "Semua field wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        INSERT INTO products (category_id, name, price)
        VALUES (%s, %s, %s)
        RETURNING id, name, price
    """, (category_id, name, price))

    new_product = cur.fetchone()

    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": "Produk berhasil ditambahkan",
        "data": {
            "id": new_product[0],
            "name": new_product[1],
            "price": new_product[2]
        }
    }), 201
    
# DELETE PRODUCT
@product_bp.route("/<int:id>", methods=["DELETE"])
def delete_product(id):

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        DELETE FROM products
        WHERE id = %s
    """, (id,))

    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": "Produk berhasil dihapus"
    })
    
    # UPDATE PRODUCT
@product_bp.route("/<int:id>", methods=["PUT"])
def update_product(id):

    data = request.get_json()

    category_id = data.get("category_id")
    name = data.get("name")
    price = data.get("price")

    if not category_id or not name or not price:
        return jsonify({
            "message": "Semua field wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        UPDATE products
        SET
            category_id = %s,
            name = %s,
            price = %s
        WHERE id = %s
    """, (
        category_id,
        name,
        price,
        id
    ))

    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": "Produk berhasil diupdate"
    })
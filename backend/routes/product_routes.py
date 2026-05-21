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
            categories.name AS category_name,
            products.stock,
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
            "stock": row[4],
            "created_at": str(row[5])
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
    stock = data.get("stock", 0)

    if not category_id or not name or not price:
        return jsonify({
            "message": "Semua field wajib diisi"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        INSERT INTO products (category_id, name, price, stock)
        VALUES (%s, %s, %s, %s)
        RETURNING id, name, price, stock
    """, (category_id, name, price, stock))

    new_product = cur.fetchone()

    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": "Produk berhasil ditambahkan",
        "data": {
            "id": new_product[0],
            "name": new_product[1],
            "price": new_product[2],
            "stock": new_product[3]
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


# RESTOCK PRODUCT
@product_bp.route("/<int:id>/restock", methods=["PUT"])
def restock_product(id):
    data = request.get_json()

    add_stock = data.get("add_stock")

    if add_stock is None:
        return jsonify({
            "message": "Jumlah stock wajib diisi"
        }), 400

    try:
        add_stock = int(add_stock)
    except ValueError:
        return jsonify({
            "message": "Jumlah stock harus berupa angka"
        }), 400

    if add_stock <= 0:
        return jsonify({
            "message": "Jumlah stock harus lebih dari 0"
        }), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        UPDATE products
        SET stock = COALESCE(stock, 0) + %s
        WHERE id = %s
        RETURNING id, name, stock
    """, (add_stock, id))

    product = cur.fetchone()

    if not product:
        conn.rollback()
        cur.close()
        conn.close()

        return jsonify({
            "message": "Produk tidak ditemukan"
        }), 404

    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": f"Stock {product[1]} berhasil ditambah",
        "product_id": product[0],
        "name": product[1],
        "stock": product[2]
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
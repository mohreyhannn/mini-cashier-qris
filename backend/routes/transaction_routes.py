from flask import Blueprint, jsonify, request
from db import get_db_connection
from datetime import datetime

transaction_bp = Blueprint("transaction_bp", __name__)


@transaction_bp.route("/", methods=["GET"])
def get_transactions():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT
            t.id,
            t.invoice_code,
            t.total_price,
            t.payment_method,
            t.payment_status,
            TO_CHAR(
                t.created_at + INTERVAL '7 hours',
                'YYYY-MM-DD HH24:MI:SS'
            ) AS created_at,
            COALESCE(u.name, u.username, '-') AS cashier_name,
            ti.product_id,
            p.name,
            ti.quantity,    
            ti.price,
            ti.subtotal
        FROM transactions t
        LEFT JOIN users u
            ON t.user_id = u.id
        LEFT JOIN transaction_items ti
            ON t.id = ti.transaction_id
        LEFT JOIN products p
            ON ti.product_id = p.id
        ORDER BY t.id DESC
    """)

    rows = cur.fetchall()
    transactions_dict = {}

    for row in rows:
        transaction_id = row[0]

        if transaction_id not in transactions_dict:
            transactions_dict[transaction_id] = {
                "id": row[0],
                "invoice_code": row[1],
                "total_price": row[2],
                "payment_method": row[3],
                "payment_status": row[4],
                "created_at": row[5],
                "cashier_name": row[6],
                "items": []
            }

        if row[7] is not None:
            transactions_dict[transaction_id]["items"].append({
                "product_id": row[7],
                "product_name": row[8],
                "quantity": row[9],
                "price": row[10],
                "subtotal": row[11]
            })

    cur.close()
    conn.close()

    return jsonify(list(transactions_dict.values()))


@transaction_bp.route("/daily", methods=["GET"])
def get_daily_transactions():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT
        DATE(created_at + INTERVAL '7 hours') AS date,
        COUNT(*) AS total_transactions,
        COALESCE(SUM(total_price), 0) AS total_income
    FROM transactions
    WHERE payment_status = 'PAID'
    GROUP BY DATE(created_at + INTERVAL '7 hours')
    ORDER BY date DESC
    """)

    rows = cur.fetchall()

    result = []
    for row in rows:
        result.append({
            "date": str(row[0]),
            "total_transactions": row[1],
            "total_income": row[2]
        })

    cur.close()
    conn.close()

    return jsonify(result)


@transaction_bp.route("/monthly", methods=["GET"])
def get_monthly_transactions():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT
            TO_CHAR(created_at + INTERVAL '7 hours', 'YYYY-MM') AS month,
            COUNT(*) AS total_transactions,
            COALESCE(SUM(total_price), 0) AS total_income
        FROM transactions
        WHERE payment_status = 'PAID'
        GROUP BY 1
        ORDER BY 1 DESC
    """)

    rows = cur.fetchall()

    result = []
    for row in rows:
        result.append({
            "month": row[0],
            "total_transactions": row[1],
            "total_income": row[2]
        })

    cur.close()
    conn.close()

    return jsonify(result)


@transaction_bp.route("/yearly", methods=["GET"])
def get_yearly_transactions():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT
            TO_CHAR(created_at + INTERVAL '7 hours', 'YYYY') AS year,
            COUNT(*) AS total_transactions,
            COALESCE(SUM(total_price), 0) AS total_income
        FROM transactions
        WHERE payment_status = 'PAID'
        GROUP BY 1
        ORDER BY 1 DESC
    """)

    rows = cur.fetchall()

    result = []
    for row in rows:
        result.append({
            "year": row[0],
            "total_transactions": row[1],
            "total_income": row[2]
        })

    cur.close()
    conn.close()

    return jsonify(result)


@transaction_bp.route("/", methods=["POST"])
def create_transaction():
    data = request.get_json()

    payment_method = data.get("payment_method", "QRIS")
    payment_status = data.get("payment_status", "PENDING")
    user_id = data.get("user_id")
    shift_id = data.get("shift_id")
    items = data.get("items")

    if not items:
        return jsonify({
            "message": "Item transaksi tidak boleh kosong"
        }), 400

    invoice_code = "INV-" + datetime.now().strftime("%Y%m%d%H%M%S")

    conn = get_db_connection()
    cur = conn.cursor()

    total_price = 0
    transaction_items = []

    try:
        for item in items:
            product_id = item["product_id"]
            quantity = item["quantity"]

            cur.execute("""
                SELECT id, name, price, stock
                FROM products
                WHERE id = %s
                FOR UPDATE
            """, (product_id,))

            product = cur.fetchone()

            if not product:
                conn.rollback()
                return jsonify({
                    "message": f"Produk dengan id {product_id} tidak ditemukan"
                }), 404

            product_name = product[1]
            price = product[2]
            stock = product[3]

            if stock < quantity:
                conn.rollback()
                return jsonify({
                    "message": f"Stock {product_name} tidak cukup. Sisa stock: {stock}"
                }), 400

            subtotal = price * quantity
            total_price += subtotal

            transaction_items.append({
                "product_id": product_id,
                "quantity": quantity,
                "price": price,
                "subtotal": subtotal
            })

        cur.execute("""
            INSERT INTO transactions
        (
            invoice_code,
            user_id,
            shift_id,
            total_price,
            payment_method,
            payment_status
        )
        VALUES (%s, %s, %s, %s, %s, %s)
            RETURNING id
        """, (
                invoice_code,
                user_id,
                shift_id,
                total_price,
                payment_method,
                payment_status
            ))

        transaction_id = cur.fetchone()[0]

        for item in transaction_items:
            cur.execute("""
                INSERT INTO transaction_items
                (
                    transaction_id,
                    product_id,
                    quantity,
                    price,
                    subtotal
                )
                VALUES (%s, %s, %s, %s, %s)
            """, (
                transaction_id,
                item["product_id"],
                item["quantity"],
                item["price"],
                item["subtotal"]
            ))

            cur.execute("""
                UPDATE products
                SET stock = stock - %s
                WHERE id = %s
            """, (
                item["quantity"],
                item["product_id"]
            ))

        conn.commit()

        return jsonify({
            "message": "Transaksi berhasil dibuat",
            "transaction_id": transaction_id,
            "invoice_code": invoice_code,
            "total_price": total_price,
            "payment_method": payment_method,
            "payment_status": payment_status
        }), 201

    except Exception as e:
        conn.rollback()

        return jsonify({
            "message": f"Transaksi gagal: {str(e)}"
        }), 500

    finally:
        cur.close()
        conn.close()
        
@transaction_bp.route("/best-sellers", methods=["GET"])
def best_sellers():

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
    SELECT
        p.name,
        SUM(ti.quantity) AS total_sold
    FROM transaction_items ti
    JOIN products p
        ON ti.product_id = p.id
    JOIN transactions t
        ON ti.transaction_id = t.id
    WHERE t.payment_status = 'PAID'
    GROUP BY p.name
    ORDER BY total_sold DESC
    LIMIT 5
    """)

    rows = cur.fetchall()

    result = []

    for row in rows:
        result.append({
            "product_name": row[0],
            "total_sold": row[1]
        })

    cur.close()
    conn.close()

    return jsonify(result)

@transaction_bp.route("/<int:id>/pay", methods=["PUT"])
def pay_transaction(id):
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        UPDATE transactions
        SET payment_status = 'PAID'
        WHERE id = %s
    """, (id,))

    conn.commit()

    cur.close()
    conn.close()

    return jsonify({
        "message": "Pembayaran berhasil"
    })
    
@transaction_bp.route("/dashboard", methods=["GET"])
def dashboard_summary():

    conn = get_db_connection()
    cur = conn.cursor()

    # Total income
    cur.execute("""
        SELECT COALESCE(SUM(total_price), 0)
        FROM transactions
        WHERE payment_status = 'PAID'
    """)

    total_income = cur.fetchone()[0]

    # Total transaksi
    cur.execute("""
        SELECT COUNT(*)
        FROM transactions
        WHERE payment_status = 'PAID'
    """)

    total_transactions = cur.fetchone()[0]

    # Total item terjual
    cur.execute("""
    SELECT COALESCE(SUM(ti.quantity), 0)
    FROM transaction_items ti
    JOIN transactions t
        ON ti.transaction_id = t.id
    WHERE t.payment_status = 'PAID'
    """)

    total_items_sold = cur.fetchone()[0]

    # Last transaction
    cur.execute("""
        SELECT invoice_code, total_price
        FROM transactions
        ORDER BY id DESC
        LIMIT 1
    """)

    last_transaction = cur.fetchone()

    cur.close()
    conn.close()

    return jsonify({
        "total_income": total_income,
        "total_transactions": total_transactions,
        "total_items_sold": total_items_sold,
        "last_transaction": {
            "invoice_code": last_transaction[0] if last_transaction else "-",
            "total_price": last_transaction[1] if last_transaction else 0
        }
    })
    
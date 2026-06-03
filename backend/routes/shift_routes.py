from flask import Blueprint, jsonify, request
from db import get_db_connection

shift_bp = Blueprint("shift_bp", __name__)


@shift_bp.route("/start", methods=["POST"])
def start_shift():
    data = request.get_json()

    user_id = data.get("user_id")
    opening_cash = data.get("opening_cash", 0)

    if not user_id:
        return jsonify({"message": "user_id wajib diisi"}), 400

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT id
        FROM shifts
        WHERE user_id = %s
        AND status = 'OPEN'
        LIMIT 1
    """, (user_id,))

    active_shift = cur.fetchone()

    if active_shift:
        cur.close()
        conn.close()
        return jsonify({
            "message": "Shift masih aktif",
            "shift_id": active_shift[0]
        }), 400

    cur.execute("""
        INSERT INTO shifts (user_id, opening_cash, status)
        VALUES (%s, %s, 'OPEN')
        RETURNING id
    """, (user_id, opening_cash))

    shift_id = cur.fetchone()[0]

    conn.commit()
    cur.close()
    conn.close()

    return jsonify({
        "message": "Shift berhasil dimulai",
        "shift_id": shift_id,
        "status": "OPEN"
    }), 201


@shift_bp.route("/active/<int:user_id>", methods=["GET"])
def get_active_shift(user_id):
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT id, user_id, start_time, opening_cash, status
        FROM shifts
        WHERE user_id = %s
        AND status = 'OPEN'
        ORDER BY id DESC
        LIMIT 1
    """, (user_id,))

    shift = cur.fetchone()

    cur.close()
    conn.close()

    if not shift:
        return jsonify({
            "active": False,
            "shift": None
        })

    return jsonify({
        "active": True,
        "shift": {
            "id": shift[0],
            "user_id": shift[1],
            "start_time": str(shift[2]),
            "opening_cash": shift[3],
            "status": shift[4]
        }
    })


@shift_bp.route("/end/<int:shift_id>", methods=["PUT"])
def end_shift(shift_id):
    data = request.get_json()
    closing_cash = data.get("closing_cash", 0)

    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT COALESCE(SUM(total_price), 0)
        FROM transactions
        WHERE shift_id = %s
        AND payment_status = 'PAID'
    """, (shift_id,))

    total_sales = cur.fetchone()[0]

    cur.execute("""
        UPDATE shifts
        SET
            end_time = CURRENT_TIMESTAMP,
            closing_cash = %s,
            total_sales = %s,
            status = 'CLOSED'
        WHERE id = %s
        RETURNING id
    """, (closing_cash, total_sales, shift_id))

    updated_shift = cur.fetchone()

    if not updated_shift:
        conn.rollback()
        cur.close()
        conn.close()
        return jsonify({"message": "Shift tidak ditemukan"}), 404

    conn.commit()
    cur.close()
    conn.close()

    return jsonify({
        "message": "Shift berhasil ditutup",
        "shift_id": shift_id,
        "total_sales": total_sales,
        "closing_cash": closing_cash,
        "status": "CLOSED"
    })


@shift_bp.route("/history", methods=["GET"])
def shift_history():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT
            s.id,
            COALESCE(u.name, u.username, '-') AS cashier_name,
            s.start_time,
            s.end_time,
            s.opening_cash,
            s.closing_cash,
            s.total_sales,
            s.status
        FROM shifts s
        JOIN users u
            ON s.user_id = u.id
        ORDER BY s.id DESC
    """)

    rows = cur.fetchall()

    result = []

    for row in rows:
        result.append({
            "id": row[0],
            "cashier_name": row[1],
            "start_time": str(row[2]),
            "end_time": str(row[3]) if row[3] else None,
            "opening_cash": row[4],
            "closing_cash": row[5],
            "total_sales": row[6],
            "status": row[7]
        })

    cur.close()
    conn.close()

    return jsonify(result)

@shift_bp.route("/summary-by-cashier", methods=["GET"])
def summary_by_cashier():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
        SELECT
            COALESCE(u.name, u.username, '-') AS cashier_name,
            COUNT(DISTINCT s.id) AS total_shifts,
            COUNT(DISTINCT t.id) AS total_transactions,
            COALESCE(SUM(
                CASE 
                    WHEN t.payment_status = 'PAID' THEN t.total_price
                    ELSE 0
                END
            ), 0) AS total_sales
        FROM shifts s
        JOIN users u
            ON s.user_id = u.id
        LEFT JOIN transactions t
            ON s.id = t.shift_id
        GROUP BY u.id, cashier_name
        ORDER BY total_sales DESC
    """)

    rows = cur.fetchall()

    result = []

    for row in rows:
        result.append({
            "cashier_name": row[0],
            "total_shifts": row[1],
            "total_transactions": row[2],
            "total_sales": int(row[3])
        })

    cur.close()
    conn.close()

    return jsonify(result)
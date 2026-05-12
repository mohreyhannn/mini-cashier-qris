from flask import Flask
from flask_cors import CORS

from routes.category_routes import category_bp
from routes.product_routes import product_bp
from routes.transaction_routes import transaction_bp
from routes.auth_routes import auth_bp

app = Flask(__name__)
CORS(app)

# CATEGORY ROUTES
app.register_blueprint(category_bp, url_prefix="/api/categories")

# PRODUCT ROUTES
app.register_blueprint(product_bp, url_prefix="/api/products")

# TRANSACTION ROUTES
app.register_blueprint(transaction_bp, url_prefix="/api/transactions")

# AUTH ROUTES
app.register_blueprint(auth_bp, url_prefix="/api/auth")


@app.route("/", methods=["GET"])
def home():
    return {
        "message": "Mini Cashier QRIS API is running"
    }
    

if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )
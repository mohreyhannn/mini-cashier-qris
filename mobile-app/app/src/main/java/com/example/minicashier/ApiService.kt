package com.example.minicashier

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class TransactionItemRequest(
    val product_id: Int,
    val quantity: Int
)

data class TransactionRequest(
    val payment_method: String = "QRIS",
    val user_id: Int,
    val items: List<TransactionItemRequest>
)

data class UpdateProductResponse(
    val message: String
)

data class TransactionResponse(
    val message: String,
    val transaction_id: Int,
    val invoice_code: String,
    val total_price: Int,
    val payment_method: String,
    val payment_status: String
)

data class PaymentResponse(
    val message: String
)

data class Category(
    val id: Int,
    val name: String,
    val created_at: String
)

data class CreateProductRequest(
    val category_id: Int,
    val name: String,
    val price: Int
)

data class CreateProductResponse(
    val message: String
)

data class DeleteProductResponse(
    val message: String
)

data class DailyReportResponse(
    val date: String,
    val total_transactions: Int,
    val total_income: Int
)

data class MonthlyReportResponse(
    val month: String,
    val total_transactions: Int,
    val total_income: Int
)

data class YearlyReportResponse(
    val year: String,
    val total_transactions: Int,
    val total_income: Int
)

data class LastTransaction(
    val invoice_code: String,
    val total_price: Int
)

data class DashboardResponse(
    val total_income: Int,
    val total_transactions: Int,
    val total_items_sold: Int,
    val last_transaction: LastTransaction
)

data class TransactionItem(
    val product_id: Int,
    val product_name: String,
    val quantity: Int,
    val price: Int,
    val subtotal: Int
)

data class TransactionData(
    val id: Int,
    val invoice_code: String,
    val total_price: Int,
    val payment_method: String,
    val payment_status: String,
    val created_at: String,
    val cashier_name: String,
    val items: List<TransactionItem>
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class UserData(
    val id: Int,
    val name: String,
    val username: String,
    val role: String
)
data class LoginResponse(
    val message: String,
    val user: UserData
)

data class RestockRequest(
    val add_stock: Int
)

data class RestockResponse(
    val message: String,
    val product_id: Int,
    val name: String,
    val stock: Int
)

data class BestSellerResponse(
    val product_name: String,
    val total_sold: Int
)

interface ApiService {

    @GET("products/")
    suspend fun getProducts(): List<Product>

    @POST("products/")
    suspend fun createProduct(
        @Body product: CreateProductRequest
    ): CreateProductResponse

    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Int
    ): DeleteProductResponse

    @GET("categories/")
    suspend fun getCategories(): List<Category>

    @GET("transactions/")
    suspend fun getTransactions(): List<TransactionData>

    @POST("transactions/")
    suspend fun createTransaction(
        @Body transaction: TransactionRequest
    ): TransactionResponse

    @PUT("transactions/{id}/pay")
    suspend fun payTransaction(
        @Path("id") id: Int
    ): PaymentResponse

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body product: CreateProductRequest
    ): UpdateProductResponse

    @GET("transactions/daily")
    suspend fun getDailyReports(): List<DailyReportResponse>

    @GET("transactions/monthly")
    suspend fun getMonthlyReports(): List<MonthlyReportResponse>

    @GET("transactions/yearly")
    suspend fun getYearlyReports(): List<YearlyReportResponse>

    @GET("transactions/dashboard")
    suspend fun getDashboard(): DashboardResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @PUT("products/{id}/restock")
    suspend fun restockProduct(
        @Path("id") id: Int,
        @Body request: RestockRequest
    ): RestockResponse

    @GET("transactions/best-sellers")
    suspend fun getBestSellers(): List<BestSellerResponse>
}
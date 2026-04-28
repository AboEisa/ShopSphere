package com.example.shopsphere.CleanArchitecture.data.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiServices {

    // ── Products ──────────────────────────────────────────────────────────

    @GET("All_Products")
    suspend fun getAllProducts(): List<BackendProductDto>

    @GET("{endpoint}")
    suspend fun getProductsByEndpoint(
        @Path("endpoint") endpoint: String
    ): List<BackendProductDto>

    // GET /Search?query=<text>
    @GET("Search")
    suspend fun searchProducts(
        @Query("query") query: String
    ): List<BackendProductDto>

    // ── Auth ──────────────────────────────────────────────────────────────

    @POST("SignUp")
    suspend fun signUp(
        @Body request: AuthRequestDto
    ): AuthResponseDto

    // /Login expects lowercase {email, password} — see LoginRequestDto.
    @POST("Login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthResponseDto

    @POST("Logout")
    suspend fun logout(): GenericResponseDto

    // ── Cart ──────────────────────────────────────────────────────────────

    // POST /AddToCart  body: { "productID": int, "quantity": int }
    @POST("AddToCart")
    suspend fun addToCart(
        @Body request: AddToCartRequestDto
    ): CartMutationResponseDto

    @GET("GetCartItems")
    suspend fun getCartItems(): GetCartItemsResponseDto

    // PUT /UpdateQuantity?productId=<int>&newQuantity=<int>  (no body)
    @PUT("UpdateQuantity")
    suspend fun updateQuantity(
        @Query("productId") productId: Int,
        @Query("newQuantity") newQuantity: Int
    ): CartMutationResponseDto

    @DELETE("RemoveFromCart")
    suspend fun removeFromCart(
        @Query("productId") productId: Int
    ): CartMutationResponseDto

    @DELETE("ClearCart")
    suspend fun clearCart(): CartMutationResponseDto

    // ── Favorites ─────────────────────────────────────────────────────────

    @POST("AddToFavorite")
    suspend fun addToFavorite(
        @Body request: FavoriteRequestDto
    ): FavoriteMutationResponseDto

    // RemoveFromFavorite is DELETE with a JSON body.
    @HTTP(method = "DELETE", path = "RemoveFromFavorite", hasBody = true)
    suspend fun removeFromFavorite(
        @Body request: FavoriteRequestDto
    ): FavoriteMutationResponseDto

    @GET("GetAllFavorites")
    suspend fun getAllFavorites(): List<FavoriteItemDto>

    // ── Profile ───────────────────────────────────────────────────────────

    @GET("MyDetails")
    suspend fun getMyDetails(): MyDetailsDto

    @PUT("UpdateMyDetails")
    suspend fun updateMyDetails(
        @Body request: UpdateMyDetailsRequest
    ): GenericResponseDto

    /**
     * The backend route literally contains an ampersand: `/UpdateMyAddress&Phone`.
     * Retrofit would percent-encode `&` if placed in `@PUT("...")`, breaking the
     * call, so we route through `@Url` and pass the path verbatim from the
     * data source. Do NOT change this without coordinating a backend rename.
     */
    @PUT
    suspend fun updateMyAddressAndPhone(
        @Url url: String,
        @Body request: UpdateAddressPhoneRequest
    ): GenericResponseDto

    // ── Images ────────────────────────────────────────────────────────────

    // POST /Upload  multipart/form-data, part name "file"
    @Multipart
    @POST("Upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): UploadResponseDto

    // ── Orders / Payment ──────────────────────────────────────────────────

    // POST /Checkout — body-less; server reads the authenticated user's cart.
    @POST("Checkout")
    suspend fun checkout(): CheckoutResponseDto

    @GET("MyOrders")
    suspend fun getMyOrders(): List<MyOrderDto>

    @POST("CreateInvoice")
    suspend fun createInvoice(): InvoiceResponseDto

    @POST("PayNow")
    suspend fun payNow(): PayNowResponseDto

    // /Callbackt is intentionally spelled with the trailing `t` — that is how
    // the backend route is registered. Don't "fix" the typo client-side.
    @POST("Callbackt")
    suspend fun paymentCallback(): PaymentCallbackDto
}

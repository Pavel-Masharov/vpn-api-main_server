package com.skyrox.vpnapp

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {
    data class AuthRequest(val email: String, val password: String)
    data class AuthResponse(val access_token: String, val token_type: String)

    data class VpnServer(
        val id: Int,
        val name: String,
        val location: String,
        val endpoint: String,
        val load: Int
    )

    data class ShadowsocksConfig(
        val server: String,
        val port: Int,
        val password: String,
        val method: String
    )

    data class VpnConfig(
        @SerializedName("private_key") val privateKey: String,
        @SerializedName("public_key") val publicKey: String,
        @SerializedName("address") val address: String,
        @SerializedName("dns") val dns: String,
        @SerializedName("endpoint") val endpoint: String,
        @SerializedName("allowed_ips") val allowedIps: String
    )

    data class KeyResponse(
        val message: String,
        val key: String?
    )

    @POST("/register")
    fun register(@Body request: AuthRequest): Call<AuthResponse>

    @POST("/login")
    fun login(@Body request: AuthRequest): Call<AuthResponse>

    @GET("/servers")
    fun getServers(@Header("Authorization") token: String): Call<List<VpnServer>>

    //@GET("/get-config/{serverId}")
    //fun getConfig(@Header("Authorization") token: String, @Path("serverId") serverId: Int): Call<VpnConfig>
    @GET("get-config")
    fun getConfig(
        @Header("Authorization") token: String,
        @Query("server_id") serverId: Int,
        @Query("user_email") email: String
    ): Call<VpnConfig>

    @POST("/users/update-key")
    fun updateUserKey(@Query("key") key: String): Call<KeyResponse>

    companion object {
        private const val BASE_URL = "http://176.98.40.16:8000/"

        @JvmStatic
        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

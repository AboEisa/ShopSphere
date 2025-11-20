package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.databinding.FragmentAddCardBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AddCardFragment : Fragment() {

    private var _binding: FragmentAddCardBinding? = null
    private val binding get() = _binding!!
    private lateinit var stripe: Stripe

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val publishableKey = PaymentConfiguration.getInstance(requireContext()).publishableKey
        stripe = Stripe(requireContext(), publishableKey)

        binding.btnAddCart.setOnClickListener {
            val params = binding.cardInputWidget.paymentMethodCreateParams
            if (params == null) {
                showToast("Invalid card details")
                return@setOnClickListener
            }

            getClientSecret { clientSecret ->
                requireActivity().runOnUiThread {
                    if (!clientSecret.isNullOrEmpty()) {
                        val confirmParams = ConfirmPaymentIntentParams
                            .createWithPaymentMethodCreateParams(params, clientSecret)
                        stripe.confirmPayment(this, confirmParams)
                    } else {
                        showToast("Failed to fetch client secret")
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun getClientSecret(callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val json = JSONObject()
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:4242/create-payment-intent") // Localhost for emulator
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Stripe", "Network error: ${e.message}")
                requireActivity().runOnUiThread {
                    showToast("Network error: ${e.message}")
                }
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                Log.d("Stripe", "Server response: $bodyString")

                try {
                    val jsonResponse = JSONObject(bodyString ?: "{}")
                    val clientSecret = jsonResponse.optString("clientSecret", null)
                    callback(clientSecret)
                } catch (e: Exception) {
                    Log.e("Stripe", "Invalid JSON", e)
                    requireActivity().runOnUiThread {
                        showToast("Invalid response from server")
                    }
                    callback(null)
                }
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
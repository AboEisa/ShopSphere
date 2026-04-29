package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.GetMyDetailsUseCase
import com.example.shopsphere.CleanArchitecture.domain.LogoutUseCase
import com.example.shopsphere.CleanArchitecture.domain.UpdateMyDetailsUseCase
import com.example.shopsphere.CleanArchitecture.domain.UploadImageUseCase
import com.example.shopsphere.CleanArchitecture.utils.Constant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Drives AccountFragment (header) and MyDetailsFragment (all-field edit).
 * PUT /UpdateMyDetails accepts all 5 fields: firstName, lastName, email, phone, address.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getMyDetailsUseCase: GetMyDetailsUseCase,
    private val updateMyDetailsUseCase: UpdateMyDetailsUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val prefs: SharedPreference
) : ViewModel() {

    // ── Profile state ─────────────────────────────────────────────────────────

    private val _profileState = MutableLiveData<ProfileUiState>(ProfileUiState.Empty)
    val profileState: LiveData<ProfileUiState> = _profileState

    // ── Loading ───────────────────────────────────────────────────────────────

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isUploadingImage = MutableLiveData(false)
    val isUploadingImage: LiveData<Boolean> = _isUploadingImage

    // ── One-shot UI events ────────────────────────────────────────────────────

    private val _uiEvent = MutableLiveData<AccountUiEvent?>(null)
    val uiEvent: LiveData<AccountUiEvent?> = _uiEvent

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        populateFromCache()
        refreshFromServer()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by MyDetailsFragment when user taps Save.
     * Sends all 5 fields to PUT /UpdateMyDetails.
     */
    fun saveMyDetails(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        address: String
    ) {
        val tFirstName = firstName.trim()
        val tLastName  = lastName.trim()
        val tEmail     = email.trim()
        val tPhone     = phone.trim()
        val tAddress   = address.trim()

        if (tFirstName.isBlank()) {
            _uiEvent.value = AccountUiEvent.ValidationError("firstName", "First name is required")
            return
        }
        if (tEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(tEmail).matches()) {
            _uiEvent.value = AccountUiEvent.ValidationError("email", "A valid email is required")
            return
        }

        // Optimistic local save
        prefs.saveProfile(name = "$tFirstName $tLastName".trim(), email = tEmail, phone = tPhone)
        if (tAddress.isNotBlank()) prefs.saveDeliveryAddress(tAddress)
        _profileState.value = (_profileState.value ?: ProfileUiState.Empty).copy(
            firstName = tFirstName,
            lastName  = tLastName,
            email     = tEmail,
            phone     = tPhone,
            address   = tAddress
        )

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val result = updateMyDetailsUseCase(
                    firstName = tFirstName,
                    lastName  = tLastName,
                    email     = tEmail,
                    phone     = tPhone,
                    address   = tAddress
                )
                if (result.isSuccess) {
                    _uiEvent.postValue(AccountUiEvent.SaveSuccess)
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to save"
                    _uiEvent.postValue(AccountUiEvent.PartialSaveError(msg))
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Silent server refresh — populates state from GET /MyDetails.
     * Called on fragment start; never shows a loading spinner.
     */
    fun refreshFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = getMyDetailsUseCase()
            result.fold(
                onSuccess = { dto ->
                    val sFirstName = dto.firstName.orEmpty().trim()
                    val sLastName  = dto.lastName.orEmpty().trim()
                    val sEmail     = dto.email.orEmpty()
                    val sPhone     = dto.phone.orEmpty()
                    val sAddress   = dto.address.orEmpty()

                    prefs.saveProfile(
                        name  = "$sFirstName $sLastName".trim(),
                        email = sEmail,
                        phone = sPhone
                    )
                    if (sAddress.isNotBlank()) prefs.saveDeliveryAddress(sAddress)

                    _profileState.postValue(
                        ProfileUiState(
                            firstName       = sFirstName,
                            lastName        = sLastName,
                            email           = sEmail,
                            phone           = sPhone,
                            address         = sAddress,
                            profileImageUrl = buildImageUrlFromPrefs(),
                            initials        = computeInitials(sFirstName, sLastName)
                        )
                    )
                },
                onFailure = { /* silent — keep cached state */ }
            )
        }
    }

    fun uploadProfileImage(file: File) {
        if (!file.exists() || file.length() == 0L) {
            _uiEvent.value = AccountUiEvent.Error("Selected image file is empty or missing")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isUploadingImage.postValue(true)
            try {
                val result = uploadImageUseCase(file)
                result.fold(
                    onSuccess = { dto ->
                        val fileName = dto.resolvedFileName()
                        if (!fileName.isNullOrBlank()) {
                            prefs.saveProfileImageName(fileName)
                            val imageUrl = buildImageUrl(fileName)
                            _profileState.postValue(
                                (_profileState.value ?: ProfileUiState.Empty).copy(profileImageUrl = imageUrl)
                            )
                            _uiEvent.postValue(AccountUiEvent.ImageUploadSuccess(imageUrl))
                        } else {
                            _uiEvent.postValue(AccountUiEvent.Error("Upload succeeded but returned no file name"))
                        }
                    },
                    onFailure = { error ->
                        _uiEvent.postValue(AccountUiEvent.Error(error.message ?: "Image upload failed"))
                    }
                )
            } finally {
                _isUploadingImage.postValue(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _profileState.postValue(ProfileUiState.Empty)
            _uiEvent.postValue(AccountUiEvent.LogoutSuccess)
        }
    }

    fun clearEvent() { _uiEvent.value = null }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun populateFromCache() {
        val storedName    = prefs.getProfileName()
        val parts         = storedName.trim().split(Regex("\\s+"), limit = 2)
        val firstName     = parts.getOrElse(0) { "" }
        val lastName      = parts.getOrElse(1) { "" }
        val email         = prefs.getProfileEmail()
        val phone         = prefs.getProfilePhone()
        val address       = prefs.getDeliveryAddress()
        val imageFileName = prefs.getProfileImageName()
        val imageUrl      = if (imageFileName.isNotBlank()) buildImageUrl(imageFileName) else ""

        _profileState.value = ProfileUiState(
            firstName       = firstName,
            lastName        = lastName,
            email           = email,
            phone           = phone,
            address         = address,
            profileImageUrl = imageUrl,
            initials        = computeInitials(firstName, lastName)
        )
    }

    private fun buildImageUrlFromPrefs(): String {
        val fileName = prefs.getProfileImageName()
        return if (fileName.isNotBlank()) buildImageUrl(fileName) else ""
    }

    private fun buildImageUrl(fileName: String): String = "${Constant.BASE_URL}GetImage/$fileName"

    private fun computeInitials(firstName: String, lastName: String): String {
        val f = firstName.firstOrNull()?.uppercaseChar()
        val l = lastName.firstOrNull()?.uppercaseChar()
        return when {
            f != null && l != null -> "$f$l"
            f != null              -> f.toString()
            else                   -> "?"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val firstName: String       = "",
    val lastName: String        = "",
    val email: String           = "",
    val phone: String           = "",
    val address: String         = "",
    val profileImageUrl: String = "",
    val initials: String        = "?"
) {
    val fullName: String get() = "$firstName $lastName".trim()

    companion object {
        val Empty = ProfileUiState()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// One-shot Events
// ─────────────────────────────────────────────────────────────────────────────

sealed class AccountUiEvent {
    object SaveSuccess : AccountUiEvent()
    data class PartialSaveError(val message: String) : AccountUiEvent()
    data class ValidationError(val field: String, val message: String) : AccountUiEvent()
    data class ImageUploadSuccess(val imageUrl: String) : AccountUiEvent()
    data class Error(val message: String) : AccountUiEvent()
    object LogoutSuccess : AccountUiEvent()
}

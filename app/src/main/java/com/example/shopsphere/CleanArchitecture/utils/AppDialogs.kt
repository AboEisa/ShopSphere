package com.example.shopsphere.CleanArchitecture.utils

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.example.shopsphere.R
import com.example.shopsphere.databinding.DialogFigmaActionBinding

fun Fragment.showSuccessDialog(
    title: String,
    message: String,
    primaryText: String? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    onOk: (() -> Unit)? = null
) {
    if (!isAdded) return
    showFigmaDialog(
        type = FigmaDialogType.SUCCESS,
        title = title,
        message = message,
        primaryText = primaryText ?: getString(R.string.dialog_ok),
        secondaryText = secondaryText,
        primaryColor = ContextCompat.getColor(requireContext(), R.color.black),
        onPrimary = onOk,
        onSecondary = onSecondary
    )
}

fun Fragment.showConfirmDialog(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String? = null,
    onConfirmed: () -> Unit
) {
    if (!isAdded) return
    showFigmaDialog(
        type = FigmaDialogType.WARNING,
        title = title,
        message = message,
        primaryText = positiveText,
        secondaryText = negativeText ?: getString(R.string.dialog_cancel),
        primaryColor = ContextCompat.getColor(requireContext(), R.color.ff3434),
        onPrimary = onConfirmed
    )
}

private enum class FigmaDialogType {
    SUCCESS,
    WARNING
}

private fun Fragment.showFigmaDialog(
    type: FigmaDialogType,
    title: String,
    message: String,
    primaryText: String,
    secondaryText: String? = null,
    primaryColor: Int,
    onPrimary: (() -> Unit)? = null,
    onSecondary: (() -> Unit)? = null
) {
    if (!isAdded) return
    val binding = DialogFigmaActionBinding.inflate(LayoutInflater.from(requireContext()))
    val dialog = AlertDialog.Builder(requireContext())
        .setView(binding.root)
        .create()

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.setCancelable(false)

    binding.textTitle.text = title
    binding.textMessage.text = message
    binding.buttonPrimary.text = primaryText
    binding.buttonPrimary.backgroundTintList = ColorStateList.valueOf(primaryColor)

    when (type) {
        FigmaDialogType.SUCCESS -> {
            binding.iconCircle.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_icon_circle_green)
            binding.iconImage.setImageResource(R.drawable.ic_check_circle)
            binding.iconImage.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.bright_green))
        }

        FigmaDialogType.WARNING -> {
            binding.iconCircle.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_icon_circle_red)
            binding.iconImage.setImageResource(R.drawable.ic_warning_outline)
            binding.iconImage.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ff3434))
        }
    }

    if (secondaryText.isNullOrBlank()) {
        binding.buttonSecondary.visibility = View.GONE
    } else {
        binding.buttonSecondary.visibility = View.VISIBLE
        binding.buttonSecondary.text = secondaryText
    }

    binding.buttonPrimary.setOnClickListener {
        dialog.dismiss()
        onPrimary?.invoke()
    }

    binding.buttonSecondary.setOnClickListener {
        dialog.dismiss()
        onSecondary?.invoke()
    }

    dialog.show()
}

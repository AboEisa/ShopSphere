package com.example.shopsphere.CleanArchitecture.utils

import java.text.DecimalFormat
import kotlin.math.abs

/**
 * Single source of truth for product / order price strings.
 *
 * If the price has no meaningful fractional part (e.g. 1190.0, 1190.001), it is
 * rendered as an integer with thousands separators ("1,190"). If there is a
 * real fractional component (e.g. 1190.50), two decimal places are kept
 * ("1,190.50"). All callers should use [formatEgpPrice] so the rule is
 * applied consistently across Home, Cart, Details, Checkout, etc.
 */
private val INT_FORMAT = DecimalFormat("#,##0")
private val DECIMAL_FORMAT = DecimalFormat("#,##0.00")

private const val FRACTION_EPSILON = 0.005

fun Double.formatPriceNumber(): String {
    val rounded = Math.round(this).toDouble()
    return if (abs(this - rounded) < FRACTION_EPSILON) {
        INT_FORMAT.format(rounded)
    } else {
        DECIMAL_FORMAT.format(this)
    }
}

fun formatEgpPrice(value: Double): String = "EGP ${value.formatPriceNumber()}"

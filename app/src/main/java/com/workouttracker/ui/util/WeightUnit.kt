package com.workouttracker.ui.util

object WeightUnit {
    const val KG_TO_LBS = 2.20462f
    const val LBS_TO_KG = 0.453592f

    fun display(kg: Float, useLbs: Boolean): String {
        return if (useLbs) {
            val lbs = kg * KG_TO_LBS
            if (lbs == kotlin.math.floor(lbs.toDouble()).toFloat()) "${lbs.toInt()} lbs"
            else "${String.format("%.1f", lbs)} lbs"
        } else {
            if (kg == kotlin.math.floor(kg.toDouble()).toFloat()) "${kg.toInt()} kg"
            else "${String.format("%.1f", kg)} kg"
        }
    }

    fun unitLabel(useLbs: Boolean) = if (useLbs) "lbs" else "kg"

    fun toKg(value: Float, useLbs: Boolean) = if (useLbs) value * LBS_TO_KG else value
    fun fromKg(kg: Float, useLbs: Boolean) = if (useLbs) kg * KG_TO_LBS else kg

    fun formatVolume(kg: Float, useLbs: Boolean): String {
        val v = if (useLbs) kg * KG_TO_LBS else kg
        return when {
            v >= 1_000_000 -> "${String.format("%.1f", v / 1_000_000)}M ${unitLabel(useLbs)}"
            v >= 1_000     -> "${String.format("%.1f", v / 1_000)}K ${unitLabel(useLbs)}"
            else           -> "${String.format("%.0f", v)} ${unitLabel(useLbs)}"
        }
    }
}

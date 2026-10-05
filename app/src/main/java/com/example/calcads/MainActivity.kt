package com.example.calcads

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {
    private var expr = ""
    private lateinit var tvExpr: TextView
    private lateinit var tvResult: TextView
    private val ops = "+−×÷"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvExpr = findViewById(R.id.tvExpr)
        tvResult = findViewById(R.id.tvResult)

        val root = findViewById<LinearLayout>(R.id.root)
        val m12 = dp(12)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(m12 + b.left, m12 + b.top, m12 + b.right, m12 + b.bottom)
            insets
        }

        MobileAds.initialize(this) {}
        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        val rows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )
        val pad = findViewById<LinearLayout>(R.id.pad)
        for (row in rows) {
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            }
            for (label in row) {
                val w = if (label == "0") 2f else 1f
                val (bg, fg) = when {
                    label == "=" -> "#6C7BFF" to "#FFFFFF"
                    label == "C" -> "#1B2038" to "#FF7A8A"
                    label in listOf("⌫", "%") -> "#1B2038" to "#C5C9E6"
                    label in ops.map { it.toString() } -> "#262C55" to "#8FA0FF"
                    else -> "#151A30" to "#FFFFFF"
                }
                line.addView(Button(this).apply {
                    text = label
                    textSize = 24f
                    gravity = Gravity.CENTER
                    isAllCaps = false
                    stateListAnimator = null
                    setTextColor(Color.parseColor(fg))
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(bg))
                        cornerRadius = dp(18).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, -1, w).apply {
                        setMargins(dp(4), dp(4), dp(4), dp(4))
                    }
                    setOnClickListener { press(label) }
                })
            }
            pad.addView(line)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun press(k: String) {
        when {
            k == "C" -> expr = ""
            k == "⌫" -> expr = expr.dropLast(1)
            k == "=" -> {
                val r = calc()
                if (r != null) expr = r
            }
            k in ops.map { it.toString() } -> {
                if (expr.isEmpty()) {
                    if (k == "−") expr = "−"
                } else {
                    if (expr.last() in ops) expr = expr.dropLast(1)
                    expr += k
                }
            }
            else -> expr += k
        }
        tvExpr.text = expr
        tvResult.text = if (k == "=") "" else (calc() ?: "")
    }

    private fun calc(): String? {
        if (expr.isEmpty()) return null
        return try {
            val v = Parser(expr).parse()
            if (v.isNaN() || v.isInfinite()) "Ошибка"
            else BigDecimal(v).setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        } catch (e: ArithmeticException) {
            "Деление на 0"
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(val s: String) {
        var p = 0
        fun parse(): Double {
            val v = expr()
            if (p < s.length) throw IllegalArgumentException()
            return v
        }
        fun expr(): Double {
            var v = term()
            while (p < s.length && (s[p] == '+' || s[p] == '−')) {
                val o = s[p++]
                val t = term()
                v = if (o == '+') v + t else v - t
            }
            return v
        }
        fun term(): Double {
            var v = unary()
            while (p < s.length && (s[p] == '×' || s[p] == '÷')) {
                val o = s[p++]
                val t = unary()
                if (o == '×') v *= t else {
                    if (t == 0.0) throw ArithmeticException()
                    v /= t
                }
            }
            return v
        }
        fun unary(): Double {
            if (p < s.length && s[p] == '−') { p++; return -unary() }
            return postfix()
        }
        fun postfix(): Double {
            var v = number()
            while (p < s.length && s[p] == '%') { p++; v /= 100 }
            return v
        }
        fun number(): Double {
            val st = p
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++
            return s.substring(st, p).toDouble()
        }
    }
}

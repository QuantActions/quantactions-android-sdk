package com.quantactions.sdktestapp.core_ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.quantactions.sdktestapp.core_ui.theme.Brand
import com.quantactions.sdktestapp.R


val Roboto = FontFamily(
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_regular, FontWeight.Normal)
)

// Set of Material typography styles to start with
object TP {

    val regular = Typography(
        h4 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.15.sp
        ),
        h1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.21.sp
        ),
        h2 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.15.sp
        ),
        caption = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        subtitle1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 9.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        body1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        h6 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        body2 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        overline = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        h5 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.21.sp
        )
    )

    val medium = Typography(
        h6 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        h5 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        body2 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        h1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            textDecoration = TextDecoration.Underline,
            color = Brand
        ),
        body1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        overline = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        ),
        caption = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
    )

    val light = Typography(
        h1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Light,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            letterSpacing = -(0.48.sp)
        ),
        h2 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Light,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.63.sp
        )
    )

    val one = Typography(
        body1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),

        h3 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),

        h1 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        h2 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            textDecoration = TextDecoration.Underline,
            color = Brand
        ),

        // Metric row title
        h4 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Light,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            letterSpacing = -(0.48.sp)
        ),

        // Metric row gain
        h5 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),

        // Graph labels
        h6 = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),

        overline = TextStyle(
            fontFamily = Roboto,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        )
    )
}


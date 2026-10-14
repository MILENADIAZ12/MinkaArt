package com.minkaart2.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.minkaart2.app.R
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.theme.AzulAcero
import com.minkaart2.app.ui.theme.Blanco
import com.minkaart2.app.ui.theme.Guinda
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlin.random.Random

// logo1.png es blanco sobre vinotinto. Este filtro convierte la luminancia en
// transparencia (el fondo vinotinto queda transparente, las letras opacas) y pinta todo
// con Guinda, así el logo se ve sobre fondo blanco sin necesitar otro archivo.
private fun filtroLogoSobreBlanco(): ColorFilter {

    val r = Guinda.red * 255f
    val g = Guinda.green * 255f
    val b = Guinda.blue * 255f

    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0f, 0f, 0f, 0f, r,
                0f, 0f, 0f, 0f, g,
                0f, 0f, 0f, 0f, b,
                0.39f, 1.29f, 0.13f, 0f, -51f
            )
        )
    )
}

// Hebras de la pincelada, generadas una sola vez: posición a lo ancho (-1..1), dónde
// empieza y termina cada una (las puntas irregulares dan la textura de brocha seca),
// grosor y opacidad.
private class Hebra(
    val desplazamiento: Float,
    val inicio: Float,
    val fin: Float,
    val grosor: Float,
    val alfa: Float
)

private fun generarHebras(): List<Hebra> {

    val azar = Random(7)

    return List(26) { i ->

        val desplazamiento = -1f + 2f * i / 25f + (azar.nextFloat() - 0.5f) * 0.05f
        val borde = kotlin.math.abs(desplazamiento)

        Hebra(
            desplazamiento = desplazamiento,
            // Las hebras de los bordes empiezan tarde y terminan antes: punta desgastada.
            inicio = azar.nextFloat() * 0.06f + borde * 0.05f,
            fin = 0.9f + azar.nextFloat() * 0.1f - borde * azar.nextFloat() * 0.18f,
            grosor = 0.55f + azar.nextFloat() * 0.8f,
            alfa = 0.55f + azar.nextFloat() * 0.4f
        )
    }
}

@Composable
fun SplashScreen(
    navController: NavHostController
) {
    val progreso = remember { Animatable(0f) }
    val hebras = remember { generarHebras() }
    val colorPincelada = AzulAcero
    val filtroLogo = remember { filtroLogoSobreBlanco() }

    LaunchedEffect(Unit) {

        delay(350)

        progreso.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )

        delay(650)

        navController.navigate(Routes.WELCOME) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 56.dp)
                .aspectRatio(415f / 431f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize(),
                colorFilter = filtroLogo
            )

            // Una sola pincelada gruesa que cruza la "K" de MINKA, a medio trazar:
            // el progreso nunca llega a "limpiar" el logo, queda como trabajo en curso.
            Canvas(modifier = Modifier.fillMaxSize()) {

                val ancho = size.width
                val alto = size.height

                // Eje de la pincelada (coordenadas relativas al logo): sube de izquierda a derecha.
                val x0 = 0.36f * ancho
                val y0 = 0.52f * alto
                val x1 = 0.80f * ancho
                val y1 = 0.30f * alto
                val ctrlX = 0.58f * ancho
                val ctrlY = 0.30f * alto

                val dx = x1 - x0
                val dy = y1 - y0
                val largo = sqrt(dx * dx + dy * dy)
                val normalX = -dy / largo
                val normalY = dx / largo
                val mitadAncho = 0.055f * ancho
                val pasos = 36

                fun punto(t: Float): Offset {
                    val u = 1f - t
                    return Offset(
                        u * u * x0 + 2f * u * t * ctrlX + t * t * x1,
                        u * u * y0 + 2f * u * t * ctrlY + t * t * y1
                    )
                }

                hebras.forEach { hebra ->

                    val hasta = minOf(hebra.fin, progreso.value)

                    if (hasta <= hebra.inicio) return@forEach

                    val desplazamiento = hebra.desplazamiento * mitadAncho
                    val grosor = hebra.grosor * mitadAncho * 0.16f

                    val numero = (pasos * (hasta - hebra.inicio)).toInt().coerceAtLeast(1)

                    for (k in 0 until numero) {

                        val t0 = hebra.inicio + (hasta - hebra.inicio) * k / numero
                        val t1 = hebra.inicio + (hasta - hebra.inicio) * (k + 1) / numero

                        // Pequeños huecos (cerdas que "saltan") para el aspecto de brocha seca.
                        if (((k * 7 + (hebra.desplazamiento * 100).toInt()) % 11) == 0) continue

                        val a = punto(t0)
                        val b = punto(t1)

                        drawLine(
                            color = colorPincelada,
                            start = Offset(a.x + normalX * desplazamiento, a.y + normalY * desplazamiento),
                            end = Offset(b.x + normalX * desplazamiento, b.y + normalY * desplazamiento),
                            strokeWidth = grosor,
                            cap = StrokeCap.Round,
                            alpha = hebra.alfa
                        )
                    }
                }
            }
        }
    }
}

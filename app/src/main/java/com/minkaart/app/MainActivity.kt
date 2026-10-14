package com.minkaart2.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.minkaart2.app.navigation.AppNavigation
import com.minkaart2.app.ui.theme.MinkaArt_btaTheme
import com.minkaart2.app.util.SesionLocal
import com.minkaart2.app.vision.MotorHuellaVisual

private const val EXTRA_TIPO_NOTIFICACION = "tipo"
private const val EXTRA_REFERENCIA_NOTIFICACION = "referenciaId"

class MainActivity : AppCompatActivity() {

    // Tipo + referencia de la notificación que abrió la app (MinkaMessagingService) —
    // mutableStateOf para que AppNavigation reaccione tanto al arranque en frío
    // (onCreate) como si la app ya estaba abierta (onNewIntent, gracias a
    // android:launchMode="singleTop" en el manifest).
    private var tipoDesdeNotificacion by mutableStateOf<String?>(null)
    private var referenciaDesdeNotificacion by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // El Image Embedder de MediaPipe necesita un Context para leer el modelo
        // .tflite de assets — se guarda una sola vez aquí; el propio ImageEmbedder se
        // crea perezoso, en segundo plano, la primera vez que se calcula una huella.
        MotorHuellaVisual.inicializar(applicationContext)
        SesionLocal.inicializar(applicationContext)

        tipoDesdeNotificacion = intent?.getStringExtra(EXTRA_TIPO_NOTIFICACION)
        referenciaDesdeNotificacion = intent?.getStringExtra(EXTRA_REFERENCIA_NOTIFICACION)

        setContent {
            MinkaArt_btaTheme {
                AppNavigation(
                    tipoInicial = tipoDesdeNotificacion,
                    referenciaInicial = referenciaDesdeNotificacion
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tipoDesdeNotificacion = intent.getStringExtra(EXTRA_TIPO_NOTIFICACION)
        referenciaDesdeNotificacion = intent.getStringExtra(EXTRA_REFERENCIA_NOTIFICACION)
    }
}

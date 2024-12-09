package com.pharari.thiago.marketmap

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale

object Constantes {

    const val anuncio_disponible = "Disponible"
    const val anuncio_vendido = "Vendido"

    val categorias = arrayOf(
        "Móviles",
        "Ordenadores/Laptops",
        "Electrónica y electrodomésticos",
        "Vehículos",
        "Consolas y videojuegos",
        "Hogar y muebles",
        "Belleza y cuidado personal",
        "Libros",
        "Deportes",
        "Otros"
    )

    val condiciones = arrayOf(
        "Nuevo",
        "Usado",
        "Renovado",
        "Desgastado"
    )

    fun obtenerTiempoDis() : Long {
        return System.currentTimeMillis()
    }
    fun obtenerFecha(tiempo : Long): String {
        val calendario = Calendar.getInstance(Locale.ENGLISH)
        calendario.timeInMillis = tiempo

        return DateFormat.format("dd/MM/yyyy", calendario).toString()
    }

}
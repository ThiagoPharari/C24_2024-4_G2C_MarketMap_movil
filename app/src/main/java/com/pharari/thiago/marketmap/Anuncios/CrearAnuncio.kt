package com.pharari.thiago.marketmap.Anuncios

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.pharari.thiago.marketmap.Adaptadores.AdaptadorImagenSeleccionada
import com.pharari.thiago.marketmap.Constantes
import com.pharari.thiago.marketmap.Modelo.ModeloImageSeleccionada
import com.pharari.thiago.marketmap.R
import com.pharari.thiago.marketmap.SeleccionarUbicacion
import com.pharari.thiago.marketmap.databinding.ActivityCrearAnuncioBinding

class CrearAnuncio : AppCompatActivity() {

    private lateinit var binding: ActivityCrearAnuncioBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var imagenUri : Uri?= null

    private lateinit var imagenSelecArrayList: ArrayList<ModeloImageSeleccionada>
    private lateinit var adaptadorImagenSelec : AdaptadorImagenSeleccionada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearAnuncioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        val adaptadorCat = ArrayAdapter(this, R.layout.item_categoria, Constantes.categorias)

        binding.Categoria.setAdapter(adaptadorCat)

        val adaptadorCon = ArrayAdapter(this, R.layout.item_condicion, Constantes.condiciones)

        binding.Condicion.setAdapter(adaptadorCon)

        imagenSelecArrayList = ArrayList()
        cargarImagenes()

        binding.agregarImg.setOnClickListener {
            mostrarOpciones()
        }

        binding.Locacion.setOnClickListener {
            val intent = Intent(this, SeleccionarUbicacion::class.java)
            seleccionarUbicacion_ARL.launch(intent)
        }

        binding.BtnCrearUsuario.setOnClickListener {
            validarDatos()
        }
    }

    private var marca = ""
    private var categoria = ""
    private var condicion = ""
    private var direccion = ""
    private var precio = ""
    private var titulo = ""
    private var descripcion = ""
    private var latitud = 0.0
    private var longitud = 0.0

    private fun validarDatos(){
        marca = binding.EtMarca.text.toString().trim()
        categoria = binding.Categoria.text.toString().trim()
        condicion = binding.Condicion.text.toString().trim()
        direccion = binding.Locacion.text.toString().trim()
        precio = binding.EtPrecio.text.toString().trim()
        titulo = binding.EtTitulo.text.toString().trim()
        descripcion = binding.EtDescripcion.text.toString().trim()

        if (marca.isEmpty()) {
            binding.EtMarca.error = "Ingrese una marca"
            binding.EtMarca.requestFocus()
        } else if (categoria.isEmpty()) {
            binding.Categoria.error = "Ingrese una categoría"
            binding.Categoria.requestFocus()
        } else if (condicion.isEmpty()) {
            binding.Condicion.error = "Ingrese una condición"
            binding.Condicion.requestFocus()
        } else if (direccion.isEmpty()) {
            binding.Locacion.error = "Ingrese una Locación"
            binding.Locacion.requestFocus()
        } else if (precio.isEmpty()) {
            binding.EtPrecio.error = "Ingrese un precio"
            binding.EtPrecio.requestFocus()
        } else if (titulo.isEmpty()) {
            binding.EtTitulo.error = "Ingrese un título"
            binding.EtTitulo.requestFocus()
        } else if (descripcion.isEmpty()) {
            binding.EtDescripcion.error = "Ingrese una descripción"
            binding.EtDescripcion.requestFocus()
        } else if (imagenUri == null) {
            Toast.makeText(this, "Agregue al menos una imagen", Toast.LENGTH_SHORT).show()
        } else {
            agregarAnuncio()
        }


    }

    private fun agregarAnuncio() {
        progressDialog.setMessage("Agregando anuncio")
        progressDialog.show()

        val  tiempo = Constantes.obtenerTiempoDis()

        val ref = FirebaseDatabase.getInstance().getReference("Anuncios")
        val keyId = ref.push().key

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "${keyId}"
        hashMap["uid"] = "${firebaseAuth.uid}"
        hashMap["marca"] = "${marca}"
        hashMap["categoria"] = "${categoria}"
        hashMap["condicion"] = "${condicion}"
        hashMap["direccion"] = "${direccion}"
        hashMap["precio"] = "${precio}"
        hashMap["titulo"] = "${titulo}"
        hashMap["descripcion"] = "${descripcion}"
        hashMap["estado"] = "${Constantes.anuncio_disponible}"
        hashMap["tiempo"] = "${tiempo}"
        hashMap["latitud"] = latitud
        hashMap["longitud"] = longitud

        ref.child(keyId!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                cargarImagenesStorage(keyId)
            }
            .addOnFailureListener { e->
                Toast.makeText(
                    this, "${e.message}", Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun cargarImagenesStorage(keyId: String) {
        for (i in imagenSelecArrayList.indices) {
            val modeloImagenSel = imagenSelecArrayList[i]
            val nombreImagen = modeloImagenSel.id
            val rutaNombreImagen = "Anuncios/$nombreImagen"

            val storageReference = FirebaseStorage.getInstance().getReference(rutaNombreImagen)
            storageReference.putFile(modeloImagenSel.imagenUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val urlImgCargada = uriTask.result

                    if (uriTask.isSuccessful) {
                        val hashMap = HashMap<String, Any>()
                        hashMap["id"] = "${modeloImagenSel.imagenUri}"
                        hashMap["imagenUrl"] = "$urlImgCargada"

                        val ref = FirebaseDatabase.getInstance().getReference("Anuncios")
                        ref.child(keyId).child("Imagenes")
                            .child(nombreImagen)
                            .updateChildren(hashMap)
                    }
                    progressDialog.dismiss()
                    Toast.makeText(this, "Se publicó su anuncio", Toast.LENGTH_SHORT).show()
                    limpiarCampos()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private val seleccionarUbicacion_ARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado->
            if(resultado.resultCode == Activity.RESULT_OK){
                val data = resultado.data
                if (data != null){
                    latitud = data.getDoubleExtra("latitud", 0.0)
                    longitud = data.getDoubleExtra("longitud", 0.0)
                    direccion = data.getStringExtra("direccion") ?: ""

                    binding.Locacion.setText(direccion)
                }
            }else{
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }

        }


    private fun limpiarCampos(){
        imagenSelecArrayList.clear()
        adaptadorImagenSelec.notifyDataSetChanged()
        binding.EtMarca.setText("")
        binding.Categoria.setText("")
        binding.Condicion.setText("")
        binding.Locacion.setText("")
        binding.EtPrecio.setText("")
        binding.EtTitulo.setText("")
        binding.EtDescripcion.setText("")
    }


    private fun mostrarOpciones() {
        val popupMenu = PopupMenu(this, binding.agregarImg)

        popupMenu.menu.add(Menu.NONE,1,1,"Cámara")
        popupMenu.menu.add(Menu.NONE,2,2,"Galería")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item->
            val itemId = item.itemId
            if (itemId == 1){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    solicitarPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                    solicitarPermisoCamara.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
            }else if (itemId == 2){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    imagenGaleria()
                }else{
                    solicitarPermisoAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    private val solicitarPermisoAlmacenamiento = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){esConcedido->
        if (esConcedido){
            imagenGaleria()
        }else{
            Toast.makeText(
                this,
                "El acceso fue denegado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_ARL.launch(intent)
    }

    private val resultadoGaleria_ARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado->
            if (resultado.resultCode == Activity.RESULT_OK){
                val data = resultado.data
                imagenUri = data!!.data

                val tiempo = "${Constantes.obtenerTiempoDis()}"
                val modeloImgSel = ModeloImageSeleccionada(
                    tiempo, imagenUri,null, false
                )
                imagenSelecArrayList.add(modeloImgSel)
                cargarImagenes()

            }else{
                Toast.makeText(
                    this,
                    "Cancelado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val solicitarPermisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){ resultado->
        var todosConcedidos = true
        for (esConcedido in resultado.values){
            todosConcedidos = todosConcedidos && esConcedido
        }

        if (todosConcedidos){
            imageCamara()
        }else{
            Toast.makeText(
                this,
                "El permiso de la cámara o almacenamiento ha sido denegada, o ambas fueron denegadas",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun imageCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE,"Titulo_imagen")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Descripcion_imagen")
        imagenUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri)
        resultadoCamara_ARL.launch(intent)
    }

    private val resultadoCamara_ARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){resultado->
            if (resultado.resultCode == Activity.RESULT_OK){

                val tiempo = "${Constantes.obtenerTiempoDis()}"
                val modeloImgSel = ModeloImageSeleccionada(
                    tiempo, imagenUri,null,false
                )
                imagenSelecArrayList.add(modeloImgSel)
                cargarImagenes()
            }else {
                Toast.makeText(
                    this,
                    "Cancelado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun cargarImagenes(){
        adaptadorImagenSelec = AdaptadorImagenSeleccionada(this, imagenSelecArrayList)
        binding.RVImagenes.adapter = adaptadorImagenSelec
    }
}
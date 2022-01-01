package com.example.reversigame

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.text.InputFilter
import android.text.Spanned
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import android.util.Log
import android.util.Patterns
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.reversigame.databinding.ActivityStartBinding
import kotlinx.android.synthetic.main.activity_start.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StartActivity : AppCompatActivity(){

    private lateinit var b : ActivityStartBinding
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStartBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        b.btModo1.setOnClickListener {
            if(b.nomeJogador.text.equals(""))
                return@setOnClickListener
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(MainActivity.jogadorUm, b.nomeJogador.text)
            startActivity(intent)
        }

        b.btModo2.setOnClickListener {
            b.btModo2.visibility = View.INVISIBLE
            b.btModo2.width = 1
            b.btModoCliente.visibility = View.VISIBLE
            b.btModoServer.visibility = View.VISIBLE
        }

        b.btModoServer.setOnClickListener {
            val intent = Intent(this, TwoServerActivity::class.java)
            intent.putExtra(MainActivity.jogadorUm, b.nomeJogador.text)
            startActivity(intent)
        }

        b.btModoCliente.setOnClickListener {
            val intent = Intent(this, TwoClientActivity::class.java)
            intent.putExtra(MainActivity.jogadorUm, b.nomeJogador.text)
            startActivity(intent)
        }

        b.btNome.setOnClickListener {
            setupDialogo()
        }

        val nome = loadData(this)
        if(!nome.equals(""))
            b.nomeJogador.setText(nome)
        setFoto()

        b.btTirarFoto.setOnClickListener{
            if (allPermissionsGranted()) {
                b.cameraCaptureButton.visibility = View.VISIBLE
                b.viewFinder.visibility = View.VISIBLE
                b.btModo1.visibility = View.GONE
                b.btModo2.visibility = View.GONE
                b.btModo3.visibility = View.GONE
                b.btTirarFoto.visibility = View.GONE
                b.btNome.visibility = View.GONE
                b.nomeJogador.visibility = View.GONE
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), 10)
            }
        }
        b.cameraCaptureButton.setOnClickListener {
            Log.d("TagCheck","A tirar foto...")
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupDialogo(){
        val edtBox = EditText(this).apply {
            maxLines = 1
        }

        val dlg = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.nome_jogador))
            setMessage(getString(R.string.ask_name))
            setPositiveButton("Confirmar") { _: DialogInterface, _: Int ->
                val strIP = edtBox.text.toString()
                if (strIP.isEmpty()) {
                    Toast.makeText(this@StartActivity, getString(R.string.error_address), Toast.LENGTH_LONG).show()
                } else {
                    b.nomeJogador.setText(strIP)
                    saveData(this@StartActivity,strIP)
                }
            }
            setCancelable(true)
            setView(edtBox)
            create()
        }
        dlg.show()
    }

    private fun saveData(context: Context, nome: String) {
        val sharedPreferences = context.getSharedPreferences("perfilJogador", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("nomeJogador", nome)
        editor.apply()
    }

    private fun loadData(context: Context) : String? {
        val sharedPreferences = context.getSharedPreferences("perfilJogador", MODE_PRIVATE)
        val text = sharedPreferences.getString("nomeJogador", "")
        return text
    }

    private fun getOutputDirectory(): File {
        val dir = File(getFilesDir(), "pastaFoto");
        if(!dir.exists()){
            dir.mkdir();
        }
        return dir
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e("TagError", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            "foto.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        Log.d("TagCheck","A tirar foto.....")
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("TagError", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Foto tirada com sucesso: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d("TagCheck", msg)
                    val sharedPreferences = this@StartActivity.getSharedPreferences("perfilJogador", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("fotoJogador", savedUri.toString())
                    editor.apply()
                    val myBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath())
                    b.fotoPerfil.setImageBitmap(myBitmap)
                    b.cameraCaptureButton.visibility = View.GONE
                    b.viewFinder.visibility = View.GONE
                    b.btModo1.visibility = View.VISIBLE
                    b.btModo2.visibility = View.VISIBLE
                    b.btModo3.visibility = View.VISIBLE
                    b.btTirarFoto.visibility = View.VISIBLE
                    b.btNome.visibility = View.VISIBLE
                    b.nomeJogador.visibility = View.VISIBLE
                }
            })
    }

    private fun setFoto(){
        val sharedPreferences = this@StartActivity.getSharedPreferences("perfilJogador", MODE_PRIVATE)
        val uri = sharedPreferences.getString("fotoJogador", "")?.toUri()
        if(uri == null)
            return
        val imgFile = File(uri.path)
        if(imgFile.exists()){
            val myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
            b.fotoPerfil.setImageBitmap(myBitmap)
        }
    }
}

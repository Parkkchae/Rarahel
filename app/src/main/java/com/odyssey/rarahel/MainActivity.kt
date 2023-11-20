package com.odyssey.rarahel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import kotlin.random.Random
import android.widget.TextView
import android.location.Location
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.scheduling.DefaultIoScheduler.executor


class MainActivity : AppCompatActivity() {
    private lateinit var speedTextView: TextView
    private var lastLocation: Location? = null
    private var lastTime: Long = 0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult?.lastLocation?.let { currentLocation ->
                val currentTime = System.currentTimeMillis()

                if (lastLocation != null) {
                    val distance = lastLocation?.distanceTo(currentLocation) ?: 0.0
                    val timeDifference = (currentTime - lastTime) / 1000.0
                    val speed = distance / timeDifference // 속도 계산 (미터/초)

                    speedTextView.text = String.format("%.2f m/s", speed)
                }

                lastLocation = currentLocation
                lastTime = currentTime
            }
        }
    private val permissionsRequestCode = Random.nextInt(0, 10000)
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>



    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        } else {
            bindCameraUseCases()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)

        // 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        lastTime = System.currentTimeMillis()

        // FusedLocationProviderClient 초기화
        fusedLocationClient = FusedLocationProviderClient(this)
    }
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 1) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 허용되면 위치 업데이트 시작
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "위치 권한을 허용해야 속도를 측정할 수 있습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun startLocationUpdates() {
            // 위치 업데이트 관련 설정 및 리스너 등록
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000) // 위치 업데이트 주기 (1초마다)
                .setFastestInterval(1000)

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }
    private fun bindCameraUseCases() {
        var cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    fun bindPreview(cameraProvider : ProcessCameraProvider) {
        var preview : Preview = Preview.Builder()
            .build()

        var cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val previewView = this.findViewById<PreviewView>(R.id.previewView1)
        preview.setSurfaceProvider(previewView.getSurfaceProvider())

        val imageAnalysis = ImageAnalysis.Builder()
            // enable the following line if RGBA output is needed.
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // insert your code here.
            // after done, release the ImageProxy object
            imageProxy.close()
        })

        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }
}

class LocationRequest {

}

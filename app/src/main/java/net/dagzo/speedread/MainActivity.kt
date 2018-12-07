package net.dagzo.speedread

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.facebook.stetho.Stetho
import kotlinx.android.synthetic.main.activity_main.*

/**
 * - 画像内文字認識
 * - 形態素解析
 * - WindowManager
 * https://akira-watson.com/android/windowmanager.html
 * - スクショ
 * https://techbooster.org/android/application/17026/
 * https://akira-watson.com/android/screenshot.html
 */
class MainActivity : AppCompatActivity() {

    private val rcOverlay = 1
    private val rcScreenshot = 2

    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var data: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Stetho.initializeWithDefaults(this)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            text_desc.setText(R.string.guide_desc_test)
            checkScreenshotPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.rcOverlay && resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.please_allow_permission, Toast.LENGTH_SHORT).show()
                } else {
                    startService()
                }
            }
        } else if (requestCode == rcScreenshot) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, R.string.please_allow_permission, Toast.LENGTH_SHORT).show()
                return
            }
            this.data = data
            checkOverlayPermission()
        }
    }

    private fun checkScreenshotPermission() {
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mMediaProjectionManager!!.createScreenCaptureIntent()
        startActivityForResult(permissionIntent, rcScreenshot)
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, rcOverlay)
            } else {
                startService()
            }
        } else {
            startService()
        }
    }

    private fun startService() {
        MainService.start(application, data!!)
    }

}

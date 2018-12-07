package net.dagzo.speedread

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.support.annotation.NonNull
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit


class MainService : Service() {

    private val compositeDisposable = CompositeDisposable()
    private var view: View? = null
    private var trimView: TrimView? = null
    private var windowManager: WindowManager? = null

    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mImageReader: ImageReader? = null // スクリーンショット用
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    companion object {

        private fun isOreoOrLater(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }

        fun start(@NonNull context: Context, @NonNull data: Intent) {
            val intent = Intent(context, MainService::class.java)
            intent.putExtra("data", data)
            if (isOreoOrLater()) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val data: Intent = intent.getParcelableExtra("data")
        setUpScreenshot(data)

        setUpNotification(intent)
        setUpWindowManager()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(view)
        mMediaProjection?.stop()
        mVirtualDisplay?.release()
        compositeDisposable.clear()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun setUpNotification(intent: Intent) {
        val context = applicationContext
        val channelId = "default"
        val title = context.getString(R.string.app_name)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isOreoOrLater()) {
            val channel = NotificationChannel(channelId, "title", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = if (isOreoOrLater())
            Notification.Builder(this, channelId)
        else
            Notification.Builder(this)

        val notification = builder
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.btn_star)
            .setContentText("APPLICATION_OVERLAY")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .build()
        startForeground(1, notification)
    }

    private fun setUpWindowManager() {
        val layoutInflater = LayoutInflater.from(this)
        val typeLayer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_TOAST
        }
        windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            typeLayer,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        val nullParent: ViewGroup? = null
        view = layoutInflater.inflate(R.layout.service_layer, nullParent)

        view?.findViewById<Button>(R.id.button_start)?.setOnClickListener {
            clickStart()
        }

        view?.findViewById<Button>(R.id.button_stop)?.setOnClickListener {
            stopSelf()
        }

        trimView = view?.findViewById<TrimView>(R.id.trim_view)!!
        trimView!!.sizeSet(mWidth, mHeight)

        view?.findViewById<Button>(R.id.button_trim)?.setOnClickListener {
            trimView!!.visibility = if (trimView!!.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        windowManager!!.addView(view, params)
    }

    private fun clickStart() {
        // 画面のスクショを取るため不要なレイアウトを一時的に GONE する
        view!!.visibility = View.GONE
        updateView()
        compositeDisposable.add(
            Observable
                .interval(100, TimeUnit.MILLISECONDS)
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, { it.printStackTrace() }, {
                    recognizeBitmapTextCloud(getScreenshot())
                    view!!.visibility = View.VISIBLE
                    trimView!!.visibility = View.GONE
                    updateView()
                })
        )
    }

    private fun setUpScreenshot(data: Intent) {
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaProjection = mMediaProjectionManager!!.getMediaProjection(Activity.RESULT_OK, data)

        val metrics: DisplayMetrics = resources.displayMetrics
        mWidth = metrics.widthPixels
        mHeight = metrics.heightPixels
        val density = metrics.densityDpi

//            mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.RGB_565, 2)
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)

        mVirtualDisplay = mMediaProjection!!
            .createVirtualDisplay(
                "Capturing Display",
                mWidth, mHeight, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader!!.surface, null, null
            )
    }

    private fun getScreenshot(): Bitmap {
        // ImageReaderから画面を取り出す
        var image: Image = mImageReader!!.acquireLatestImage()
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer

        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * mWidth

        // バッファからBitmapを生成
        var bitmap = Bitmap.createBitmap(
            mWidth + rowPadding / pixelStride, mHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val trimData = trimView!!.trimData

        bitmap = Bitmap.createBitmap(
            bitmap,
            trimData[0],
            trimData[1],
            trimData[2] - trimData[0],
            trimData[3] - trimData[1],
            null,
            true
        )
        return bitmap
    }

    private fun recognizeBitmapText(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer

        textRecognizer.processImage(image)
            .addOnSuccessListener {
                val result = it.text
                parse(result.replace("\n", ""))
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    private fun recognizeBitmapTextCloud(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
            .setLanguageHints(Arrays.asList("en", "ja"))
            .build()
        val textRecognizer = FirebaseVision.getInstance()
            .getCloudTextRecognizer(options)

        textRecognizer.processImage(image)
            .addOnSuccessListener {
                val result = it.text
                parse(result.replace("\n", ""))
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    private fun parse(text: String) {
        val disposable = Api().parse(getString(R.string.yahoo_application_id), text)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (response == null || response.total == 0) {
                    return@subscribe
                }
                val list: MutableList<String> = ArrayList()
                var i = 0
                val size = response.words.size - 1
                while (i < size) {
                    val item = response.words[i]
                    var value = item.surface
                    i++
                    while (true) {
                        if (i == size) break
                        if (canJoinNext(response.words[i].pos)) {
                            value += response.words[i].surface
                            i++
                        } else {
                            break
                        }
                    }
                    list.add(value)
                }
                list.add(getString(R.string.flash_end))
                flashWords(list)
            }, { err ->
                err.printStackTrace()
            })
        compositeDisposable.add(disposable)
    }

    private fun canJoinNext(pos: String): Boolean {
        return pos == "助詞" || pos == "特殊" || pos == "接尾辞" || pos == "助動詞"
    }

    private fun flashWords(words: List<String>) {
        compositeDisposable.add(
            Observable.fromIterable(words)
                .concatMap<Any> { i -> Observable.just(i).delay(300, TimeUnit.MILLISECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        view?.findViewById<TextView>(R.id.text_view)?.text = it.toString()
                        updateView()
                    },
                    { it.printStackTrace() }, {
                    }
                )
        )
    }

    private fun updateView() {
        val param = view!!.layoutParams as WindowManager.LayoutParams
        windowManager?.updateViewLayout(view, param)
    }
}
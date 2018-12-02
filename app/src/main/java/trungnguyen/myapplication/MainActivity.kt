package trungnguyen.myapplication

import android.app.AlertDialog
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class MainActivity : AppCompatActivity() {

    private var mText: TextView? = null
    private val mSubscription = CompositeDisposable()
    private var mX509Manager: X509TrustManager? = null
    private var mCache: Cache? = null

    private var mTittle: TextView? = null
    private var imageChangeBroadcastReceiver: ImageChangeBroadcastReceiver? = null
    private var enableNotificationListenerAlertDialog: AlertDialog? = null

    private val isNotificationServiceEnabled: Boolean
        get() {
            val pkgName = packageName
            val flat = Settings.Secure.getString(contentResolver,
                    ENABLED_NOTIFICATION_LISTENERS)
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in names.indices) {
                    val cn = ComponentName.unflattenFromString(names[i])
                    if (cn != null) {
                        if (TextUtils.equals(pkgName, cn.packageName)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // If the user did not turn the notification listener service on we prompt him to do so
        if (!isNotificationServiceEnabled) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog()
            enableNotificationListenerAlertDialog!!.show()
        }
        mText = findViewById(R.id.text)
        mTittle = findViewById(R.id.title)
        // Finally we register a receiver to tell the MainActivity when a notification has been received
        imageChangeBroadcastReceiver = ImageChangeBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.trungnguyen.myapplication.notificationlistenerexample")
        registerReceiver(imageChangeBroadcastReceiver, intentFilter)

        mX509Manager = provideX509TrustManager()
        mCache = provideOkHttpCache(this)

    }

    private fun networkModule(): DummyApi {
        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(provideOkHttpClientTimeoutLonger(mCache!!, mX509Manager!!))
                .build()
        return retrofit.create(DummyApi::class.java)
    }


    internal fun provideOkHttpCache(application: Context): Cache {
        val cacheSize = 50 * 1024 * 1024 // 10 MiB
        return Cache(application.cacheDir, cacheSize.toLong())
    }

    fun provideX509TrustManager(): X509TrustManager? {
        var x509TrustManager: X509TrustManager? = null
        try {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            val trustManagers = factory.trustManagers
            x509TrustManager = trustManagers[0] as X509TrustManager
        } catch (exception: NoSuchAlgorithmException) {
            //            Timber.d("not trust manager available");
        } catch (exception: KeyStoreException) {
        }

        return x509TrustManager
    }

    internal fun provideOkHttpClientTimeoutLonger(cache: Cache,
                                                  trustManager: X509TrustManager?): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "debugproguard") {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }
        builder.cache(cache)
        builder.connectionPool(ConnectionPool(1,
                5, TimeUnit.MINUTES))
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val cs = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build()

            val specs = ArrayList<ConnectionSpec>()
            specs.add(cs)
            specs.add(ConnectionSpec.COMPATIBLE_TLS)
            specs.add(ConnectionSpec.CLEARTEXT)

            builder.sslSocketFactory(TLSSocketFactory(), trustManager!!)
            builder.connectionSpecs(specs)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mSubscription?.clear()
        unregisterReceiver(imageChangeBroadcastReceiver)
    }

    private fun changeInterceptedNotificationImage(notificationCode: Bundle?) {
        if (mText == null || mTittle == null) {
            return
        }
        val tittle = notificationCode!!.getString("NOTIFY_TITLE", "")
        val text = notificationCode.getString("NOTIFY_TEXT", "")
        mText?.text = text
        mTittle?.text = tittle

        val subscription = networkModule().callDummyApi(text, tittle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this@MainActivity, "Successful push data to cloud", Toast.LENGTH_SHORT).show()
                }, {
                    Log.d("MainActivity", it.message)
                })

        mSubscription.add(subscription)
    }


    inner class ImageChangeBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            changeInterceptedNotificationImage(bundle)
        }
    }

    private fun buildNotificationServiceAlertDialog(): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(R.string.notification_listener_service)
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation)
        alertDialogBuilder.setPositiveButton(R.string.yes
        ) { dialog, id -> startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        alertDialogBuilder.setNegativeButton(R.string.no
        ) { dialog, id ->
            // If you choose to not enable the notification listener
            // the app. will not work as expected
        }
        return alertDialogBuilder.create()
    }

    companion object {
        private val BASE_URL = "http://demo.com/";
        private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    }
}

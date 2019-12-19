package com.ruhsuzsirin.pcuzaktankontrolwifi

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.VolumeProviderCompat
import java.io.IOException
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

class MyService: Service() {

    lateinit var mediaSession: MediaSessionCompat;

    var ip = "";
    var port = "";

    val DEBUG_TAG = "MYSERVICE";
    var wakeLock:PowerManager.WakeLock? = null;
    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate() {
        super.onCreate()

        // kaynak : https://developer.android.com/reference/android/os/PowerManager.html
        /*
           Flag Value	            CPU	    Screen	Keyboard
           PARTIAL_WAKE_LOCK	    On* 	Off	    Off
           SCREEN_DIM_WAKE_LOCK	    On	    Dim	    Off
           SCREEN_BRIGHT_WAKE_LOCK	On	    Bright	Off
           FULL_WAKE_LOCK	        On      Bright  Bright
        */
        // ########## Servis işlem yapmadığında CPU otomatik servisi uyku moduna alacaktır bu yüzden CPU yu bu servisi kısmı bir süre açık tutabilmemiz için WAKE_LOCK kullanıyoruz. ##########
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"pcuzaktankontrolwifipro");
        wakeLock!!.acquire()

        /*
        * NOTE:
        * Konu hakkında daha fazla bilgi için : https://stackoverflow.com/questions/10154118/listen-to-volume-buttons-in-background-service/
        * //Gerekli kütüphanelerin eklenmesi! build.grandle eklenmesi gerekiyor. API 21 Android 5.0+ Destekleyen kodlardır ! ! !
            //noinspection GradleCompatible
            implementation "com.android.support:support-compat:28.0.0"
            //noinspection GradleCompatible
            implementation "com.android.support:support-media-compat:28.0.0"
        * */

        mediaSession = MediaSessionCompat(this, "PlayerService")
        mediaSession.setFlags((MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS))
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0, 0F).build())
        val myVolumeProvider = object:VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE,100,50) {
            override fun onAdjustVolume(direction:Int) {
                //-1 = volume down, 1 = volume up, 0  = volume button released
                if(direction == 1){
                    //toast("volume up")
                    islemler.bilgisayarSesi = islemler.bilgisayarSesi + 2;
                    if(islemler.bilgisayarSesi > 100){ // 100 degerini aştıysa
                        islemler.bilgisayarSesi = 100
                    }
                    servereGonder("ses", islemler.bilgisayarSesi.toString());
                }
                else if(direction == -1){
                    //toast("volume down")
                    islemler.bilgisayarSesi = islemler.bilgisayarSesi - 2;
                    if(0 > islemler.bilgisayarSesi){ // eksilere gittiyse
                        islemler.bilgisayarSesi = 0
                    }
                    servereGonder("ses", islemler.bilgisayarSesi.toString());
                }
            }
        }
        mediaSession.setPlaybackToRemote(myVolumeProvider)
        mediaSession.setActive(true)
    }
    /*
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable*/

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //toast("Servis Başlatıldı.")
        ip = intent.extras?.get("ip").toString();
        port = intent.extras?.get("port").toString();
        //bilgisayarSesSeviyesi = intent.extras?.get("ses").toString().toInt();
        Log.d(DEBUG_TAG, ip +" - "+ port + " - "+islemler.bilgisayarSesi.toString());


        //############## ÖNEMLİ SERVISI UYKU MODUNA GIRDIRMEMEK İÇİN ##############
        //kaynak : https://stackoverflow.com/questions/48903548/background-service-going-to-sleep
        //Android bataryayı korumak için bir süre sonra her servisi uyku moduna geçirecek. Bu durumu önlemek için runAsForeground kullanıldı !
        runAsForeground();

        /*mHandler = Handler()
        mRunnable = Runnable { rasgeleSayiUret() }
        mHandler.postDelayed(mRunnable, 3000)*/

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //toast("Servis Sonlandırıldı")
        //mHandler.removeCallbacks(mRunnable)
        if(wakeLock != null)
            wakeLock!!.release();
        mediaSession.release();
    }


    private fun runAsForeground() {
        val NOTIFICATION_ID = 12333;
        val notificationIntent = Intent(this, MyService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0)
        val notification = NotificationCompat.Builder(this)
            .setOngoing(true)
            .setSmallIcon(R.drawable.iconntf)
            .setContentTitle("Arka Plan SES Servisi")
            .setColor(ContextCompat.getColor(this, R.color.white))
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIFICATION_ID , notification)
    }


    fun servereGonder(tag: String, deger: String) {
        val DEBUG_TAG: String = "MYSERVICESERVERGONDER";
        thread {
            try {
                var send = tag + "=" + deger;
                val conn = Socket(ip, port.toInt());
                Log.d(DEBUG_TAG, "SEND " + send);
                conn.outputStream.write(send.toByteArray());
                conn.close();
            } catch (e: IOException) {
                Log.d(DEBUG_TAG, e.toString());
            }
        }
    }

    fun toast(str:String){
        Toast.makeText(applicationContext, str, Toast.LENGTH_SHORT).show();
    }

    // ##########################################################################################

    /*
    private fun rasgeleSayiUret() {
        val rand = Random()
        val sayi = rand.nextInt(100)
        toast("UYANIK TUT")
        mHandler.postDelayed(mRunnable, 3000)
    }*/

    //Servis durumunu öğrenmek çalışıyor mu kapalı mı ? // kullanmadım sadece not için bıraktım.
    /*private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Loop through the running services
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                // If the service is running then return true
                return true
            }
        }
        return false
    }*/

}

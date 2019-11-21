package com.ruhsuzsirin.pcuzaktankontrolwifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.net.Socket
import kotlin.concurrent.thread

class MyBroadCastReciever: BroadcastReceiver() {

    var ip = "";
    var port = "";
    val DEBUG_TAG = "MYRECIEVER";
    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action

        if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            //Toast.makeText(context, "TEST SCREEN OFF", Toast.LENGTH_SHORT).show();
            Log.d(DEBUG_TAG, ip+" - "+ port + " - "+ islemler.bilgisayarSesi);

            if(ip != "" && port != ""){
                servereGonder("sesBilgiAl", ""); // Ses bilgisini güncelle. Dinleyici islemler.kt de mevcut.
                val myIntent = Intent(context, MyService::class.java)
                myIntent.putExtra("ip", ip);
                myIntent.putExtra("port", port);
                context.startService(myIntent);
            }

        } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            //Toast.makeText(context, "TEST SCREEN ON", Toast.LENGTH_SHORT).show();
            context.stopService(Intent(context, MyService::class.java))

        } else if (action.equals("my.action.mydata")) {
            ip = intent.extras?.get("ip").toString()
            port = intent.extras?.get("port").toString()
            //Toast.makeText(context, ip.toString() + " - " + port, Toast.LENGTH_SHORT).show();
        }
        /* // Telefon ses değeri %0 olursa daha tetiklenmez %100 olursa yine daha tetiklenmez.
        else if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION"))
        {
            val newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)
            val oldVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", 0)
            if (newVolume != oldVolume)
            {
                Toast.makeText(context, oldVolume.toString()+ " >>> "+ newVolume.toString(), Toast.LENGTH_SHORT).show();
            }
        }*/

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


}

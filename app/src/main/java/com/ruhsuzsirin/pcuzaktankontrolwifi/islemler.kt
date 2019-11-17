package com.ruhsuzsirin.pcuzaktankontrolwifi


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_islemler.*

//Socket kitapları
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import java.io.BufferedReader
import java.io.IOException

//AlertDialog İçin Gerekli Kitaplar
import android.widget.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.RelativeLayout
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View.OnTouchListener
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Handler
import android.view.inputmethod.InputMethodManager


class islemler : AppCompatActivity() {

    var ip = "";
    var port = "";
    var clientip = "";

    var txtSes:TextView? = null;
    var skBarSes:SeekBar? = null;
    var bilgisayarSesi:Int = 0;

    var txtPlyrSes:TextView? = null
    var skBarPlyrSes:SeekBar? = null
    var fragmentAktif = false;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_islemler);

        val actionbar = supportActionBar;
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true)
        };


        val DEBUG_TAG:String = "ISLEM";
        val bundle = intent.extras;

        if (bundle != null) {
            if(bundle.getString("ip")!= null && bundle.getString("port")!= null && bundle.getString("clientip")!= null){
                ip = bundle.getString("ip")!!;
                port = bundle.getString("port")!!;
                clientip = bundle.getString("clientip")!!;
                Log.d(DEBUG_TAG, "Connected"+ ip + ":"+port);
                Log.d(DEBUG_TAG, "clientip="+clientip);

            }
        }

        actionbar!!.title = "İşlemler: "+ip;

        serveriOku(); //Bilgileri güncellemek için server bize sistem hakkında bilgi verecek.

        // ###############  SES ###############

        imgBtnSes.setOnClickListener(View.OnClickListener {
            val DEBUG_TAG:String = "SESCNT";

            val dialogBuilder = AlertDialog.Builder(this@islemler);
            var alertDialogSes:AlertDialog? = null;

            val inflater = this.layoutInflater;
            val dialogView = inflater.inflate(R.layout.ses, null);

            dialogBuilder.setView(dialogView);
            dialogBuilder.setOnKeyListener { v, keyCode, event -> //Yeni gelen görünümde basılan tuşları dinlemek için.

                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if(alertDialogSes != null){
                        if(alertDialogSes!!.isShowing){
                            var sesYukselt = skBarSes!!.progress + 1;
                            if(sesYukselt > 100){ // 100 degerini aştıysa
                                sesYukselt = 100;
                            }
                            skBarSes!!.setProgress(sesYukselt);
                        }
                    }
                    true;
                }
                else if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if(alertDialogSes != null){
                        if(alertDialogSes!!.isShowing){
                            var sesDusur = skBarSes!!.progress - 1;
                            if(0 > sesDusur){ // eksilere gittiyse
                                sesDusur = 0;
                            }
                            skBarSes!!.setProgress(sesDusur);

                        }
                    }
                    true;
                }

                true
            }

            //Görünüm içerisinde nesneleri tanımlama
            //skBarSes = dialogView.findViewById<SeekBar>(R.id.skBarSes);
            //txtSes = dialogView.findViewById<TextView>(R.id.txtSes);
            //VEYA
            skBarSes = dialogView.findViewById(R.id.skBarSes) as SeekBar
            txtSes = dialogView.findViewById(R.id.txtSes) as TextView

            //Nesnelerin eventlarının tanımlanması.
            skBarSes!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    txtSes!!.text = "Ses: "+i.toString();
                    servereGonder("ses", i.toString());
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            });

            //Dialog buton atamak için
            /*dialogBuilder.setNegativeButton("TAMAM"){dialog,which ->
                alertDialogSes!!.dismiss();
            }*/
            /*dialogBuilder.setPositiveButton("EVET"){dialog, which ->
            }*/
            //alertDialogSes.setCancelable(false); //sadece buton ile çıkış yapabilir.

            alertDialogSes = dialogBuilder.create();
            alertDialogSes!!.show();

            servereGonder("sesBilgiAl", "");
        });

        // ###############  PLAYER KONTROL / SES ###############

        imgBtnPlayer.setOnClickListener(View.OnClickListener {
            val DEBUG_TAG:String = "PLAYER";

            val dialogBuilder = AlertDialog.Builder(this@islemler);
            var alertDialogPlayer:AlertDialog? = null;

            val inflater = this.layoutInflater;
            val dialogView = inflater.inflate(R.layout.player, null);
            dialogBuilder.setView(dialogView);
            dialogBuilder.setOnKeyListener { v, keyCode, event ->

                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if(alertDialogPlayer != null){
                        if(alertDialogPlayer!!.isShowing){
                            val sesDzyCglt = skBarPlyrSes!!.progress + 1;
                            if(sesDzyCglt > 100){ // 100 degerini aştıysa
                                skBarPlyrSes!!.setProgress(100);
                            }
                            skBarPlyrSes!!.setProgress(sesDzyCglt);
                        }
                    }
                    true;
                }
                else if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if(alertDialogPlayer != null){
                        if(alertDialogPlayer!!.isShowing){
                            val sesDzyAzlt = skBarPlyrSes!!.progress - 1;
                            if(0 > sesDzyAzlt){ // eksilere gittiyse
                                skBarPlyrSes!!.setProgress(0);
                            }
                            skBarPlyrSes!!.setProgress(sesDzyAzlt);
                        }
                    }
                    true;
                }

                true
            }

            val btnTarayiciAc = dialogView.findViewById(R.id.btnTarayiciAc) as Button
            val edtUrl = dialogView.findViewById(R.id.edtUrl) as EditText
            val swhTarayiDurum = dialogView.findViewById(R.id.swhTarayiDurum) as Switch
            val imgBtnSesKnt = dialogView.findViewById(R.id.imgBtnSesKnt) as ImageButton
            val imgBtnSesAzalt = dialogView.findViewById(R.id.imgBtnSesAzalt) as ImageButton
            val imgBtnSesCogalt = dialogView.findViewById(R.id.imgBtnSesCogalt) as ImageButton
            val imgBtnDurdur  = dialogView.findViewById(R.id.imgBtnDurdur) as ImageButton
            val imgBtnBslt  = dialogView.findViewById(R.id.imgBtnBslt) as ImageButton
            val imgBtnOnceki  = dialogView.findViewById(R.id.imgBtnOnceki) as ImageButton
            val imgBtnSonraki  = dialogView.findViewById(R.id.imgBtnSonraki) as ImageButton

            txtPlyrSes = dialogView.findViewById(R.id.txtPlyrSes) as TextView
            skBarPlyrSes = dialogView.findViewById(R.id.skBarPlyrSes) as SeekBar

            skBarPlyrSes!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    txtPlyrSes!!.text = i.toString();
                    servereGonder("ses", i.toString());
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            });

            //Nesnelerin eventlarının tanımlanması.
            btnTarayiciAc.setOnClickListener(View.OnClickListener {
                if(swhTarayiDurum.isChecked){
                    servereGonder("tarayici", edtUrl.text.toString()+ " tarayicikpt");
                }
                else{
                    servereGonder("tarayici", edtUrl.text.toString());
                }

            });

            var imgBtnSesKntAcKpt = false;
            imgBtnSesKnt.setOnClickListener(View.OnClickListener {

                if(!imgBtnSesKntAcKpt){
                    imgBtnSesKnt.setImageResource(R.drawable.mediavolumeunmute);
                    servereGonder("anaSesDurum", "kpt");
                    imgBtnSesKntAcKpt = true;
                }
                else{
                    imgBtnSesKnt.setImageResource(R.drawable.mediavolumemute);
                    servereGonder("anaSesDurum", "ac");
                    imgBtnSesKntAcKpt = false;
                }
            });

            imgBtnSesAzalt.setOnClickListener(View.OnClickListener {
                val sesDzyAzlt = skBarPlyrSes!!.progress - 1;
                if(0 > sesDzyAzlt){ // eksilere gittiyse
                    skBarPlyrSes!!.setProgress(0);
                }
                skBarPlyrSes!!.setProgress(sesDzyAzlt);

            });

            imgBtnSesCogalt.setOnClickListener(View.OnClickListener {
                val sesDzyCglt = skBarPlyrSes!!.progress + 1;
                if(sesDzyCglt > 100){ // 100 degerini aştıysa
                    skBarPlyrSes!!.setProgress(100);
                }
                skBarPlyrSes!!.setProgress(sesDzyCglt);

            });

            imgBtnDurdur.setOnClickListener(View.OnClickListener {
                servereGonder("playerDurdur", "");
            });

            imgBtnBslt.setOnClickListener(View.OnClickListener {
                servereGonder("playerBaslat", "");
            });

            imgBtnOnceki.setOnClickListener(View.OnClickListener {
                servereGonder("playerOnceki", "");
            });

            imgBtnSonraki.setOnClickListener(View.OnClickListener {
                servereGonder("playerSonraki", "");
            });
            //Dialog buton atamak için
            dialogBuilder.setNegativeButton("TAMAM"){dialog,which ->
                alertDialogPlayer!!.dismiss();
            }

            alertDialogPlayer = dialogBuilder.create();
            alertDialogPlayer!!.setCancelable(false); //sadece buton ile çıkış yapabilir.
            alertDialogPlayer!!.show();

            servereGonder("sesBilgiAl", ""); //bilgisayarSesi değişkenini günceller.

        });

        // ###############  FARE / KLAVYE / SES ###############

        imgBtnFare.setOnClickListener(View.OnClickListener {
            val DEBUG_TAG:String = "FARE";
            val inflater = this.layoutInflater;
            val fragmentView = inflater.inflate(R.layout.mouse, null);
            frameLayout.addView(fragmentView);
            scrollViewMenu.visibility = View.GONE;
            frameLayout.visibility = View.VISIBLE;
            fragmentAktif = true;
            servereGonder("sesBilgiAl", "");

            frameLayout.isFocusableInTouchMode = true
            frameLayout.requestFocus()
            frameLayout.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    bilgisayarSesi = bilgisayarSesi + 1;
                    if(bilgisayarSesi > 100){ // 100 degerini aştıysa
                        bilgisayarSesi = 100;
                    }
                    servereGonder("ses", bilgisayarSesi.toString());
                    true;
                }
                else if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    bilgisayarSesi = bilgisayarSesi - 1;
                    if(0 > bilgisayarSesi){ // eksilere gittiyse
                        bilgisayarSesi = 0;
                    }
                    servereGonder("ses", bilgisayarSesi.toString());
                    true;
                }
                false
            })

            val imgFare = fragmentView.findViewById(R.id.imgFare) as ImageView
            val edtKlavye = fragmentView.findViewById(R.id.edtKlavye) as EditText
            val skBarMouseSens = fragmentView.findViewById(R.id.skBarMouseSens) as SeekBar

            //TextWatcher kullanmadık çünkü bazı android sürümlerinde backspace inputda çalışmıyor yinede önlem olarak boşluk karakterini kullanmaktayım.
            edtKlavye.setOnFocusChangeListener(object: View.OnFocusChangeListener {
                override fun onFocusChange(view:View, hasFocus:Boolean) {
                    if (hasFocus)
                    {
                        edtKlavye.setText(" ");
                        edtKlavye.setSelection(edtKlavye.text.length);
                    }
                }
            })

            edtKlavye.setSelection(edtKlavye.text.length);
            edtKlavye.setFilters(arrayOf<InputFilter>(object : InputFilter {
                var kontrolEdit = false;
                override fun filter(s: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {

                    if(kontrolEdit){
                        kontrolEdit = false;
                    }
                    else{
                        if (end == 0 || dstart < dend){
                            servereGonder("klavye", "{BACKSPACE}");
                        }
                        else{
                            servereGonder("klavye", s.toString());
                        }
                        kontrolEdit = true;
                        edtKlavye.setText(" ");
                        edtKlavye.setSelection(edtKlavye.text.length);
                    }

                    return s;
                }
            }))

            edtKlavye.setOnEditorActionListener() { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    servereGonder("klavye", "{ENTER}");
                 }
                return@setOnEditorActionListener true
            }


            var fareSens = 0.2;
            skBarMouseSens.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    fareSens = i / 100.0;
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            });

            skBarMouseSens.setProgress(20); // fare varsayılan sens
            imgFare.setOnTouchListener(object:OnTouchListener{
                var x_pressed:Int = 0;
                var y_pressed:Int = 0;

                var x_move:Int = 0;
                var y_move:Int = 0;

                var hareket_x:Int = 0;
                var hareket_y:Int = 0;

                var oneClick = true;
                val mHandlerClick = Handler();
                val mRunnableClick = Runnable {
                    oneClick = false;
                }

                var rightClick = true;
                val mHandlerRightClick = Handler();
                val mRunnableRightClick = Runnable {
                    rightClick = false;
                }

                var rightClickBekletme = false;
                val mHandlerRightClickBekletme = Handler();
                val mRunnableRightClickBekletme = Runnable {
                    rightClickBekletme = true;
                }

                override fun onTouch(v:View, event:MotionEvent):Boolean {
                    when (event.getAction()) {
                        MotionEvent.ACTION_DOWN -> {
                            x_pressed = event.x.toInt();
                            y_pressed = event.y.toInt();
                            imgFare.setImageResource(R.drawable.fareiconphover);

                            /*mHandlerClick.removeCallbacks(mRunnableClick);
                               mHandlerRightClick.removeCallbacks(mRunnableRightClick);
                               mHandlerRightClickBekletme.removeCallbacks(mRunnableRightClickBekletme);*/

                            oneClick = true;
                            mHandlerClick.postDelayed(mRunnableClick, 100)

                            //Bekletme sebebi X mili saniye olduğu yerde bekletmiş mi diye.
                            rightClickBekletme = false;
                            mHandlerRightClickBekletme.postDelayed(mRunnableRightClickBekletme, 300) // 300 mili saniyeden sonra sağ tık eventi yapabilir.

                            rightClick = true;
                            mHandlerRightClick.postDelayed(mRunnableRightClick, 1000) // 1 saniye içerisinde sağ tuş yapma hakkı. yani buda şu demek 1000 - 300 = 700 milisaniye içerisinde sağ click

                        }
                        MotionEvent.ACTION_MOVE -> {

                            x_move = event.x.toInt();
                            y_move = event.y.toInt();

                            hareket_x = (x_pressed - x_move);
                            hareket_y = (y_pressed - y_move);

                            hareket_x = (hareket_x * fareSens).toInt();
                            hareket_y = (hareket_y * fareSens).toInt();

                            if (hareket_x > 0)
                                hareket_x *= -1;
                            else
                                hareket_x *= -1;

                            if (hareket_y > 0)
                                hareket_y *= -1;
                            else
                                hareket_y *= -1;

                            servereGonder("fare", hareket_x.toString()+","+hareket_y.toString());
                        }
                        MotionEvent.ACTION_UP -> {
                            if(oneClick){
                                servereGonder("fareleftclick","");
                            }

                            if(rightClick && rightClickBekletme){
                                if(hareket_x >= 0 && hareket_x <= 3 || hareket_x >= -3 && hareket_x <= 0 && hareket_y >= 0 && hareket_y <= 3 || hareket_y >= -3 && hareket_y <= 0  ){
                                    servereGonder("farerightclick","");
                                }
                            }
                            //Bekleyen threadları yoketme.
                            mHandlerClick.removeCallbacksAndMessages(null);
                            mHandlerRightClick.removeCallbacksAndMessages(null);
                            mHandlerRightClickBekletme.removeCallbacksAndMessages(null);

                            imgFare.setImageResource(R.drawable.fareiconp);

                        }
                    }
                    return true
                }
            });


        }); //####### IMGBTNFARE END

        imgBtnShutdown.setOnClickListener(View.OnClickListener {
            val DEBUG_TAG:String = "SHTDWN";

            val dialogBuilder = AlertDialog.Builder(this@islemler);
            var alertDialogShutdown:AlertDialog? = null;

            val inflater = this.layoutInflater;
            val dialogView = inflater.inflate(R.layout.shutdown, null);
            dialogBuilder.setView(dialogView);

            val imgBtnPcShtdwn = dialogView.findViewById(R.id.imgBtnPcShtdwn) as ImageButton
            val imgBtnPcRstrt = dialogView.findViewById(R.id.imgBtnPcRstrt) as ImageButton
            val imgBtnPcSleep = dialogView.findViewById(R.id.imgBtnPcSleep) as ImageButton
            val imgBtnPcLock = dialogView.findViewById(R.id.imgBtnPcLock) as ImageButton

            //Nesnelerin eventlarının tanımlanması.
            imgBtnPcShtdwn.setOnClickListener(View.OnClickListener {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("DİKKAT!")
                alert.setMessage("Bilgisayarı Kapatmak İstediğinize Emin Misiniz?")
                alert.setCancelable(false);
                alert.setPositiveButton("Evet") { dialogInterface: DialogInterface, i: Int ->
                    servereGonder("shutdown", "kapat");
                }

                alert.setNegativeButton("Hayır") {dialogInterface: DialogInterface, i: Int ->
                }
                alert.show()
            });

            imgBtnPcRstrt.setOnClickListener(View.OnClickListener {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("DİKKAT!")
                alert.setMessage("Bilgisayarı Yeniden Başlatmak İstediğinize Emin Misiniz?")
                alert.setCancelable(false);
                alert.setPositiveButton("Evet") { dialogInterface: DialogInterface, i: Int ->
                    servereGonder("shutdown", "restart");
                }

                alert.setNegativeButton("Hayır") {dialogInterface: DialogInterface, i: Int ->
                }
                alert.show()
            });

            imgBtnPcSleep.setOnClickListener(View.OnClickListener {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("DİKKAT!")
                alert.setMessage("Bilgisayarı Uyku Moduna Almak İstediğinize Emin Misiniz?")
                alert.setCancelable(false);
                alert.setPositiveButton("Evet") { dialogInterface: DialogInterface, i: Int ->
                    servereGonder("shutdown", "uyku");
                }

                alert.setNegativeButton("Hayır") {dialogInterface: DialogInterface, i: Int ->
                }
                alert.show()
            });

            imgBtnPcLock.setOnClickListener(View.OnClickListener {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("DİKKAT!")
                alert.setMessage("Bilgisayarı Oturumu Kilitlemek İstediğinize Emin Misiniz?")
                alert.setCancelable(false);
                alert.setPositiveButton("Evet") { dialogInterface: DialogInterface, i: Int ->
                    servereGonder("shutdown", "kilit");
                }

                alert.setNegativeButton("Hayır") {dialogInterface: DialogInterface, i: Int ->
                }
                alert.show()
            });



            alertDialogShutdown = dialogBuilder.create();
            alertDialogShutdown!!.show();


        });

    }

    fun servereGonder(tag:String,deger:String){
        val DEBUG_TAG:String = "SERVERGONDER";
        thread {
            try{
                var send = tag+"="+ deger;
                val conn = Socket(ip,port.toInt());
                Log.d(DEBUG_TAG, "SEND "+send);
                conn.outputStream.write(send.toByteArray());
                conn.close();
            }
            catch (e: IOException) {
                Log.d(DEBUG_TAG, e.toString());
            }
        }
    }

    var thServerOku:Thread? = null;
    var thRunning = true;
    var socketListener:ServerSocket? = null;
    fun serveriOku(){
        val DEBUG_TAG:String = "SERVEROKU";
        thServerOku = thread{
            try{
                socketListener = ServerSocket(port.toInt());
                socketListener.use {
                    while(thRunning){
                        val socket = socketListener!!.accept();
                        //val output = PrintWriter(socket.getOutputStream(), true)
                        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val gelen = input.readLine();
                        socket.close();
                        Log.d(DEBUG_TAG, "GELEN "+ gelen);

                        if(gelen.indexOf("ses=") != -1){
                            val ses = gelen.replace("ses=", "");
                            bilgisayarSesi = ses.toInt();
                            this@islemler.runOnUiThread(Runnable {
                                if(txtSes != null && skBarSes != null){
                                    skBarSes!!.setProgress(ses.toInt());
                                }

                                if(txtPlyrSes != null && skBarPlyrSes != null){
                                    skBarPlyrSes!!.setProgress(ses.toInt());
                                }

                            });
                        }


                    }
                }

            }
            catch (e: IOException) {
                Log.d(DEBUG_TAG, "HATA: "+e.toString());
            }
        }


    }

    fun fragmentdenCik(){
        if (this.supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(); // aktif fragment sırayla geri ile siliniyor.
        }

        if(this.supportFragmentManager.backStackEntryCount == 0 || this.supportFragmentManager.backStackEntryCount == 1){
            fragmentAktif = false;
            scrollViewMenu.visibility = View.VISIBLE;
            frameLayout.visibility = View.GONE;
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        if(!fragmentAktif){
            socketiKapat();
            onBackPressed()
        }
        else{
            fragmentdenCik();
        }

        return true
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(!fragmentAktif){ // true fragment aktif | false fragment kapalı bu sayede aktivitiden çıkış yapılabilir.
                socketiKapat();
                finish();
            }
            else{
                fragmentdenCik();
                return false;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    fun socketiKapat(){
        try{
            if(socketListener != null){
                socketListener!!.close();
                thRunning = false;
                thServerOku!!.interrupt();
                thServerOku = null;
            }
        }
        catch(e: IOException)
        {
            Log.d("SOKETKPT", "HATA: "+e.toString());
        }


    }

}

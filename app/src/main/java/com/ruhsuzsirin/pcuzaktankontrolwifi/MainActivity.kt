package com.ruhsuzsirin.pcuzaktankontrolwifi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*

//context bilgisini almamız için gerekli kitap
import android.content.Context
import android.content.DialogInterface

//Wifi bağlımı diye kontrol ettiğim gerekli kitaplar
import android.net.ConnectivityManager
import android.net.NetworkInfo

//Wifi IP Adresi alabilmem için gerekli kitaplar
import android.net.wifi.WifiManager
import android.text.format.Formatter

//Arkaplan işlemi için gerekli kitap
import android.os.AsyncTask
import android.util.Log


//Bağlı olduğum networkdeki diğer aktif cihazları bulmak için gerekli kitap
import java.net.InetAddress

//Progressler için
import android.app.ProgressDialog

//Veri kontrolleri için:

//İzin için
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.widget.*

//List için
import android.widget.ArrayAdapter
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private val PermissionsRequestCode = 123
    private lateinit var managePermissions: ManagePermissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*val btnSearch = findViewById<Button>(R.id.btnSearch);
        val prgBar = findViewById<ProgressBar>(R.id.prgBar);*/
        //val btnSearch = this.btnSearch
        //val progressBar: ProgressBar = this.prgBar

        //val btnBgln = findViewById<Button>(R.id.btnBaglan);

        //İzin Kontrolleri
        val list = listOf<String>(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET
        )

        managePermissions = ManagePermissions(this,list,PermissionsRequestCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            managePermissions.checkPermissions()


        //IP TARAMA
        var nsTask = networkSearching(this, this.progBar, this.txtProgress, this.listIps, this.btnSearch,this.edtIpAdres);
        btnSearch.setOnClickListener(View.OnClickListener {
            if(btnSearch.text == "Cihaz Ara"){
                if(isOnline()){
                    //Toast.makeText(this,"Wifi Bağlı", Toast.LENGTH_LONG).show();
                    btnSearch.text = "Durdur";
                    nsTask = networkSearching(this, this.progBar, this.txtProgress, this.listIps, this.btnSearch,this.edtIpAdres);
                    nsTask.execute();

                }
                else{
                    Toast.makeText(this,"Lütfen Wifi'ye Bağlanın.", Toast.LENGTH_LONG).show();
                }
            }
            else{
                btnSearch.text = "Cihaz Ara";
                nsTask.cancel(true);

            }



        });


        var btnBaglanBeklet = false;
        btnBaglan.setOnClickListener(View.OnClickListener {
            val DEBUG_TAG:String = "CONN";
            if(!btnBaglanBeklet){
                btnBaglanBeklet = true;

                if(isOnline()){
                    if(edtIpAdres.text.toString() != "" && edtPort.text.toString() != ""){
                        thread {
                            try{

                                val wm = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                val ip = Formatter.formatIpAddress(wm.connectionInfo.ipAddress); //Bağlandığı wifi ip.

                                val sockAdr = InetSocketAddress(edtIpAdres.text.toString(), edtPort.text.toString().toInt());
                                val conn = Socket();
                                conn.connect(sockAdr, 500); // 500 milisaniye içerisinde bağlanmazsa exception atacak.

                                Log.d(DEBUG_TAG, "Connected"+ edtIpAdres.text.toString() + ":"+edtPort.text.toString().toInt());
                                var send = "clientip="+ ip.toString();
                                conn.outputStream.write(send.toByteArray());
                                conn.close();

                                this@MainActivity.runOnUiThread(java.lang.Runnable {
                                    Toast.makeText(this,"Bağlandı: IP:"+edtIpAdres.text.toString(), Toast.LENGTH_SHORT).show();
                                })

                                val intent = Intent(this, islemler::class.java)
                                intent.putExtra("ip", edtIpAdres.text.toString());
                                intent.putExtra("port", edtPort.text.toString());
                                intent.putExtra("clientip", ip);
                                startActivity(intent)
                                btnBaglanBeklet = false;
                            }
                            catch (e: IOException) {
                                Log.d(DEBUG_TAG, e.toString());
                                this@MainActivity.runOnUiThread(java.lang.Runnable {
                                    Toast.makeText(this, "IP: "+edtIpAdres.text.toString()+" Bağlanamadı!", Toast.LENGTH_SHORT).show();
                                })
                                btnBaglanBeklet = false;
                                /*this.runOnUiThread( Runnable() { //thread içinde toastın çalışması için runOnUiThread kullanmalıyız.
                                    Toast.makeText(this, "IP: "+edtIpAdres.text.toString()+" Bağlanamadı!", Toast.LENGTH_SHORT).show();
                                });*/
                            }
                        }

                    }
                    else{
                        Toast.makeText(this, "Lütfen Ip adresini ve portu giriniz!", Toast.LENGTH_SHORT).show();
                        btnBaglanBeklet = false;
                    }
                }
                else{
                    Toast.makeText(this,"Lütfen Wifi'ye Bağlanın.", Toast.LENGTH_LONG).show();
                    btnBaglanBeklet = false;
                }

            }
            else{
                Toast.makeText(this,"Bağlanıyor Bekleyin...", Toast.LENGTH_LONG).show();
            }



        });
    }

    fun isOnline(): Boolean {
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    class networkSearching(context: Context, progBar:ProgressBar, txtProgress:TextView,listIps:ListView, btnSearch:Button, edtIpAdres:EditText) : AsyncTask<String, Int, String>() {
        val mContext = context;
        val prgBarActivity = progBar;
        val txtPrgs = txtProgress;
        val listViewIps = listIps;
        val btnSrch = btnSearch;
        val edtIpAddres = edtIpAdres;

        val ipList = mutableListOf<String>();
        //var ipList = listOf<String>();
        val adapter = ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, ipList);

        val DEBUG_TAG:String = "NWSRH";
        lateinit var progressBar: ProgressDialog

        override fun onPreExecute() {
            super.onPreExecute();
            progressBar = ProgressDialog(mContext)
            progressBar.setMax(255);
            progressBar.setProgress(0);
            progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

            progressBar.setTitle("Lütfen Bekleyin...")
            progressBar.setMessage("Aktif Cihazlarınız aranıyor...");
            progressBar.setCancelable(false);
            progressBar.setButton(DialogInterface.BUTTON_NEGATIVE, "ARKA PLAN", DialogInterface.OnClickListener { progressBar, which ->
                //this.cancel(true); progress durdurmak için gerekirse
                progressBar.dismiss();
            });

            progressBar.show();

            //Ayrı ProgressBar için max
            prgBarActivity.max = 255;

            //LİSTVİEW
            listViewIps.adapter = adapter;
            listViewIps.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->

                val secilen = parent.getItemAtPosition(position) as String
                edtIpAddres.setText(secilen);
                //Toast.makeText(mContext, "Seçilen : $secilen", Toast.LENGTH_SHORT).show()
            }


        }

        override fun doInBackground(vararg str: String): String? {

            val wm = mContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = Formatter.formatIpAddress(wm.connectionInfo.ipAddress); // Telefonun bağlandığı ip verir.
            //Log.d(DEBUG_TAG, "IP ADRESİ: " + ip);
            val prefix = ip.substring(0, ip.lastIndexOf(".") + 1)
            //Log.d(DEBUG_TAG, "IP PREFIX: " + prefix);

            for (i in 0..255) {

                val testIp = prefix + i.toString();
                //Log.d(DEBUG_TAG, "TEST IP: "+testIp);
                val address = InetAddress.getByName(testIp);
                val reachable = address.isReachable(70);
                //val hostName = address.canonicalHostName;

                if (reachable) {
                   if(address.hostAddress != ip)
                        ipList += address.hostAddress;

                    //ipList += "Adı: " + address.canonicalHostName + " IP: " + address.hostAddress + "";
                    //Log.d(DEBUG_TAG,"Host: " + address.canonicalHostName + "(" + testIp + ") is reachable!");
                }
                publishProgress(i); //((i+1) / 254) * 100

                if(isCancelled()){
                    break;
                }
            }

            return "İŞLEM BİTTİ";
        }


        override fun onProgressUpdate(vararg progress: Int?) {
            //Log.d(DEBUG_TAG, "Progress: "+progress[0]);
            val progress = progress[0];
            if (progress != null) {
                progressBar.setProgress(progress)
                prgBarActivity.setProgress(progress);
                txtPrgs.text = progress.toString() + " / 255";

                adapter.notifyDataSetChanged(); //Adapter Günceller listview güncellenmiş olur.
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            progressBar.dismiss();
            btnSrch .text = "Cihaz Ara"; // arama tamamlandığında isim değiştirme.
            Toast.makeText(mContext,"ARAMA TAMAMLANDI", Toast.LENGTH_SHORT).show();
        }

        override fun onCancelled() {
            super.onCancelled()
            progressBar.setProgress(0);
            prgBarActivity.setProgress(0);
            txtPrgs.text = "0 / 255";
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PermissionsRequestCode ->{
                val isPermissionsGranted = managePermissions
                    .processPermissionsResult(requestCode,permissions,grantResults)

                if(isPermissionsGranted){
                    // Do the task now
                    //toast("Permissions granted.")
                }else{
                    //toast("Permissions denied.")
                }
                return
            }
        }
    }


}

/*// Extension function to show toast message // izin için kullanmak gerekirse.
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}*/


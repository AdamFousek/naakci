package cz.naakci.app.naakciprojekt.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;


import cz.naakci.app.naakciprojekt.AESCrypt;
import cz.naakci.app.naakciprojekt.CustomCameraSettings;
import cz.naakci.app.naakciprojekt.R;
import cz.naakci.app.naakciprojekt.models.ApiClient;
import cz.naakci.app.naakciprojekt.models.Code;
import cz.naakci.app.naakciprojekt.models.Tickets;
import cz.naakci.app.naakciprojekt.models.Event;
import cz.naakci.app.naakciprojekt.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class BarcodeReaderActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    // Retrofit
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Retrofit.Builder builder = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://www.tickito.cz/")
            .addConverterFactory(GsonConverterFactory.create(gson));
    private final Retrofit retrofit = builder.build();

    // ScannerView - pracuje s kamerou
    private ZXingScannerView scannerView;
    private String codeString = "";
    private Code code = new Code();
    private Tickets tickets;
    private ArrayList<String> usedTickets= new ArrayList<>();
    private Event event;
    private User user;

    // SharedPreferences
    private SharedPreferences mySharedPref;
    private SharedPreferences.Editor mySharedEditor;

    private boolean activeBR = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        event = (Event)intent.getSerializableExtra("Event");
        user = (User)intent.getSerializableExtra("User");

        // Nastavení layoutu pro kameru
        scannerView = new ZXingScannerView(this) {

            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new CustomCameraSettings(context);
            }

        };
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);
        }

        setContentView(scannerView);
        scannerView.setResultHandler(this);
        scannerView.startCamera();
        activeBR = true;
        checkConnection();

        syncCodes();
    }

    @Override
    public void onStart() {
        super.onStart();
        activeBR = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        activeBR = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeBR = false;
        scannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeBR = true;
        scannerView.startCamera();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        activeBR = false;
    }

    @Override
    public void handleResult(Result result) {
        final String myResult = result.getText();
        if(isInternet()){
            CheckCodeTask mAuthTask = new CheckCodeTask(myResult);
            mAuthTask.execute();
        } else {
            if(tickets != null) {
                if (tickets.getCodes().containsKey(myResult)) {
                    Code code = tickets.getCodes().get(myResult);
                    AlertDialog.Builder builder;
                    if (code.getUsed() == null) {
                        builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.SuccessDialogTheme);
                        builder.setTitle("Kód přijat");

                        if(code.getLine_second() != null)
                            builder.setMessage(code.getLine_first() + "\n" + code.getLine_second());
                        else
                            builder.setMessage(code.getLine_first());

                        code.setUsed(new Date());
                        tickets.getCodes().put(myResult, code);
                        usedTickets.add(myResult);
                    } else {
                        builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);
                        builder.setTitle("Kód nebyl přijat");
                        String message = code.getLine_first();
                        if(code.getLine_second() != null)
                            message += "\n" + code.getLine_second();
                        builder.setMessage(message+"\n\nKód již byl použit " + DateFormat.format("HH:mm:ss dd.MM.yyyy", code.getUsed()));
                    }
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                        }
                    });

                    AlertDialog alertCode = builder.create();
                    alertCode.setCancelable(false);
                    alertCode.setCanceledOnTouchOutside(false);
                    alertCode.show();
                } else {
                    // Když kód k dané akci neexistuje
                    AlertDialog.Builder builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);

                    builder.setTitle("Kód nebyl přijat");
                    builder.setMessage("Kód pro danou akci neexistuje");

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                        }
                    });

                    AlertDialog alertCode = builder.create();
                    alertCode.setCancelable(false);
                    alertCode.setCanceledOnTouchOutside(false);
                    alertCode.show();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);

                builder.setTitle("Vstupenky nebyly staženy");
                builder.setMessage("Prosím, obnovte internetové spojení pro stažení lístků!");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                    }
                });

                AlertDialog alertCode = builder.create();
                alertCode.setCancelable(false);
                alertCode.setCanceledOnTouchOutside(false);
                alertCode.show();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Menu pro rozsvicení světla a zadání kódu ručně
        getMenuInflater().inflate(cz.naakci.app.naakciprojekt.R.menu.barcode_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == cz.naakci.app.naakciprojekt.R.id.flashlight){
            // Rozsvícení světla
            try{
                scannerView.stopCameraPreview();
                scannerView.toggleFlash();
                if(scannerView.getFlash()){
                    item.setIcon(ResourcesCompat.getDrawable(getResources(), cz.naakci.app.naakciprojekt.R.drawable.flashlight_on, null));
                } else {
                    item.setIcon(ResourcesCompat.getDrawable(getResources(), cz.naakci.app.naakciprojekt.R.drawable.flashlight, null));
                }
                scannerView.resumeCameraPreview(this);
            }catch(Exception e){
                Toast.makeText(getApplicationContext(), "Světlo nelze zapnout", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if(id == cz.naakci.app.naakciprojekt.R.id.manulaScan){
            // Zobrazí se Dialog na zadání kódu ručně
            scannerView.stopCameraPreview();
            final EditText input = new EditText(this);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Zadejte kód");

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    codeString = input.getText().toString();
                    CheckCodeTask mAuthTask = new CheckCodeTask(codeString);
                    mAuthTask.execute((Void) null);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                }
            });

            builder.show();
        }
        return super.onOptionsItemSelected(item);

    }

    private void syncCodes() {
        final Handler handler = new Handler();
        final Timer timer = new Timer();


        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if(!activeBR){
                            timer.cancel();
                            timer.purge();
                        }
                        if (isInternet()) {
                            TicketsSyncTask ticketsSync = new TicketsSyncTask();
                            ticketsSync.execute();
                        } else {
                            Toast.makeText(getApplicationContext(), "Zkontrolujte připojení k internetu", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };

        timer.schedule(task, 0, 60*1000*2);  // interval of one minute

        if(!activeBR){
            timer.cancel();
        }
    }

    /**
     * Kontrola připojení.
     */
    private void checkConnection() {
        final Handler handler = new Handler();
        final Timer timer = new Timer();


        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if(!activeBR){
                            timer.cancel();
                            timer.purge();
                        }
                        if (isInternet()) {

                        } else {
                            Toast.makeText(getApplicationContext(), "Zkontrolujte připojení k internetu", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };

        timer.schedule(task, 0, 5000);  // interval of one minute

        if(!activeBR){
            timer.cancel();
        }
    }

    /**
     * Je k dispozici internet
     * @return boolean
     */
    private boolean isInternet(){
        ConnectivityManager cn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nf = cn.getActiveNetworkInfo();
        return nf != null && nf.isConnected() == true;
    }

    /**
     * Classa na AsyncTask - kontrola lístků
     */
    public class CheckCodeTask extends AsyncTask<Void, Void, Boolean> {

        private final String name;
        private final String password;
        private final String codeS;
        private ProgressDialog progDialog;
        private int responseCode;

        CheckCodeTask(String code){
            mySharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
            this.name = mySharedPref.getString("name", "");
            String pass = mySharedPref.getString("password", "");
            try {
                pass = AESCrypt.decrypt(pass);
            } catch (Exception e){
                e.printStackTrace();
            }
            this.password = pass;
            this.codeS=code;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            scannerView.stopCameraPreview();
            progDialog = new ProgressDialog(BarcodeReaderActivity.this);
            progDialog.setMessage("Načítání...");
            progDialog.setIndeterminate(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDialog.setCancelable(true);
            progDialog.show();
        }

        // Získání údajů z API
        @Override
        protected Boolean doInBackground(Void... voids) {
            ApiClient userClient = retrofit.create(ApiClient.class);

            String base = name + ":" + password;

            String authHeader = "Basic " + Base64.encodeToString(base.getBytes(), Base64.NO_WRAP);
            Call<Code> call = userClient.checkCode(authHeader, event.getId(), codeS);

            try {
                Response<Code> response = call.execute();
                responseCode = response.code();
                if(response.isSuccessful()){
                    code = response.body();
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success){
            progDialog.dismiss();
            if (responseCode == 200) {
                // Vypsání kódu a jestli byl použitý nebo ne
                AlertDialog.Builder builder;
                if (code.getUsed() == null) {
                    builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.SuccessDialogTheme);
                    builder.setTitle("Kód přijat");
                    if(code.getLine_second() != null)
                        builder.setMessage(code.getLine_first() + "\n" + code.getLine_second());
                    else
                        builder.setMessage(code.getLine_first());
                    Code tmp = tickets.getCodes().get(code.getCode());
                    tmp.setUsed(new Date());
                    tickets.getCodes().put(code.getCode(), tmp);
                } else {
                    builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);
                    builder.setTitle("Kód nebyl přijat");
                    String message = code.getLine_first();
                    if(code.getLine_second() != null)
                        message += "\n" + code.getLine_second();
                    builder.setMessage(message+"\n\nKód již byl použit " + DateFormat.format("HH:mm:ss dd.MM.yyyy", code.getUsed()));
                }
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                    }
                });

                AlertDialog alertCode = builder.create();
                alertCode.setCancelable(false);
                alertCode.setCanceledOnTouchOutside(false);
                alertCode.show();

            } else if (responseCode == 404) {
                // Když kód k dané akci neexistuje
                AlertDialog.Builder builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);

                builder.setTitle("Kód nebyl přijat");
                builder.setMessage("Kód pro danou akci neexistuje");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                    }
                });

                AlertDialog alertCode = builder.create();
                alertCode.setCancelable(false);
                alertCode.setCanceledOnTouchOutside(false);
                alertCode.show();
            } else if (responseCode == 500) {
                // Když aplikace hodí 500 na webu
                AlertDialog.Builder builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);

                builder.setTitle("Problém na straně serveru");
                builder.setMessage("Momentálně nefunguje ověření kódů");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                    }
                });

                AlertDialog alertCode = builder.create();
                alertCode.setCancelable(false);
                alertCode.setCanceledOnTouchOutside(false);
                alertCode.show();
            }else{
                // Když se něco dojebe v apce - timeout nebo tak
                AlertDialog.Builder builder = new AlertDialog.Builder(BarcodeReaderActivity.this, cz.naakci.app.naakciprojekt.R.style.FailDialogTheme);

                builder.setTitle("Timeout - ověření selhalo");
                builder.setMessage("Načítání kódu se nezdařilo, prosím načtěte znova");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scannerView.resumeCameraPreview(BarcodeReaderActivity.this);
                    }
                });

                AlertDialog alertCode = builder.create();
                alertCode.setCancelable(false);
                alertCode.setCanceledOnTouchOutside(false);
                alertCode.show();
            }
        }

        @Override
        protected void  onCancelled() {
        }

    }

    /**
     * Classa na AsyncTask - Code
     */
    public class TicketsSyncTask extends AsyncTask<Void, Void, Boolean> {

        private final String name;
        private final String password;

        TicketsSyncTask(){
            this.password = user.getPassword();
            this.name = user.getName();
        }

        // Získání údajů z API
        @Override
        protected Boolean doInBackground(Void... voids) {
            ApiClient userClient = retrofit.create(ApiClient.class);

            String base = name + ":" + password;

            String authHeader = "Basic " + Base64.encodeToString(base.getBytes(), Base64.NO_WRAP);

            for(String s : usedTickets){
                Call<Code> call = userClient.checkCode(authHeader, event.getId(), s);

                try {
                    Response<Code> response = call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            usedTickets.clear();

            Call<Tickets> call = userClient.getCodes(authHeader, event.getId());

            try {
                Response<Tickets> response = call.execute();
                if(response.isSuccessful()){
                    tickets = response.body();
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success){
            if(success){
                // Uložení proběhlo v pořádku
            }else{
                // Nějaká chyba
            }

        }

        @Override
        protected void  onCancelled() {
        }

    }


}

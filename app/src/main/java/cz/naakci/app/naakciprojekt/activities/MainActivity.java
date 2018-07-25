package cz.naakci.app.naakciprojekt.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import cz.naakci.app.naakciprojekt.AESCrypt;
import cz.naakci.app.naakciprojekt.R;
import cz.naakci.app.naakciprojekt.models.User;
import cz.naakci.app.naakciprojekt.models.ApiClient;

import java.util.Timer;
import java.util.TimerTask;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {

    // Komponenty layoutu
    private Button buttonLogin;
    private EditText loginText;
    private EditText passwordText;
    private TextView wrong;

    // Informace o uživateli
    private User user = new User();

    // SharedPreferences uložení dat o uživateli
    private SharedPreferences mySharedPref;
    private SharedPreferences.Editor mySharedEditor;

    // Retrofit knihovna na api
    private final Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl("https://www.tickito.cz/")
            .addConverterFactory(GsonConverterFactory.create());
    private final Retrofit retrofit = builder.build();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        componentsSettings();

        checkLogin();

        initListeners();

        checkConnection();
    }

    private void componentsSettings(){
        wrong = (TextView) findViewById(R.id.wrongLogin);
        loginText = (EditText) findViewById(R.id.editTextLogin);
        passwordText = (EditText) findViewById(R.id.editTextPassword);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
    }

    private void checkLogin(){
        // SharedPreference - po prvním přihlášení se nastaví cache na 1 den - kontrola jestli vyprachala
        mySharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        String name = mySharedPref.getString("name", "");
        String password = mySharedPref.getString("password", "");
        long timestamp = mySharedPref.getLong("timestamp", 1);
        long currentTimestamp = System.currentTimeMillis() / 1000L;
        try {
            password = AESCrypt.decrypt(password);
        } catch (Exception e){
            e.printStackTrace();
        }
        if(timestamp > currentTimestamp){
            if(!name.isEmpty() && !password.isEmpty()) {
                UserLoginTask mAuthTask = new UserLoginTask(name, password);
                mAuthTask.execute((Void) null);
            }
        }
    }

    private void initListeners() {
        loginText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        passwordText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        buttonLogin.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:{
                        // Effekt tlačítka :)
                        buttonLogin.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.button_selector_pressed, null));
                        wrong.setText("");
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonLogin.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.button_selector, null));
                        // Kontrola jestli uživatel vyplnil text pokud ano, udělá se AsyncTask
                        if(loginText.getText().toString().isEmpty() || passwordText.getText().toString().isEmpty()){
                            wrong.setText("Vyplňte prosím pole");
                        }else{
                            UserLoginTask mAuthTask = new UserLoginTask(loginText.getText().toString(), passwordText.getText().toString());
                            mAuthTask.execute((Void) null);
                        }
                        break;
                    }
                }

                return true;
            }
        });
    }

    /**
     * Schování klávesnice když kliknu mimo text componenty
     * @param view
     */
    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(MainActivity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // Kontrola připojení
    private void checkConnection() {
        final Handler handler = new Handler();
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
            handler.post(new Runnable() {
                public void run() {
                ConnectivityManager cn=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo nf=cn.getActiveNetworkInfo();
                if(nf != null && nf.isConnected()==true )
                {
                    wrong.setText("");
                }
                else
                {
                    wrong.setText("Zkontrolujte prosím připojení k internetu");
                }
                }
            });
            }
        };

        timer.schedule(task, 0, 5000);  // Co 5 sekund
    }

    /**
     * Classa na AsyncTask - kontrola údajů api
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String name;
        private final String password;
        private Boolean logedIn = false;

        UserLoginTask(String name, String password){
            this.name = name;
            this.password = password;
        }

        // Získání údajů z API
        @Override
        protected Boolean doInBackground(Void... voids) {
            ApiClient userClient = retrofit.create(ApiClient.class);

            String base = name + ":" + password;

            String authHeader = "Basic " + Base64.encodeToString(base.getBytes(), Base64.NO_WRAP);
            Call<User> call = userClient.getUser(authHeader);

            try {
                Response<User> response = call.execute();
                if(response.isSuccessful()){
                    user = response.body();
                    user.setName(name);
                    user.setPassword(password);
                    // Ukládání údajů do sharedPreferences - na 1 den
                    mySharedEditor = mySharedPref.edit();
                    mySharedEditor.putString("name", name);
                    mySharedEditor.putString("password", AESCrypt.encrypt(password));
                    mySharedEditor.putLong("timestamp", (System.currentTimeMillis() / 1000L)+(3*60*60));
                    mySharedEditor.apply();
                    logedIn = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return logedIn;
        }

        @Override
        protected void onPostExecute(final Boolean success){

            if(success){
                // Uživateli zobrazí Úspěch přepne do nové aktivity s User objektem a aktuální aktivitu ukončí
                Toast.makeText(getApplicationContext(), "Přihlášení úspěšné", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, ListOfEventsActivity.class);
                intent.putExtra("User", user);
                startActivity(intent);
                finish();
            }else{
                // Špatné jméno nebo heslo oznámí uživateli v TextView
                wrong.setText("Špatné jméno nebo heslo");
            }

        }

        @Override
        protected void  onCancelled() {
        }

    }
}
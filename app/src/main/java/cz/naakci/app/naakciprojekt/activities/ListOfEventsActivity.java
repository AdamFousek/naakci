package cz.naakci.app.naakciprojekt.activities;

import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import cz.naakci.app.naakciprojekt.R;
import cz.naakci.app.naakciprojekt.models.ApiClient;
import cz.naakci.app.naakciprojekt.models.Event;
import cz.naakci.app.naakciprojekt.EventAdapter;
import cz.naakci.app.naakciprojekt.models.User;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ListOfEventsActivity extends AppCompatActivity {

    // Retrofit
    private final Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl("https://www.tickito.cz/")
            .addConverterFactory(GsonConverterFactory.create());
    private final Retrofit retrofit = builder.build();

    // SharedPreferences
    private SharedPreferences mySharedPref;
    private SharedPreferences.Editor mySharedEditor;

    // Informace o uživateli
    private User user = new User();
    private ListView lv;
    private EventAdapter adapter;

    private boolean activeLoE = true;

    @Override
    public void onStart() {
        super.onStart();
        activeLoE = true;
        displayData();
    }

    @Override
    public void onPause() {
        super.onPause();
        activeLoE = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        activeLoE = true;
        displayData();
    }

    @Override
    public void onStop() {
        super.onStop();
        activeLoE = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(cz.naakci.app.naakciprojekt.R.layout.activity_list_of_events);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Získání informací o uživateli
        Intent intent = getIntent();
        user = (User)intent.getSerializableExtra("User");
        activeLoE = true;

        adapter = new EventAdapter(ListOfEventsActivity.this, cz.naakci.app.naakciprojekt.R.layout.list_event_layout, user.getEvents());
        lv = (ListView)findViewById(cz.naakci.app.naakciprojekt.R.id.listView1);
        lv.setAdapter(adapter);

    }

    // Přídání menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(cz.naakci.app.naakciprojekt.R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == cz.naakci.app.naakciprojekt.R.id.logout){
            // Při odhlášení smažeme SharedPreferences aby se uživatel znovu nepřihlásil
            mySharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
            mySharedEditor = mySharedPref.edit();
            mySharedEditor.putString("name", "");
            mySharedEditor.putString("password", "");
            mySharedEditor.putLong("timestamp", (System.currentTimeMillis() / 1000L));
            mySharedEditor.apply();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // Kontrola připojení
    private void displayData() {
        final Handler handler = new Handler();
        final Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if(!activeLoE){
                            timer.cancel();
                            timer.purge();
                        }
                        ConnectivityManager cn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo nf = cn.getActiveNetworkInfo();
                        if (nf != null && nf.isConnected() == true) {
                            ListViewTask checkEvents = new ListViewTask(user.getName(), user.getPassword());
                            checkEvents.execute();
                        } else {
                            if (activeLoE) {
                                Toast.makeText(getApplicationContext(), "Zkontrolujte připojení k internetu", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        };
        timer.schedule(task, 0, 5000);  // interval of one minute
    }

    /**
     * Classa na AsyncTask - kontrola eventů a kodů
     */
    public class ListViewTask extends AsyncTask<Void, Void, Boolean> {

        private final String name;
        private final String password;
        private Boolean logedIn = false;

        ListViewTask(String name, String password){
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
                    user.setEvents(response.body().getEvents());
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
                // Vyplnění layoutu akcema
                if(lv.getAdapter()==null)
                    lv.setAdapter(adapter);
                else{
                    adapter.clear();
                    adapter.addAll(user.getEvents());
                }
                adapter.notifyDataSetChanged();
                findViewById(R.id.noEventsListed).setVisibility(
                        adapter.getDataSize() == 0 ? View.VISIBLE : View.GONE
                );
                lv.setOnItemClickListener(new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Event event = (Event) adapterView.getItemAtPosition(i);

                        Intent intent = new Intent(view.getContext(), BarcodeReaderActivity.class);
                        intent.putExtra("Event", event);
                        intent.putExtra("User", user);
                        startActivity(intent);
                    }
                });
            }else{
                // Something wrong
            }

        }

        @Override
        protected void  onCancelled() {
        }

    }
}

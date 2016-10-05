package com.ravendmaster.onecore.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ravendmaster.onecore.service.AppSettings;
import com.ravendmaster.onecore.R;

public class ConnectionSettingsActivity extends AppCompatActivity {

    EditText username;
    EditText password;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean validateUrl(String adress) {
        if(adress.endsWith(".xyz"))return true;
        return Patterns.DOMAIN_NAME.matcher(adress).matches();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.close:

                AppSettings settings = AppSettings.getInstance();
                settings.username = username.getText().toString();
                settings.password = password.getText().toString();

                settings.saveConnectionSettingsToPrefs(this);

                if(MainActivity.presenter!=null) {
                    MainActivity.presenter.connectionSettingsChanged();
                }

                finish();
                MainActivity.presenter.resetCurrentSessionTopicList();

                MainActivity.presenter.subscribeToAllTopicsInDashboards(settings);
                break;
        }
        return true;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        username = (EditText) findViewById(R.id.editText_username);
        password = (EditText) findViewById(R.id.editText_password);

        AppSettings settings = AppSettings.getInstance();
        username.setText(settings.username);
        password.setText(settings.password);
    }

    public void OnClickHelp(View view){
        MainActivity.presenter.OnClickHelp(this, view);
    }

}

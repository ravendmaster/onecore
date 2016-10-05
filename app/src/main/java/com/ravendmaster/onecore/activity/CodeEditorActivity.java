package com.ravendmaster.onecore.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.ravendmaster.onecore.R;

public class CodeEditorActivity extends AppCompatActivity {

    EditText program;
    String mac;

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.presenter.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.presenter.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_editor);


        Intent intent = getIntent();
        program = (EditText) findViewById(R.id.editText_codeEditor);
        program.setText(intent.getStringExtra("program"));
        mac=intent.getStringExtra("mac");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_options, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.public_prog:
                MainActivity.presenter.publicProgram(mac, program.getText().toString());
                break;
            case R.id.close:
                Intent intent = new Intent();
                intent.putExtra("program", program.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
                break;
        }
        return true;
    }

}
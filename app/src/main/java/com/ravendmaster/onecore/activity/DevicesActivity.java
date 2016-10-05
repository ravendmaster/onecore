package com.ravendmaster.onecore.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ravendmaster.onecore.devices.Device;
import com.ravendmaster.onecore.R;
import com.ravendmaster.onecore.DevicesListFragment;
import com.ravendmaster.onecore.service.AppSettings;

public class DevicesActivity extends AppCompatActivity {

    DevicesListFragment mDevicesListFragment;

    public static DevicesActivity instance;

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.presenter.onResume(this);
    }

    public void showPopupMenuTabEditButtonOnClick(View view) {

        final Device device = (Device) view.getTag();

        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_device, popup.getMenu());

        currentDevice = device;

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.device_edit:
                        showDeviceEditDialog();
                        return true;
                    case R.id.device_program_code:
                        showCodeEditorDialog();
                        return true;
                    case R.id.device_remove:
                        showDeviceRemoveDialog();
                        return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.devices_container, fragment, "fragment").commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;

        setContentView(R.layout.activity_devices);

        mDevicesListFragment = DevicesListFragment.newInstance();
        showFragment(mDevicesListFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    void showCodeEditorDialog(){
        Intent intent = new Intent(DevicesActivity.this, CodeEditorActivity.class);
        intent.putExtra("mac", currentDevice.mac);
        intent.putExtra("program", currentDevice.program);
        startActivityForResult(intent, 0);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {return;}

        if(requestCode==0) {//program editor
            currentDevice.program = data.getStringExtra("program");
        }


    }

    Device currentDevice;
    void showDeviceRemoveDialog(){
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle("Remove device");  // заголовок
        //ad.setMessage("A set of widgets on the panel will be lost. Continue?"); // сообщение
        ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                final AppSettings appSettings = AppSettings.getInstance();
                appSettings.removeDeviceById(currentDevice.id);
                mDevicesListFragment.notifyDataSetChanged();
            }
        });
        ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
            }
        });
        ad.show();
    }

    void showDeviceEditDialog() {

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.device_edit, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        final EditText nameView = (EditText) promptsView.findViewById(R.id.editText_name);
        final EditText macView = (EditText) promptsView.findViewById(R.id.editText_mac);
        if (currentDevice != null) {
            nameView.setText(currentDevice.name);
            macView.setText(currentDevice.mac);
        }
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String newMac = macView.getText().toString();
                                String newName = nameView.getText().toString();
                                if (currentDevice == null) {
                                    MainActivity.presenter.addNewDevice(newMac, newName);
                                } else {
                                    currentDevice.mac = newMac;
                                    currentDevice.name = newName;
                                }
                                mDevicesListFragment.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.add_new_device:
                currentDevice = null;
                showDeviceEditDialog();
                break;
            case R.id.save:

                finish();
                break;

        }
        return true;

    }

    @Override
    protected void onPause() {
        MainActivity.presenter.saveDevicesList(this);
        MainActivity.presenter.onPause();
        super.onPause();
    }
}

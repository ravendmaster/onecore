package com.ravendmaster.onecore;

import android.util.JsonReader;

import com.ravendmaster.onecore.devices.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.util.ArrayList;

public class DevicesCollection {
    ArrayList<Device> items = new ArrayList<>();

    public ArrayList<Device> getItems() {
        return items;
    }

    public String getDeviceMacByID(int id) {

        for (Device device : items) {
            if (device.id == id) {
                return device.mac;
            }
        }
        return null;
    }

    public String getDeviceNameByID(int id) {

        for (Device device : items) {
            if (device.id == id) {
                return device.name;
            }
        }
        return null;
    }

    public int getFreeId() {
        int maxId = 0;
        for (Device device : items) {
            if (device.id > maxId) {
                maxId = device.id;
            }
        }
        return maxId + 1;
    }

    public void removeById(int id) {
        for (Device device :
                items) {
            if (device.id == id) {
                items.remove(device);
                return;
            }
        }
    }

    public void setFromJSONString(String tabsJSON) {
        items.clear();
        JsonReader jsonReader = new JsonReader(new StringReader(tabsJSON));
        try {
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                Device tabData = new Device();
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    switch (name) {
                        case "id":
                            tabData.id = jsonReader.nextInt();
                            break;
                        case "mac":
                            tabData.mac = jsonReader.nextString();
                            break;
                        case "name":
                            tabData.name = jsonReader.nextString();
                            break;
                        case "program":
                            tabData.program = jsonReader.nextString();
                            break;
                    }
                }
                jsonReader.endObject();
                items.add(tabData);
            }
            jsonReader.endArray();
        } catch (Exception e) {
            android.util.Log.d("error", e.toString());
        }

    }


    public String getAsJSONString() {
        JSONArray ar = new JSONArray();
        for (Device tab : items) {
            JSONObject resultJson = new JSONObject();
            try {
                resultJson.put("id", tab.id);
                resultJson.put("mac", tab.mac);
                resultJson.put("name", tab.name);
                resultJson.put("program", tab.program);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ar.put(resultJson);
        }
        return ar.toString();
    }

}

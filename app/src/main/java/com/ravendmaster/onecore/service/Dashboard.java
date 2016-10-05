package com.ravendmaster.onecore.service;

import android.content.Context;
import android.util.JsonReader;

import com.ravendmaster.onecore.Utilites;
import com.ravendmaster.onecore.customview.Graph;
import com.ravendmaster.onecore.customview.MyColors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class Dashboard {

    private ArrayList<WidgetData> mWidgets;

    public int id;

    public Dashboard(int id) {
        //Log.d(getClass().getName(), "Dashboard()");
        mWidgets = new ArrayList<>();
        this.id=id;
        //this.friendlyName=friendlyName;
    }

    public WidgetData findWidgetByTopic(String topic){
        for(WidgetData widgetData: getWidgetsList()){
            if(widgetData.getTopic(0).equals(topic))return widgetData;
        }
        return null;
    }

    public void clear() {
        mWidgets.clear();
    }

    public ArrayList<WidgetData> getWidgetsList() {
        return mWidgets;
    }

    void saveDashboard(Context context) {

        //Log.d(getClass().getName(), "saveDashboard " + id);

        JSONArray ar = new JSONArray();
        for (WidgetData widget : mWidgets) {
            JSONObject resultJson = new JSONObject();
            try {
                resultJson.put("type", widget.type);
                resultJson.put("name", widget.getName(0));
                resultJson.put("name1", widget.getName(1));
                resultJson.put("name2", widget.getName(2));
                resultJson.put("name3", widget.getName(3));


                resultJson.put("publish", widget.publishTopic_);
                resultJson.put("subscribe", widget.subscribeTopic_);

                resultJson.put("topic", widget.getTopic(0));
                resultJson.put("topic1", widget.getTopic(1));
                resultJson.put("topic2", widget.getTopic(2));
                resultJson.put("topic3", widget.getTopic(3));

                resultJson.put("publishValue", widget.publishValue);
                resultJson.put("publishValue2", widget.publishValue2);

                resultJson.put("primaryColor", widget.getPrimaryColor(0));
                resultJson.put("primaryColor1", widget.getPrimaryColor(1));
                resultJson.put("primaryColor2", widget.getPrimaryColor(2));
                resultJson.put("primaryColor3", widget.getPrimaryColor(3));

                resultJson.put("feedback", widget.feedback); //устарел

                if (widget.label != null) {
                    resultJson.put("label", widget.label);
                }

                if (widget.label2 != null) {
                    resultJson.put("label2", widget.label2);
                }

                resultJson.put("newValueTopic", widget.newValueTopic);

                resultJson.put("retained", widget.retained);

                resultJson.put("additionalValue", widget.additionalValue);
                resultJson.put("additionalValue2", widget.additionalValue2);

                resultJson.put("additionalValue3", widget.additionalValue3);

                resultJson.put("decimalMode", widget.decimalMode);

                resultJson.put("mode", widget.mode);
                resultJson.put("onShowExecute", widget.onShowExecute);
                resultJson.put("onReceiveExecute", widget.onReceiveExecute);

                resultJson.put("formatMode", widget.formatMode);

                resultJson.put("uid", widget.uid);
                resultJson.put("deviceId", widget.deviceId);

                //Log.d("uid write", widget.getName(0)+" "+widget.uid.toString());


            } catch (JSONException e) {
                e.printStackTrace();
            }

            ar.put(resultJson);
        }

        AppSettings settings = AppSettings.getInstance();
        settings.dashboards.put(id, ar.toString());
        settings.saveDashboardSettingsToPrefs(id, context);
    }

    void loadDashboard() {

        mWidgets.clear();

        String data = AppSettings.getInstance().dashboards.get(id);
        if (id!= 0 && (data==null || data.isEmpty())) return;

        if (data!=null && !data.isEmpty()) {
            JsonReader jsonReader = new JsonReader(new StringReader(data));

            try {
                //TODO данных может не быть
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {

                    WidgetData widget = new WidgetData();

                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String name = jsonReader.nextName();
                        switch (name) {
                            case "type":
                                String type_text = jsonReader.nextString();
                                if (type_text.equals("VALUE")) {
                                    widget.type = WidgetData.WidgetTypes.VALUE;
                                } else if (type_text.equals("SWITCH")) {
                                    widget.type = WidgetData.WidgetTypes.SWITCH;
                                } else if (type_text.equals("BUTTON")) {
                                    widget.type = WidgetData.WidgetTypes.BUTTON;
                                } else if (type_text.equals("RGBLed")) {
                                    widget.type = WidgetData.WidgetTypes.RGBLed;
                                } else if (type_text.equals("SLIDER")) {
                                    widget.type = WidgetData.WidgetTypes.SLIDER;
                                } else if (type_text.equals("HEADER")) {
                                    widget.type = WidgetData.WidgetTypes.HEADER;
                                } else if (type_text.equals("METER")) {
                                    widget.type = WidgetData.WidgetTypes.METER;
                                } else if (type_text.equals("GRAPH")) {
                                    widget.type = WidgetData.WidgetTypes.GRAPH;
                                } else if (type_text.equals("BUTTONSSET")) {
                                    widget.type = WidgetData.WidgetTypes.BUTTONSSET;
                                } else if (type_text.equals("COMBOBOX")) {
                                    widget.type = WidgetData.WidgetTypes.COMBOBOX;
                                } else new Exception("Error!");

                                break;
                            case "name":
                                widget.setName(0, jsonReader.nextString());
                                break;
                            case "name1":
                                widget.setName(1, jsonReader.nextString());
                                break;
                            case "name2":
                                widget.setName(2, jsonReader.nextString());
                                break;
                            case "name3":
                                widget.setName(3, jsonReader.nextString());
                                break;
                            case "publish":
                                widget.publishTopic_ = jsonReader.nextString();
                                break;
                            case "subscribe":
                                widget.subscribeTopic_ = jsonReader.nextString();
                                break;
                            case "publishValue":
                                widget.publishValue = jsonReader.nextString();
                                break;
                            case "topic":
                                widget.setTopic(0, jsonReader.nextString());
                                break;
                            case "topic1":
                                widget.setTopic(1, jsonReader.nextString());
                                break;
                            case "topic2":
                                widget.setTopic(2, jsonReader.nextString());
                                break;
                            case "topic3":
                                widget.setTopic(3, jsonReader.nextString());
                                break;

                            case "publishValue2":
                                widget.publishValue2 = jsonReader.nextString();
                                break;
                            case "primaryColor":
                                widget.setPrimaryColor(0, jsonReader.nextInt());
                                break;
                            case "primaryColor1":
                                widget.setPrimaryColor(1, jsonReader.nextInt());
                                break;
                            case "primaryColor2":
                                widget.setPrimaryColor(2, jsonReader.nextInt());
                                break;
                            case "primaryColor3":
                                widget.setPrimaryColor(3, jsonReader.nextInt());
                                break;
                            case "feedback":
                                widget.feedback = jsonReader.nextBoolean();
                                break;
                            case "label":
                                widget.label = jsonReader.nextString();
                                break;
                            case "label2":
                                widget.label2 = jsonReader.nextString();
                                break;
                            case "newValueTopic":
                                widget.newValueTopic = jsonReader.nextString();
                                break;
                            case "retained":
                                widget.retained = jsonReader.nextBoolean();
                                break;
                            case "additionalValue":
                                widget.additionalValue = jsonReader.nextString();
                                break;
                            case "additionalValue2":
                                widget.additionalValue2 = jsonReader.nextString();
                                break;
                            case "additionalValue3":
                                widget.additionalValue3 = jsonReader.nextString();
                                break;
                            case "decimalMode":
                                widget.decimalMode = jsonReader.nextBoolean();
                                break;
                            case "mode":
                                widget.mode = jsonReader.nextInt();
                                break;
                            case "onShowExecute":
                                widget.onShowExecute = jsonReader.nextString();
                                break;
                            case "onReceiveExecute":
                                widget.onReceiveExecute = jsonReader.nextString();
                                break;
                            case "formatMode":
                                widget.formatMode = jsonReader.nextString();
                                break;
                            case "uid":
                                String uidString=jsonReader.nextString();
                                widget.uid=Utilites.createUUIDByString(uidString);
                                break;
                            case "deviceId":
                                widget.deviceId=jsonReader.nextInt();;
                                break;
                        }
                    }
                    jsonReader.endObject();

                    //переход с какого-то старья
                    if (widget.getTopic(0).equals("")) { //переходный момент
                        switch (widget.type) {
                            case SWITCH:
                            case BUTTON:
                                widget.setTopic(0, widget.publishTopic_);
                                break;
                            case RGBLed:
                            case VALUE:
                                widget.setTopic(0,widget.subscribeTopic_);
                                break;
                        }
                    }

                    //переход с 32 на 33
                    if (widget.publishValue.length() == 0) {
                        if (widget.type == WidgetData.WidgetTypes.SWITCH || widget.type == WidgetData.WidgetTypes.RGBLed) {
                            widget.publishValue = "1";
                        }
                    }
                    if (widget.publishValue2.length() == 0) {
                        if (widget.type == WidgetData.WidgetTypes.SWITCH || widget.type == WidgetData.WidgetTypes.RGBLed) {
                            widget.publishValue2 = "0";
                        }
                    }
                    if (widget.type == WidgetData.WidgetTypes.BUTTON) {
                        if (widget.getPrimaryColor(0) == 0) {
                            widget.setPrimaryColor(0, MyColors.getGreen());
                        }
                        if (widget.label == null) {
                            widget.label = "ON";
                        }
                    }

                    if (widget.type == WidgetData.WidgetTypes.SLIDER) {
                        if (widget.additionalValue3 == null || widget.additionalValue3.equals("")) {
                            widget.additionalValue3 = "1";
                        }
                    }

                    if (widget.type == WidgetData.WidgetTypes.VALUE && widget.getPrimaryColor(0)==0) {
                        widget.setPrimaryColor(0, MyColors.getAsBlack());
                    }

                    if (widget.type == WidgetData.WidgetTypes.BUTTONSSET && (widget.formatMode==null)) {
                        widget.formatMode="4";
                    }


                    mWidgets.add(widget);

                }
                jsonReader.endArray();


                jsonReader.close();
            } catch (
                    IOException e
                    )

            {
                e.printStackTrace();
            }
        }


        if (id == 0 && data.isEmpty()) {
            initDemoDashboard();
        }

    }

    void initDemoDashboard() {


        mWidgets.clear();
        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Demo server fake alarm!", "out/wcs/push_notifications", "", "Demo server fake alarm!", 0));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.HEADER, "Value example", "", "", "", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "Server time (Value)", "out/wcs/time", "", "", MyColors.getAsBlack(), ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.HEADER, "Graph (JSON array of double/integer)", "", "", "", 0, ""));

        WidgetData wd=new WidgetData(WidgetData.WidgetTypes.GRAPH, "Source sin(x), refresh in 3 seconds", "out/wcs/graph", "", "", MyColors.getBlue(), "");
        wd.mode= Graph.WITHOUT_HISTORY;
        mWidgets.add(wd);

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.HEADER, "RGB LED, Switch and Button example", "", "", "", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "Valves are opened (RGB LED)", "out/wcs/v0", "1", "0", MyColors.getGreen(), ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "Valves are closed (inverted input)", "out/wcs/v0", "0", "1", MyColors.getRed(), ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.SWITCH, "Valves (Switch)", "out/wcs/v0", "1", "0", 0, ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Open valves (Button)", "out/wcs/v0", "1", "", MyColors.getGreen(), "OPEN", "", true));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Close valves (Button)", "out/wcs/v0", "0", "", MyColors.getRed(), "CLOSE", "", true));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.HEADER, "Slider and Meter example", "", "", "", 0, ""));
        mWidgets.add((new WidgetData(WidgetData.WidgetTypes.METER, "Light (Meter)", "out/wcs/slider", "0", "255", 0, "").setAdditionalValues("30", "0")));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "Light (Value)", "out/wcs/slider", "0", "", MyColors.getAsBlack(), "out/wcs/slider"));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.SLIDER, "Light (Slider)", "out/wcs/slider", "0", "255", 0, ""));

        wd=new WidgetData(WidgetData.WidgetTypes.GRAPH, "Source - Light (Slider)", "out/wcs/slider", "", "", MyColors.getRed(), "");
        wd.mode= Graph.LIVE;
        mWidgets.add(wd);


        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.HEADER, "RGB LED all modes(on/off/#rrggbb)", "", "", "", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "RGB LED (default is red)", "out/wcs/rgbled_test", "ON", "OFF", MyColors.getRed(), ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTONSSET, "LED Modes (Buttons set)", "out/wcs/rgbled_test", "ON,OFF,,,#ff5555|red,#55ff55|green,#5555ff|blue", "", MyColors.getAsBlack(), "post 'ON'", "", true));

        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "ON", "out/wcs/rgbled_test", "ON", "", MyColors.getGray(), "post 'ON'", "", true));
        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "OFF", "out/wcs/rgbled_test", "OFF", "", MyColors.getGray(), "post 'OFF'", "", true));
        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Red", "out/wcs/rgbled_test", "#FF0000", "", MyColors.getRed(), "post #FF0000", "", true));
        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Green", "out/wcs/rgbled_test", "#00FF00", "", MyColors.getGreen(), "post #00FF00", "", true));

    }

    void initMyHomeDashboard() {
        mWidgets.clear();
        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Демо push", "wcs/push_notifications", "Demo server fake alarm!", "", 0));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "Время сервера", "wcs/time", "", "", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "Подача воды", "wcs/valves", "1", "0", MyColors.getGreen(), ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.SWITCH, "Краны", "wcs/in/valves", "1", "0", 0, ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "Режим аварии", "wcs/accident", "1", "0", MyColors.getRed(), ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Сброс режима аварии", "wcs/in/accident", "0", "", MyColors.getGreen(), "Сброс", "", false));

        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Close valves (Button)", "", "in/wcs/v0", "0", 0, false));
        //mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Open valves (Button)", "", "in/wcs/v0", "1", 0, false));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "ХВС", "wcs/cwm", "", "", MyColors.getAsBlack(), "wcs/in/cwm"));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "ГВС", "wcs/hwm", "", "", MyColors.getAsBlack(), "wcs/in/hwm"));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "ХВС (сутки)", "wcs/daily_cwm", "", "", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "ГВС (сутки)", "wcs/daily_hwm", "", "", 0, ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.BUTTON, "Сброс суточного", "wcs/in/cmd", "reset_daily", "", MyColors.getGreen(), "Сброс", "", false));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.METER, "WiFi датчик #0 vcc", "wcs/wifi_probe0_vcc", "2500", "4094", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "Время отчета датчик #0", "wcs/wifi_probe0_last_report", "", "", MyColors.getAsBlack(), ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "Авария датчик #0", "wcs/wifi_probe0_accident", "1", "0", MyColors.getGreen(), ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.METER, "WiFi датчик #1 vcc", "wcs/wifi_probe1_vcc", "2500", "4094", 0, ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.VALUE, "Время отчета датчик #1", "wcs/wifi_probe1_last_report", "", "", MyColors.getAsBlack(), ""));
        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.RGBLed, "Авария датчик #1", "wcs/wifi_probe1_accident", "1", "0", MyColors.getGreen(), ""));

        mWidgets.add(new WidgetData(WidgetData.WidgetTypes.SWITCH, "Аналоговый датчик активен", "wcs/in/analog_sensor_active", "1", "0", 0, ""));
    }

}

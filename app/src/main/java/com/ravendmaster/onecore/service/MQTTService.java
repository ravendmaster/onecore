package com.ravendmaster.onecore.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;

import com.ravendmaster.onecore.Utilites;
import com.ravendmaster.onecore.customview.Graph;
import com.ravendmaster.onecore.database.DbHelper;
import com.ravendmaster.onecore.database.HistoryCollector;
import com.ravendmaster.onecore.Log;
import com.ravendmaster.onecore.TabData;
import com.ravendmaster.onecore.TabsCollection;
import com.ravendmaster.onecore.activity.MainActivity;
import com.ravendmaster.onecore.R;
import com.ravendmaster.onecore.devices.Device;
import com.squareup.duktape.Duktape;

import org.fusesource.hawtbuf.Buffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MQTTService extends Service implements CallbackMQTTClient.IMQTTMessageReceiver {

    final static int STATE_DISCONNECTED = 0;
    final static int STATE_HALF_CONNECTED = 1;
    final static int STATE_FULL_CONNECTED = 2;
    int currentConnectionState;

    String push_topic;

    int currentDataVersion = 0;

    static int clientCountsInForeground = 0;

    CallbackMQTTClient callbackMQTTClient;

    public ArrayList<Dashboard> dashboards;

    public long timeShowAd;


    public int getFreeDashboardId() {
        int result = -1;
        for (Dashboard dashboard :
                dashboards) {
            result = Math.max(result, dashboard.id);
        }
        //Log.d("free", ""+result);
        return result + 1;
    }


    private HashMap<String, String> lastReceivedMessagesByTopic;

    HashMap currentMQTTValues;

    Dashboard getDashboardByID(int id) {
        //Log.d("dashboard orders", " getDashboardByID()"+index);
        //if(index>=dashboards.size())return null;
        for (Dashboard dashboard : dashboards) {
            if (dashboard.id == id) return dashboard;
        }
        new Exception("can't find dashboard by ID");
        return null;
    }

    HashMap getCurrentMQTTValues() {
        return currentMQTTValues;
    }

    private static MQTTService instance;

    static public MQTTService getInstance() {
        return instance;
    }

    public int activeTabIndex = 0;
    public int screenActiveTabIndex = 0;

    private static final String FULL_VERSION_FOR_ALL = "full_version_for_all";
    private static final String MESSAGE_TITLE = "message_title";
    private static final String MESSAGE_TEXT = "message_text";
    private static final String AD_FREQUENCY = "ad_frequency";
    private static final String REBUILD_HISTORY_DATA_FREQUENCY = "rebuild_history_data_frequency";

    public class PopUpMessage {
        String title;
        String text;

        public PopUpMessage(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    public String getMQTTCurrentValue(String topic) {
        if (getCurrentMQTTValues() == null) return "";

        String value = (String) getCurrentMQTTValues().get(topic);

        return value == null ? "" : value;
    }

    String lastMessageText = "";

    public void OnCreate(AppCompatActivity appCompatActivity) {

        Context context = getApplicationContext();
        AppSettings appSettings = AppSettings.getInstance();
        appSettings.readFromPrefs(context);
        createDashboardsBySettings();

        activeTabIndex = appSettings.tabs.getDashboardIdByTabIndex(screenActiveTabIndex);

    }

    private Duktape duktape;

    private WidgetData contextWidgetData;

    public String evalJS(WidgetData contextWidgetData, String value, String code) {
        this.contextWidgetData = contextWidgetData;
        String result = value;
        try {
            result = duktape.evaluate("var value='" + value + "'; " + code + "; String(value);");
        } catch (Exception e) {
            Log.d("script", "exec: " + e);
        }
        return result;
    }

    interface IMQTT {
        String read(String topic);

        void publish(String topic, String payload);

        void publishr(String topic, String payload);
    }

    IMQTT imqtt = new IMQTT() {
        @Override
        public String read(String topic) {
            return getMQTTCurrentValue(topic);
        }

        @Override
        public void publish(String topic, String payload) {
            publishMQTTMessage(topic, new Buffer(payload.getBytes()), false);
        }

        @Override
        public void publishr(String topic, String payload) {
            publishMQTTMessage(topic, new Buffer(payload.getBytes()), true);
        }
    };

    interface INotifier {
        void push(String topic);

        void stop();
    }

    INotifier notifier = new INotifier() {
        @Override
        public void push(String message) {
            publishMQTTMessage(getServerPushNotificationTopicForTextMessage(contextWidgetData.uid.toString()), new Buffer(message.getBytes()), true);
        }

        public void stop() {
            publishMQTTMessage(getServerPushNotificationTopicForTextMessage(contextWidgetData.uid.toString()), new Buffer("".getBytes()), true);
        }
    };

    public String getServerPushNotificationTopicRootPath() { //корень топиков от сервера приложения
        String rootPushTopic = AppSettings.getInstance().push_notifications_subscribe_topic;
        return rootPushTopic.replace("#", "") + "$server";
    }

    public String getServerPushNotificationTopicForTextMessage(String id) { //для текстовых сообщений
        return getServerPushNotificationTopicRootPath() + "/message" + Integer.toHexString(id.hashCode());
    }

    public void createDashboardsBySettings() {

        dashboards = new ArrayList<>();

        if (AppSettings.getInstance().settingsVersion == 0) {
            //старый способ
            TabsCollection tabs = AppSettings.getInstance().tabs;
            for (int i = 0; i < 4; i++) {
                TabData tabData = tabs.getItems().get(i);
                if (tabData == null || tabData.name.equals("")) {
                    continue;
                }
                Dashboard tempDashboard = new Dashboard(i);
                tempDashboard.loadDashboard();
                dashboards.add(tempDashboard);
            }
        } else {
            for (TabData tabData : AppSettings.getInstance().tabs.getItems()) {
                Dashboard tempDashboard = new Dashboard(tabData.id);
                tempDashboard.loadDashboard();
                dashboards.add(tempDashboard);

            }

        }

    }

    HashMap<String, String> getTopicsForHistoryCollect() {
        HashMap<String, String> graph_topics = new HashMap<String, String>();
        for (Dashboard dashboard : dashboards) {
            for (WidgetData widgetData : dashboard.getWidgetsList()) {
                if (widgetData.type != WidgetData.WidgetTypes.GRAPH) continue;
                for (int i = 0; i < 4; i++) {
                    String topic = widgetData.getTopic(i);
                    if (topic != null && !topic.isEmpty() && widgetData.mode >= Graph.PERIOD_TYPE_1_HOUR) {
                        //String topicForHistoryData = topic.substring(0, topic.length() - 4);
                        graph_topics.put(topic, topic);
                    }
                }
            }
        }
        return graph_topics;
    }

    HashMap<String, String> getTopicsForLiveCollect() {
        HashMap<String, String> graph_topics = new HashMap<String, String>();
        for (Dashboard dashboard : dashboards) {
            for (WidgetData widgetData : dashboard.getWidgetsList()) {
                if (widgetData.type != WidgetData.WidgetTypes.GRAPH) continue;
                for (int i = 0; i < 4; i++) {
                    String topic = widgetData.getTopic(i);
                    if (topic != null && !topic.isEmpty() && widgetData.mode == Graph.LIVE) {
                        //String topicForHistoryData = topic.substring(0, topic.length() - 4);
                        graph_topics.put(topic, topic);
                    }
                }
            }
        }
        return graph_topics;
    }

    public MQTTService() {
        Log.d(getClass().getName(), "constructor MQTTService()");

        instance = this;

        duktape = Duktape.create();
        duktape.bind("MQTT", IMQTT.class, imqtt);
        duktape.bind("Notifier", INotifier.class, notifier);
        Log.d(getClass().getName(), "duktape start");


        lastReceivedMessagesByTopic = new HashMap<>();

        currentMQTTValues = new HashMap();

        currentConnectionState = STATE_DISCONNECTED;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //Log.d("connection_test", "inRealForegroundMode="+inRealForegroundMode);
                    if (connectionInUnActualMode) {
                        connectionInUnActualMode = false;
                        if (isConnected()) callbackMQTTClient.disconnect();
                        while (isConnected()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        AppSettings appSettings = AppSettings.getInstance();
                        appSettings.readFromPrefs(getApplicationContext());
                        callbackMQTTClient.disconnect();//!!!!! предыдущие соединения тоже соединяться с новыми параметры, поэтому отключаем их силой
                        callbackMQTTClient.connect(appSettings);
                        subscribeForState(STATE_FULL_CONNECTED);
                    }


                    if ((!inRealForegroundMode && MQTTService.clientCountsInForeground > 0)) {
                        //Log.d("connection_test", "start foreground mode");

                        AppSettings appSettings = AppSettings.getInstance();

                        appSettings.readFromPrefs(getApplicationContext());
                        if (isConnected()) {
                            //Log.d("connection_test", "isConnected");
                            subscribeForState(STATE_FULL_CONNECTED);
                            //3.0 callbackMQTTClient.unsubscribe(appSettings.push_notifications_subscribe_topic);
                        } else {
                            //Log.d("connection_test", "! isConnected");
                            callbackMQTTClient.disconnect();//предыдущие соединения тоже соединятся с новыми параметры, поэтому отключаем их принудительно
                            callbackMQTTClient.connect(appSettings);

                            while (!isConnected()) {
                                //Log.d("connection_test", "wait...");
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (connectionInUnActualMode) {
                                    //Log.d("test", "a");
                                    break;
                                }
                            }
                            if (connectionInUnActualMode) continue;

                            subscribeForState(STATE_FULL_CONNECTED);

                        }

                        inRealForegroundMode = true;

                    }

                    if (MQTTService.clientCountsInForeground == 0) {
                        idleTime += 1;
                    } else {
                        idleTime = 0;
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (inRealForegroundMode & idleTime > 100) { //100*100 = 10 sec
                        AppSettings appSettings = AppSettings.getInstance();
                        appSettings.readFromPrefs(getApplicationContext());
                        //callbackMQTTClient.reConnect(appSettings);

                        if (!appSettings.isServerMode()) {
                            Log.d(getClass().getName(), "Go to the background.");
                            if (appSettings.connection_in_background && !appSettings.push_notifications_subscribe_topic.isEmpty()) {
                                if (!isConnected()) {
                                    callbackMQTTClient.connect(appSettings);
                                } else {
                                    subscribeForState(STATE_HALF_CONNECTED);
                                    //3.0 callbackMQTTClient.unsubscribe(appSettings.subscribe_topic);
                                }
                            } else {
                                callbackMQTTClient.disconnect();
                            }
                            inRealForegroundMode = false;

                        } else {
                            //Log.d(getClass().getName(), "Do not go to the background. Server mode is on.");
                        }
                    }

                    //Log.d("test", "idleTime="+idleTime);
                }

            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() { //живые данные

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                AppSettings appSettings = AppSettings.getInstance();


                while (true) {

                    if ((clientCountsInForeground > 0 || appSettings.isServerMode()) && db != null && dashboards != null) {

                        topicsForLive = getTopicsForLiveCollect();

                        final Enumeration<String> strEnum = Collections.enumeration(topicsForLive.keySet());
                        while (strEnum.hasMoreElements()) {
                            String topicForHistoryData = strEnum.nextElement();//widgetData.getTopic(0).substring(0, widgetData.getTopic(0).length() - 4);
                            String historyData = prepareHistoryGraphicData(topicForHistoryData, new int[]{Graph.LIVE});
                            processReceiveSimplyTopicPayloadData(topicForHistoryData + Graph.LIVE_TOPIC_SUFFIX, historyData);
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

    }

    HashMap<String, String> topicsForHistory;
    HashMap<String, String> topicsForLive;


    static final String TOPICS_DATA = "topics_data";
    public String SERVER_DATAPACK_NAME = "";//"serverDataPack";


    String prepareHistoryGraphicData(String sourceTopic, int[] period_types) {
        //добыча id топика
        Long topic_id = historyCollector.getTopicIDByName(sourceTopic);

        //аггрегация
        JSONObject resultJson = new JSONObject();
        JSONArray graphics = new JSONArray();

        for (int period_type : period_types) {

            long aggregationPeriod = Graph.aggregationPeriod[period_type];
            int periods_count = Graph.getPeriodCount(period_type);

            long period = aggregationPeriod * periods_count;
            Float[] mass = new Float[periods_count + 1];

            Date now = new Date();
            long time_now = now.getTime();

            GregorianCalendar c = new GregorianCalendar();
            switch (period_type) {
                case Graph.PERIOD_TYPE_4_HOUR:
                    c.add(Calendar.MINUTE, 30);
                    c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE) < 30 ? 0 : 30, 0);
                    time_now += (c.getTime().getTime() - time_now);
                    break;
                case Graph.PERIOD_TYPE_1_DAY:
                    c.add(Calendar.HOUR_OF_DAY, 1);
                    c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), 0, 0);
                    time_now += (c.getTime().getTime() - time_now);
                    break;
                case Graph.PERIOD_TYPE_1_WEEK:
                case Graph.PERIOD_TYPE_1_MOUNT:
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                    time_now += (c.getTime().getTime() - time_now);
                    break;
            }

            long now_raw = ((long) (time_now / aggregationPeriod)) * aggregationPeriod;
            long time_line = now_raw - period;

            //данные за период
            String selectQuery = "SELECT  MAX(timestamp/?), COUNT(timestamp), ROUND(AVG(value),2) FROM HISTORY WHERE detail_level=0 AND topic_id=? AND timestamp>=? GROUP BY timestamp/? ORDER BY timestamp DESC";
            Cursor cursor = db.rawQuery(selectQuery, new String[]{
                    String.valueOf(aggregationPeriod),
                    String.valueOf(topic_id),
                    String.valueOf(time_line),
                    String.valueOf(aggregationPeriod)
            });
            if (cursor.moveToFirst()) {
                do {
                    float res = cursor.getFloat(2);
                    int index = (int) (now_raw / aggregationPeriod - cursor.getLong(0));
                    if (index >= 0 && index <= periods_count) {
                        mass[index] = res;
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();


            //актуальное значение
            selectQuery = "SELECT ROUND(value,2) FROM HISTORY WHERE detail_level=0 AND topic_id=? ORDER BY timestamp DESC LIMIT 1";
            cursor = db.rawQuery(selectQuery, new String[]{
                    String.valueOf(topic_id),
            });
            Float actual_value = null;
            if (cursor.moveToFirst()) {
                actual_value = cursor.getFloat(0);
            }
            cursor.close();

            if (period_type <= Graph.PERIOD_TYPE_1_HOUR && actual_value != null) { //нужно только для живого и часового графиков, более крупным - нет

                for (int i = 0; i < periods_count; i++) {
                    if (mass[i] == null) {
                        mass[i] = actual_value;
                    } else {
                        break;
                    }
                }
                //заполняем пропуски
                Float lastVal = null;
                for (int i = periods_count - 1; i >= 0; i--) {
                    if (mass[i] == null) {
                        mass[i] = lastVal;
                    } else {
                        lastVal = mass[i];
                    }
                }


            }


            JSONArray dots = new JSONArray();
            for (int i = 0; i < periods_count; i++) {
                dots.put(mass[i] == null ? "" : mass[i]);
            }

            JSONObject GraphLineJson = new JSONObject();
            try {
                GraphLineJson.put("period_type", period_type);
                GraphLineJson.put("actual_timestamp", time_now);
                GraphLineJson.put("aggregation_period", aggregationPeriod);
                GraphLineJson.put("dots", dots);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            graphics.put(GraphLineJson);
        }

        try {
            resultJson.put("type", "graph_history");
            resultJson.put("graphics", graphics);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultJson.toString();
    }

    static DbHelper mDbHelper;
    static SQLiteDatabase db;

    HistoryCollector historyCollector;


    static boolean connectionInUnActualMode = false;

    public void connectionSettingsChanged() {
        connectionInUnActualMode = true;
    }

    private String getUserLevelTopicPath() {
        AppSettings settings = AppSettings.getInstance();
        return settings.username + "/";
    }

    private String getRootTopicPath(WidgetData widgetData) {
        AppSettings settings = AppSettings.getInstance();
        return getUserLevelTopicPath() + settings.getDeviceMacById(widgetData.deviceId) + "/";
    }

    String getRootTopicPathForSend(WidgetData widgetData) {
        return getRootTopicPath(widgetData) + "in/";
    }

    String getRootTopicPathForReceive(WidgetData widgetData) {
        return getRootTopicPath(widgetData) + "out/";
    }

    private String getPushTopicRoot() {
        return getUserLevelTopicPath() + "pn/";
    }

    public void requestRefreshGraphData(WidgetData widgetDataForUpdate) {
        AppSettings appSettings = AppSettings.getInstance();

        for (Dashboard dashboard : dashboards) {
            for (WidgetData widgetData : dashboard.getWidgetsList()) {
                if (widgetData.type != WidgetData.WidgetTypes.GRAPH || widgetData.mode < 2)
                    continue;
                if (widgetDataForUpdate != null && widgetDataForUpdate != widgetData) continue;

                publishMQTTMessage(appSettings.username + "/requestDataOfGraphics", new Buffer((widgetData.getTopic(0) + "," + widgetData.mode).getBytes()), false);
            }

        }
    }

    public void subscribeForInteractiveMode(AppSettings appSettings) {
        callbackMQTTClient.subscribe(getUserLevelTopicPath() + "+/out/#");
        callbackMQTTClient.subscribe(getPushTopicRoot() + "#");

        requestRefreshGraphData(null);
    }

    public void subscribeForBackgroundMode(AppSettings appSettings) {
        callbackMQTTClient.unsubscribe(getUserLevelTopicPath() + "+/out/#");
        callbackMQTTClient.subscribe(getPushTopicRoot() + "#");
    }


    public void subscribeForState(int newState) {
        AppSettings appSettings = AppSettings.getInstance();
        appSettings.readFromPrefs(getApplicationContext());

        switch (newState) {
            case STATE_FULL_CONNECTED:
                //3.0 callbackMQTTClient.subscribe(appSettings.subscribe_topic);
                subscribeForInteractiveMode(appSettings);
                break;
            case STATE_HALF_CONNECTED:
                if (appSettings.connection_in_background) {
                    //3.0 callbackMQTTClient.subscribe(appSettings.push_notifications_subscribe_topic);
                    subscribeForBackgroundMode(appSettings);
                }
                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(getClass().getName(), "onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(getClass().getName(), "onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(getClass().getName(), "onStartCommand()");

        String action = intent == null ? "autostart" : intent.getAction();
        Log.d(getClass().getName(), "onStartCommand: " + action);

        switch (action) {
            case "autostart":

                break;
            case "interactive":
                clientCountsInForeground++;
                //Log.d("test", "clientCountsInForeground++ ="+clientCountsInForeground);
                break;
            case "pause":
                clientCountsInForeground--;
                //Log.d("test", "clientCountsInForeground-- ="+clientCountsInForeground);
                break;
        }

        AppSettings appSettings = AppSettings.getInstance();
        appSettings.readFromPrefs(getApplicationContext());

        if (callbackMQTTClient == null) {
            Log.d(getClass().getName(), "new CallbackMQTTClient()");
            callbackMQTTClient = new CallbackMQTTClient(this);
        }

        //callbackMQTTClient.reConnect(appSettings);

        Log.d(getClass().getName(), "clientCountsInForeground=" + clientCountsInForeground);

        push_topic = appSettings.push_notifications_subscribe_topic;

        //showNotifyStatus(appSettings.push_notifications_subscribe_topic, false);

        showNotifyStatus("Running...", false);


        String rootSubscribeTopic = "";//3.0 appSettings.subscribe_topic.endsWith("#") ? appSettings.subscribe_topic.substring(0, appSettings.subscribe_topic.length() - 1) : appSettings.subscribe_topic;

        SERVER_DATAPACK_NAME = appSettings.server_topic;


        if (mDbHelper == null) {
            mDbHelper = new DbHelper(getApplicationContext());
            db = mDbHelper.getWritableDatabase();
            historyCollector = new HistoryCollector(db);
        }
        historyCollector.needCollectData = appSettings.isServerMode() || (clientCountsInForeground > 0);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (wl == null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        }
        if (appSettings.isServerMode()) {
            wl.acquire();
        } else {
            if (wl.isHeld()) {
                wl.release();
            }
            wl = null;
        }

        return START_STICKY;
    }

    static PowerManager.WakeLock wl;

    static boolean inRealForegroundMode = false;
    static int idleTime = 0;

    void showNotifyStatus(String text1, Boolean cancel) {
        Intent foreground_intent = new Intent(this, MainActivity.class);

        foreground_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        foreground_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, foreground_intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                //builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                //builder.setLights(Color.RED, 100, 100);
                //builder.setAutoCancel(false);
                //builder.setTicker("text1");
                .setContentTitle("OneCore")
                //.setContentText("Application server mode is on")
                .setSmallIcon(R.drawable.ic_playblack)
                .setContentIntent(pendingIntent)
                .setOngoing(true).setSubText(text1)
                //builder.setNumber(100);
                .setAutoCancel(true);
        ;

        if (cancel) {
            stopForeground(true);
        } else {
            //myNoticationBackground = builder.getNotification();
            startForeground(1, builder.build());
        }
    }


    void showPushNotification(String topic, String appSender, String message) {

        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        Notification.Builder builder = new Notification.Builder(this)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                //.setFullScreenIntent(pendingIntent, true) //сверхц экрана появляется
                .setLights(Color.RED, 100, 100)
                //.setTicker(subtext)
                .setContentTitle("OneCore")
                .setContentText(appSender)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                //.setOngoing(true) // не смахиваемое
                .setSubText(message)//"This is subtext...");   //API level 16
                //.setNumber(lastReceivedMessagesByTopic.size())
                .setAutoCancel(true);

        //builder.build();

        //myNotication = builder.getNotification();
        NotificationManager manager;
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(getStringHash(topic), builder.build());// myNotication);

    }

    int getStringHash(String text) {
        int hash = 7;
        for (int i = 0; i < text.length(); i++) {
            hash = hash * 31 + text.charAt(i);
        }
        return hash;
    }

    public void publishMQTTMessage(String topic, Buffer payload, boolean retained) {
        if (topic == null || topic.equals("") || payload == null) return;
        if (callbackMQTTClient == null) return;
        callbackMQTTClient.publish(topic, payload, retained);
        if(retained){
            callbackMQTTClient.publish(topic.replace("/in/","/out/"), payload, retained);
            //currentMQTTValues.put(topic.replace("/in/","/out/"), payload.toByteBuffer().toString());
        }
    }

    public boolean isConnected() {
        return callbackMQTTClient == null ? false : callbackMQTTClient.isConnected();
    }

    public void setPayLoadChangeHandler(Handler payLoadChanged) {
        mPayloadChanged = payLoadChanged;
    }

    Handler mPayloadChanged = null;

    void notifyDataInTopicChanged(String topic, String payload) {
        if (mPayloadChanged != null) {
            if (!payload.equals(currentMQTTValues.get(topic))) {
                Message msg = new Message();
                msg.obj = topic;
                mPayloadChanged.sendMessage(msg);
            }
        }
    }

    ArrayList<String> currentSessionTopicList = new ArrayList<>();

    @Override
    public void onReceiveMQTTMessage(String topic, Buffer payload) {
        Log.d("MQTTService", "onReceiveMQTTMessage()" + topic + " " + payload);
        if (topic.equals(SERVER_DATAPACK_NAME)) {
            processUniversalPack(payload);
        } else {

            if (currentSessionTopicList.indexOf(topic) == -1) {
                currentSessionTopicList.add(topic);
                Log.d("currentSessionTopicList", "add:" + topic);
            }

            //historyCollector.getTopicIDByName(topic);//регистрируем имя в базе

            String payloadAsString = null;
            try {
                payloadAsString = new String(payload.toByteArray(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (historyCollector != null && !topic.equals(SERVER_DATAPACK_NAME)) {
                if ((topicsForHistory != null && topicsForHistory.get(topic) != null) || (topicsForLive != null && topicsForLive.get(topic) != null)) {
                    historyCollector.collect(topic, payloadAsString);
                    //Log.d("collect", ""+payloadAsString);
                }
            }
            processReceiveSimplyTopicPayloadData(topic, payloadAsString);
        }
    }

    void processUniversalPack(Buffer payload) {

        String payloadAsString = null;
        try {
            //payloadAsString = new String(payload.toByteArray(), "UTF-8");
            //разжимае
            InputStream is_ = new ByteArrayInputStream(payload.toByteArray());
            ZipInputStream is = new ZipInputStream(new BufferedInputStream(is_));

            //int version=is.read();
            ZipEntry entry;
            while ((entry = is.getNextEntry()) != null) {

                ByteArrayOutputStream os = new ByteArrayOutputStream();


                byte[] buff = new byte[1024];
                int count;
                while ((count = is.read(buff, 0, 1024)) != -1) {
                    os.write(buff, 0, count);
                }
                os.flush();
                os.close();

                payloadAsString = Utilites.bytesToStringUTFCustom(os.toByteArray(), os.toByteArray().length);
            }
            is.close();
            is_.close();


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        JsonReader jsonReader = new JsonReader(new StringReader(payloadAsString));
        try {
            Integer ver = 0;
            String type = null;
            String data = null;

            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String paramName = jsonReader.nextName();
                switch (paramName) {
                    case "ver":
                        ver = jsonReader.nextInt();
                        break;
                    case "type":
                        type = jsonReader.nextString();
                        break;
                    case "data":
                        data = jsonReader.nextString();
                        break;
                }
            }
            jsonReader.endObject();
            jsonReader.close();

            if (type.equals(TOPICS_DATA)) {

                jsonReader = new JsonReader(new StringReader(data));
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    String topicName = null;
                    String payloadData = null;

                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String paramName = jsonReader.nextName();
                        switch (paramName) {
                            case "topic":
                                topicName = jsonReader.nextString();
                                break;
                            case "payload":
                                payloadData = jsonReader.nextString();
                                break;
                        }
                    }
                    jsonReader.endObject();

                    //Log.d("servermode", "topicName="+topicName+"  payload"+payloadData);
                    processReceiveSimplyTopicPayloadData(topicName, payloadData);
                }
                jsonReader.endArray();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //OnReceive()
    void processOnReceiveEvent(String topic, String payload) {
/*
        if (!AppSettings.getInstance().isServerMode()) return;

        for (Dashboard dashboard : dashboards) {
            for (WidgetData widgetData : dashboard.getWidgetsList()) {
                if (!widgetData.getTopic(0).equals(topic)) continue;

                String code = widgetData.onReceiveExecute;
                if (code.isEmpty()) continue;

                evalJS(widgetData, payload, code);
            }
        }
*/
    }


    private void processReceiveSimplyTopicPayloadData(String topic, String payload) {

        //processOnReceiveEvent(topic, payload);

        notifyDataInTopicChanged(topic, payload);

        currentMQTTValues.put(topic, payload);
        currentDataVersion++;

        if (topic.startsWith(getPushTopicRoot())) {
            int senderSegmentStart = getPushTopicRoot().length();
            String senderMac = topic.substring(senderSegmentStart, senderSegmentStart + 12);
            Device deviceSender = AppSettings.getInstance().getDeviceByMac(senderMac);

            //Log.d("pn", "Sender: " + senderMac);
            //String notificationTag=topic.substring(senderSegmentStart+13);
            //Log.d("pn", "Tag: "+notificationTag);

            String lastPush = lastReceivedMessagesByTopic.get(topic);
            if (lastPush == null || !lastPush.equals(payload)) {
                lastReceivedMessagesByTopic.put(topic, payload);

                if (!payload.equals("")) {
                    showPushNotification(topic, deviceSender != null ? deviceSender.name : "unknown device", payload);
                }
            }


        }
        /*
        if (push_topic != null && !push_topic.isEmpty()) {
            String push_topic_template = push_topic.replaceAll("/#", "");
            int template_size = push_topic_template.length();
            if (topic.length() >= template_size && topic.substring(0, template_size).equals(push_topic_template)) {

                String lastPush = lastReceivedMessagesByTopic.get(topic);
                if (lastPush == null || !lastPush.equals(payload)) {
                    lastReceivedMessagesByTopic.put(topic, payload);

                    if (topic.startsWith(getServerPushNotificationTopicRootPath())) {
                        //расширенное сообщение с сервера приложения, нужно интерпретировать
                        if (!payload.equals("")) {
                            showPushNotification(topic, payload);
                        }

                    } else {
                        //обычный кусок текста, нужно показать
                        if (!payload.equals("")) {
                            showPushNotification(topic, payload);
                        }
                    }
                }
            }
        }
        */
    }


}

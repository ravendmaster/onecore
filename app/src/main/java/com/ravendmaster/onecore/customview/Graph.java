package com.ravendmaster.onecore.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import com.ravendmaster.onecore.Log;
import com.ravendmaster.onecore.R;
import com.ravendmaster.onecore.activity.MainActivity;
import com.ravendmaster.onecore.service.WidgetData;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Graph extends View {

    public static final String HISTORY_TOPIC_SUFFIX = "_$hd";
    public static final String LIVE_TOPIC_SUFFIX = "_$live";

    public int X;
    public int Y;

    Paint primary_paint = new Paint();
    Paint secondary_paint = new Paint();

    Paint graph_paint = new Paint();

    Rect bounds = new Rect();

    int[] colors = new int[4];
    String[] values = new String[4];
    String[] names = new String[4];

    public void setName(int index, String name) {
        names[index] = name;
    }

    public String getName(int index) {
        return names[index];
    }

    public int getColorLight(int index) {
        return colors[index];
    }

    public void setColorLight(int index, int colorLight) {
        colors[index] = colorLight;
        invalidate();
    }

    private void init() {
        if (isInEditMode()) {
            int total_max_graph_density = 16;
            GraphLine line_temp = new GraphLine(0);
            //line_temp.setColor(MyColors.getGreen());
            for (int i = 0; i < total_max_graph_density; i++) {
                line_temp.setData(i, new Float(i));
            }
            graphs.add(line_temp);

            line_temp = new GraphLine(1);
            //line_temp.setColor(MyColors.getRed());
            for (int i = 0; i < total_max_graph_density; i++) {
                line_temp.setData(i, 20 - (float) i * 2);
            }
            graphs.add(line_temp);

            line_temp = new GraphLine(2);
            //line_temp.setColor(MyColors.getYellow());
            for (int i = 0; i < total_max_graph_density; i++) {
                line_temp.setData(i, (float) Math.sin((float) i) * 20);
            }
            graphs.add(line_temp);
        }

    }

    public Graph(Context context) {
        super(context);
        init();
        setLayerToHW(this);
    }

    public Graph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RGBLEDView,
                0, 0);
        try {
            colors[0] = a.getInteger(R.styleable.RGBLEDView_colorLight, Color.BLUE);
        } finally {
            a.recycle();
        }
        primary_paint.setAntiAlias(true);
        secondary_paint.setAntiAlias(true);
        graph_paint.setAntiAlias(true);

        secondary_paint.setColor(MyColors.getVeryLtGray());
        //secondary_paint.setStrokeWidth(3);

        setLayerToHW(this);
    }

    private void setLayerToHW(View v) {
        if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    public String getValue(int topic_index) {
        return values[topic_index];
    }

    int mode;

    public void setValue(int topic_index, String value) {
        if (value == null) return;
        this.values[topic_index] = value;

        if (!value.isEmpty()) {

            if (value.charAt(0) == '[') {
                //historyMode = false;

                for (GraphLine line : graphs){
                    if(line.topic_index==topic_index){
                        graphs.remove(line);
                        break;
                    }
                }

                GraphLine line;
                //if (graphs.size() == 0) {
                    line = new GraphLine(topic_index);
                    //line.setColor(MyColors.getRed());
                    graphs.add(line);
                //} else {
                //    line = graphs.get(0);
                //}
                //line.data.clear();

                int index = 0;
                JsonReader jsonReader = new JsonReader(new StringReader(value));
                try {
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        float val = (float) jsonReader.nextDouble();
                        line.setData(index++, val);
                    }
                    jsonReader.endArray();
                    jsonReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {//JSON с историей
                //historyMode = true;
                JsonReader jsonReader = new JsonReader(new StringReader(value));
                try {

                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String paramName = jsonReader.nextName();
                        switch (paramName) {
                            case "type":
                                String type = jsonReader.nextString();
                                //graph_history?
                                break;
                            case "graphics":
                                jsonReader.beginArray();
                                while (jsonReader.hasNext()) {
                                    //один параметр с разной детализацией
                                    GraphLine line = new GraphLine(topic_index);

                                    int index = 0;
                                    jsonReader.beginObject();
                                    while (jsonReader.hasNext()) {
                                        paramName = jsonReader.nextName();
                                        switch (paramName) {
                                            case "period_type":
                                                line.period_type = jsonReader.nextInt();
                                                break;
                                            case "actual_timestamp":
                                                line.actual_timestamp = jsonReader.nextLong();
                                                break;
                                            case "aggregation_period":
                                                line.aggregation_period = jsonReader.nextLong();
                                                break;
                                            case "dots":

                                                jsonReader.beginArray();
                                                while (jsonReader.hasNext()) {
                                                    Float val;
                                                    String element = jsonReader.nextString();
                                                    if (element.equals("")) {
                                                        val = null;
                                                    } else {
                                                        val = Float.parseFloat(element);
                                                    }
                                                    line.setData(index++, val);
                                                }
                                                jsonReader.endArray();
                                                break;
                                        }
                                    }
                                    jsonReader.endObject();

                                    for (GraphLine line_temp : graphs) {
                                        if (line_temp.topic_index==line.topic_index && line_temp.period_type == line.period_type) {
                                            graphs.remove(line_temp);
                                            break;
                                        }
                                    }

                                    graphs.add(line);
                                }
                                jsonReader.endArray();

                                break;

                        }
                    }
                    jsonReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (!isEnabled()) return true;

        //Log.d(getClass().getName(), event.toString());
        X = (int) event.getX();
        Y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Log.d(getClass().getName(), "DOWN");
                return true;
            case MotionEvent.ACTION_MOVE:

                break;

            case MotionEvent.ACTION_CANCEL:
                break;

            case MotionEvent.ACTION_UP:
                //Log.d(getClass().getName(), "UP, " + event.getAction());
                int part = X / ((getRight() - getLeft()) / 3);
                Log.d("part", "" + part);
                switch (part) {
                    case 0:
                        nextSubMode();
                        break;
                    case 1:
                        nextPeriodType();
                        break;
                    case 2:
                        previousPeriodType();
                        break;
                }

                invalidate();
                playSoundEffect(SoundEffectConstants.CLICK);

                return true;
        }
        return false;
    }

    int calcScreenY(float input, float min, float max, int screen_height) {

        float dev = (max - min);
        //dev = Math.max(dev, 1);

        return screen_height - (int) ((input - min) * screen_height / dev);
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int current_period_type) {
        this.mode = current_period_type;
        invalidate();
    }

    public int getSubmode() {
        return submode;
    }

    public void setSubmode(int submode) {
        this.submode = submode;
    }

    public ArrayList<GraphLine> getGraphs() {
        return graphs;
    }

    public void setGraphs(ArrayList<GraphLine> graphs) {
        this.graphs = graphs;
    }

    class GraphLine {
        private ArrayList<Float> data;

        private int topic_index;
        //private int color;

        public Float min = null;
        public Float max = null;


        public long actual_timestamp;
        public long aggregation_period;
        public int period_type;

        public GraphLine(int topic_index) {
            data = new ArrayList<Float>();
            this.topic_index = topic_index;
        }

        Integer getTopicIndex() {
            return this.topic_index;
        }

        public void setData(int index, Float value) {
            while (data.size() < index + 1) {
                data.add(null);
            }
            data.set(index, value);
        }

        public Float getData(int index) {
            return data.get(index);
        }

        public float getMin() {
            Float res = null;
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) == null) continue;
                res = (res == null ? data.get(i) : Math.min(res, data.get(i)));
            }
            return res == null ? 0 : res;
        }

        public float getMax() {
            Float res = null;
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) == null) continue;
                res = (res == null ? data.get(i) : Math.max(res, data.get(i)));
            }
            return res == null ? 1 : res;
        }

        public int getSize() {
            return data.size();
        }
    }

    private ArrayList<GraphLine> graphs = new ArrayList<>();

    final static int hour_in_ms = 3600;
    final static int minute_in_ms = 60;
    final static int sec_in_ms = 1;

    public static final int LIVE = 0;
    public static final int WITHOUT_HISTORY = 1;

    public static final int PERIOD_TYPE_1_HOUR = 2;
    public static final int PERIOD_TYPE_4_HOUR = 3;
    public static final int PERIOD_TYPE_1_DAY = 4;
    public static final int PERIOD_TYPE_1_WEEK = 5;
    public static final int PERIOD_TYPE_1_MOUNT = 6;

    public final static String[] period_names = {"Live", "Accum.", "1h", "4h", "1d", "1w", "1m"};

    public static final int[] aggregationPeriod = {sec_in_ms, -1, minute_in_ms * 2, minute_in_ms * 10, hour_in_ms / 2, hour_in_ms * 6, hour_in_ms * 24};
    private static final int[] periods_count = {60, -1, 30, 24, 24 * 2, 28, 32};
    public static final int[] time_step_divs = {10, -1, 5, 3, 3 * 2, 4, 4}; // как редко выводить временные риски
    public static final int[] time_step_multi_lines = {1, -1, 2, 3, 3, 2, 4}; // доп риски

    public static int getPeriodCount(int type) {
        return periods_count[type] + time_step_divs[type];
    }

    void nextPeriodType() {
        if (mode < PERIOD_TYPE_1_HOUR) return;
        if (mode == PERIOD_TYPE_1_MOUNT) return;
        mode++;
        WidgetData widgetData = (WidgetData) this.getTag();
        widgetData.mode = mode;
        MainActivity.presenter.requestRefreshGraphData(widgetData);
    }

    void previousPeriodType() {
        if (mode < PERIOD_TYPE_1_HOUR) return;
        if (mode == PERIOD_TYPE_1_HOUR) return;
        mode--;
        WidgetData widgetData = (WidgetData) this.getTag();
        widgetData.mode = mode;
        MainActivity.presenter.requestRefreshGraphData(widgetData);
    }

    void nextSubMode() {
        submode = submode == 0 ? 1 : 0;
        WidgetData widgetData = (WidgetData) this.getTag();
        widgetData.submode = submode;
    }

    void onDrawSimpleMode(Canvas canvas) {
        if (graphs.size() == 0) return;

        int total_max_graph_density = 0;
        for (GraphLine line : graphs) {
            total_max_graph_density = Math.max(line.getSize(), total_max_graph_density);
            //Log.d("servermode", "dots=" + line.getSize());
        }

        float density_multi = getResources().getDisplayMetrics().density;
        float textSize = 10 * density_multi;

        int left = getLeft();
        int right = getRight();
        int top = getTop();
        int bottom = getBottom();

        int graph_with = right - left - 32;
        int graph_height = bottom - top - 48;
        int graph_x_disp = 16;
        int graph_y_disp = 32;


        //int graph_density=16;
        float x_step = graph_with / (total_max_graph_density - 1);

        int graph_x_grid_count = 5;//total_max_graph_density;
        float x_grid_step = graph_with / (graph_x_grid_count - 1);

        int graph_y_grid_count = (int) (graph_height / (density_multi * 12));
        int y_grid_step = graph_height / (graph_y_grid_count - 1);


        primary_paint.setStrokeWidth(1);


        //GraphLine line=new GraphLine();

        Float min = null;
        Float max = null;
        for (GraphLine line : graphs) {
            min = (min == null ? line.getMin() : Math.min(min, line.getMin()));
            max = (max == null ? line.getMax() : Math.max(max, line.getMax()));
        }
        //округляем
        if (max - min < 2) {
            max = (float) Math.floor(max * 2) / 2 + 0.5f;
            min = (float) Math.ceil(min * 2) / 2 - 0.5f;
        } else {
            max = (float) Math.floor(max + 1);
            min = (float) Math.ceil(min - 1);
        }

        primary_paint.setTextSize(textSize);
        primary_paint.setColor(MyColors.getAsBlack());

        for (int i = 0; i < graph_x_grid_count; i++) {
            float x = graph_with - i * x_grid_step + graph_x_disp;
            canvas.drawLine(x, graph_y_disp, x, graph_height + graph_y_disp, primary_paint);
        }
        for (int i = 0; i < graph_y_grid_count; i++) {
            int y = i * y_grid_step + graph_y_disp;
            canvas.drawLine(graph_x_disp, y, graph_x_disp + graph_with, y, primary_paint);
        }


        primary_paint.setStrokeWidth(2 * density_multi);

        int dot_radius = graph_height / 18;

        for (GraphLine line : graphs) {
            primary_paint.setColor(colors[line.getTopicIndex()]);//line.getColor());
            for (int i = 0; i < (line.getSize() - 1); i++) {
                if ((line.getData(i) == null) || (line.getData(i + 1) == null)) continue;
                int x = graph_with - (int) (i * x_step) + graph_x_disp;
                int y = calcScreenY(line.getData(i), min, max, graph_height) + graph_y_disp;
                int y2 = calcScreenY(line.getData(i + 1), min, max, graph_height) + graph_y_disp;

                canvas.drawLine(x, y, x - x_step, y2, primary_paint);

            }
        }

        primary_paint.setColor(MyColors.getAsBlack());
        for (int i = 0; i < graph_y_grid_count; i++) {
            int y = i * y_grid_step + graph_y_disp;
            if (min != null && max != null) {
                Float y_label = (float) Math.round((min + i * (max - min) / (graph_y_grid_count - 1)) * 100) / 100;
                String text = String.valueOf(y_label);
                canvas.drawText(text, graph_x_disp, graph_height - i * y_grid_step - y_grid_step * 0.05f + graph_y_disp, primary_paint);
            }
        }
    }

    public class Splayn {

        public SplineTuple[] splines; // Сплайн


        // Построение сплайна
        // x - узлы сетки, должны быть упорядочены по возрастанию, кратные узлы запрещены
        // y - значения функции в узлах сетки
        // n - количество узлов сетки
        public void BuildSpline(double[] x, double[] y, int n) {
            // Инициализация массива сплайнов
            splines = new SplineTuple[n];
            for (int i = 0; i < n; i++) {
                splines[i] = new SplineTuple();
            }

            for (int i = 0; i < n; ++i) {
                splines[i].x = x[i];
                splines[i].a = y[i];
            }
            splines[0].c = splines[n - 1].c = 0.0;

            // Решение СЛАУ относительно коэффициентов сплайнов c[i] методом прогонки для трехдиагональных матриц
            // Вычисление прогоночных коэффициентов - прямой ход метода прогонки
            double[] alpha = new double[n - 1];
            double[] beta = new double[n - 1];
            alpha[0] = beta[0] = 0.0;
            for (int i = 1; i < n - 1; ++i) {
                double h_i = x[i] - x[i - 1], h_i1 = x[i + 1] - x[i];
                double A = h_i;
                double C = 2.0 * (h_i + h_i1);
                double B = h_i1;
                double F = 6.0 * ((y[i + 1] - y[i]) / h_i1 - (y[i] - y[i - 1]) / h_i);
                double z = (A * alpha[i - 1] + C);
                alpha[i] = -B / z;
                beta[i] = (F - A * beta[i - 1]) / z;
            }

            // Нахождение решения - обратный ход метода прогонки
            for (int i = n - 2; i > 0; --i)
                splines[i].c = alpha[i] * splines[i + 1].c + beta[i];

            // Освобождение памяти, занимаемой прогоночными коэффициентами
            beta = null;
            alpha = null;

            // По известным коэффициентам c[i] находим значения b[i] и d[i]
            for (int i = n - 1; i > 0; --i) {
                double h_i = x[i] - x[i - 1];
                splines[i].d = (splines[i].c - splines[i - 1].c) / h_i;
                splines[i].b = h_i * (2.0 * splines[i].c + splines[i - 1].c) / 6.0 + (y[i] - y[i - 1]) / h_i;
            }
        }

        // Вычисление значения интерполированной функции в произвольной точке
        public double f(double x) {

            SplineTuple s;
            int n = splines.length;
            //BuildSpline(myx,y,n);
            if (x <= splines[0].x) // Если x меньше точки сетки x[0] - пользуемся первым эл-тов массива
                s = splines[1];
            else if (x >= splines[n - 1].x) // Если x больше точки сетки x[n - 1] - пользуемся последним эл-том массива
                s = splines[n - 1];
            else // Иначе x лежит между граничными точками сетки - производим бинарный поиск нужного эл-та массива
            {
                int i = 0, j = n - 1;
                while (i + 1 < j) {
                    int k = i + (j - i) / 2;
                    if (x <= splines[k].x)
                        j = k;
                    else
                        i = k;
                }
                s = splines[j];
            }

            double dx = (x - s.x);
            // Вычисляем значение сплайна в заданной точке по схеме Горнера (в принципе, "умный" компилятор применил бы схему Горнера сам, но ведь не все так умны, как кажутся)
            return s.a + (s.b + (s.c / 2.0 + s.d * dx / 6.0) * dx) * dx;
        }

    }

    public class SplineTuple {
        public double a, b, c, d, x;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        if (mode == WITHOUT_HISTORY) {
            onDrawSimpleMode(canvas);
        } else {
            onDrawHistoryMode(canvas);
        }

    }

    private int submode = 0;

    void onDrawHistoryMode(Canvas canvas) {

        if (graphs.size() == 0) return;

        float density_multi = getResources().getDisplayMetrics().density;
        float textSize = 10 * density_multi;

        int total_max_graph_density = 0;
        for (GraphLine line : graphs) {
            if (line.period_type != mode) continue;
            total_max_graph_density = Math.max(line.getSize(), total_max_graph_density);
            //Log.d("servermode", "dots=" + line.getSize());
        }


        int left = getLeft();
        int right = getRight();
        int top = getTop();
        int bottom = getBottom();

        int graph_with = right - left; //-80
        int graph_height = bottom - top - (26 * (int) density_multi);// - 64;
        int graph_x_disp = 0;//40
        int graph_y_disp = (13 * (int) density_multi);

        int time_step_div = time_step_divs[mode];

        float x_step = (float) graph_with / (total_max_graph_density - time_step_div);


        int graph_x_grid_count = total_max_graph_density / time_step_div;
        float x_grid_step = (float) graph_with / (graph_x_grid_count - 1);

        int graph_y_grid_count = (int) (graph_height / (density_multi * 16));
        float y_grid_step = graph_height / (graph_y_grid_count - 1);

        primary_paint.setStrokeWidth(1);

        GraphLine firsGraph = null;
        Float min = null;
        Float max = null;
        for (GraphLine line : graphs) {
            if (line.period_type != mode) continue;
            if (firsGraph == null) firsGraph = line;

            if (submode != 0) {
                line.min = (line.min == null ? line.getMin() : Math.min(line.min, line.getMin()));
                line.max = (line.max == null ? line.getMax() : Math.max(line.max, line.getMax()));
            } else {
                min = (min == null ? line.getMin() : Math.min(min, line.getMin()));
                max = (max == null ? line.getMax() : Math.max(max, line.getMax()));
            }
        }

        if ((max == null) || (min == null)) return;

        //округляем
        if (submode == 0) {
            if (max - min <= graph_y_grid_count) {
                //max = (float) Math.floor(max);
                //min = (float) Math.floor(min);
            } else {

                // max = (float) Math.floor( max );
                min = (float) Math.floor(min);

                int ystep = (int) Math.ceil((max - min) / (graph_y_grid_count - 1));
                max = min + ystep * (graph_y_grid_count - 1);

            }
        }


        primary_paint.setTextSize(textSize);
        primary_paint.setColor(MyColors.getAsBlack());

        //из первой попавшейся
        Long actual_timestamp = firsGraph.actual_timestamp;
        Long aggregation_period = firsGraph.aggregation_period;

        //шкала времени
        Date dot_date = new Date(actual_timestamp * 1000);
        SimpleDateFormat format1;
        if (mode == LIVE) {
            format1 = new SimpleDateFormat("mm:ss");
        } else if (mode < PERIOD_TYPE_1_WEEK) {
            format1 = new SimpleDateFormat("HH:mm");
        } else {
            format1 = new SimpleDateFormat("dd HH:mm");
        }
        for (int i = 0; i < graph_x_grid_count; i++) {
            float x = graph_with - (float) i * x_grid_step + graph_x_disp;
            //canvas.drawLine(x, graph_y_disp, x, graph_height + graph_y_disp, primary_paint);

            String time_label = format1.format(dot_date);
            primary_paint.getTextBounds(time_label, 0, time_label.length(), bounds);

            if (i != 0 && i != graph_x_grid_count - 1) {//не выводим первую и последнюю метку даты
                canvas.drawText(time_label, x - (bounds.right - bounds.left) / 2 - 1, graph_y_disp - 3, primary_paint);
            }

            dot_date.setTime(dot_date.getTime() - aggregation_period * time_step_div * 1000);
        }

        float time_step_multi_line = time_step_multi_lines[mode];
        for (int i = 0; i < graph_x_grid_count * time_step_multi_line; i++) {
            Paint pain = (i % time_step_multi_line) == 0 ? primary_paint : secondary_paint;
            float x = graph_with - (float) i * x_grid_step / time_step_multi_line + graph_x_disp;
            canvas.drawLine(x, graph_y_disp, x, graph_height + graph_y_disp, pain);
        }


        //горизонтальные полосы
        for (int i = 0; i < graph_y_grid_count; i++) {
            float y = (float) i * y_grid_step + graph_y_disp;
            canvas.drawLine(graph_x_disp, y, graph_x_disp + graph_with, y, primary_paint);
        }


        graph_paint.setStrokeWidth(1 * density_multi);
        graph_paint.setStyle(Paint.Style.STROKE);
        graph_paint.setPathEffect(new CornerPathEffect(16));
        //primary_paint.setPathEffect(new DiscretePathEffect(10,15));

        for (GraphLine line : graphs) {

            if (line.period_type != mode) continue;

            graph_paint.setColor(colors[line.getTopicIndex()]);
            //primary_paint.setAlpha(127);

            Splayn splayn = new Splayn();
            double[] xdata = new double[line.getSize()];
            double[] ydata = new double[line.getSize()];
            for (int i = 0; i < line.getSize(); i++) {
                xdata[i] = i;
                ydata[i] = line.getData(i);
            }
            splayn.BuildSpline(xdata, ydata, line.getSize());

            Path path = new Path();
            float mstep = 1f;
            //path.moveTo(0, graph_height+graph_y_disp);
            //path.lineTo(graph_with, graph_height+graph_y_disp);

            path.moveTo(graph_with+50, graph_height/2);

            //path.lineTo(graph_with, graph_height/2);

            for (float i = 0; i < line.getSize(); i += mstep) {
                //if ((line.getData(i) == null) || (line.getData(i + 1) == null)) continue;
                int x = graph_x_disp + (int) (graph_with - ((float) i * x_step)) - 4;
                int y = calcScreenY((float) splayn.f(i), submode != 0 ? line.min : min, submode != 0 ? line.max : max, graph_height) + graph_y_disp;
                path.lineTo(x, y);
            }
            path.lineTo(-100, graph_height+graph_y_disp);
            path.lineTo(graph_with, graph_height+graph_y_disp);

            canvas.drawPath(path, graph_paint);

            graph_paint.setAlpha(63);
            graph_paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, graph_paint);

        }


        //primary_paint.setStyle(Paint.Style.FILL);

        //шкала значений
        int val_lavel_x = 0;
        for (GraphLine line : graphs) {
            if (line.period_type != mode) continue;

            if (submode != 0) {
                primary_paint.setColor(colors[line.getTopicIndex()]);
            } else {
                primary_paint.setColor(MyColors.getAsBlack());
            }


            for (int i = 0; i < graph_y_grid_count - 1; i++) {
                float y = (float) graph_height - i * y_grid_step + graph_y_disp - 8;//2 - отступ текста от полоски

                if ((submode != 0 ? line.min : min) != null && (submode != 0 ? line.max : max) != null) {
                    Float y_label = (float) Math.round(((submode != 0 ? line.min : min) + i * ((submode != 0 ? line.max : max) - (submode != 0 ? line.min : min)) / (graph_y_grid_count - 1)) * 100) / 100;
                    String text = String.valueOf(y_label);
                    //canvas.drawText(text, graph_x_disp, graph_height - i * y_grid_step - y_grid_step * 0.05f + graph_y_disp, primary_paint);
                    canvas.drawText(text, val_lavel_x + graph_x_disp, y, primary_paint);
                }
            }
            if (submode == 0) break;

            val_lavel_x = val_lavel_x + 100;
        }


        //режим периода
        primary_paint.setColor(MyColors.getAsBlack());
        primary_paint.setTextSize(textSize);
        canvas.drawText(period_names[mode], 0, graph_y_disp - 3, primary_paint);

        //легенда
        int y_legend_disp = 10;
        float legend_x_step = graph_with / 4;
        float xpos = 0;
        int text_label_height = (bounds.bottom - bounds.top);
        for (int i = 0; i < 4; i++) {
            String topic = getName(i);
            if (topic == null || topic.isEmpty()) continue;
            primary_paint.setColor(colors[i]);
            //float xpos=i*legend_x_step;
            float ypos = graph_y_disp + graph_height + text_label_height / 2 + text_label_height * 0.3f;
            canvas.drawLine(xpos, ypos, xpos + 30, ypos, graph_paint);
            primary_paint.setColor(MyColors.getAsBlack());
            canvas.drawText(names[i], xpos + 40, graph_y_disp + graph_height + text_label_height + text_label_height * 0.3f, primary_paint);
            xpos += legend_x_step;
        }


    }

}

package com.chartiq.chartiqsample.studies;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import com.chartiq.sdk.model.Study;
import com.chartiq.chartiqsample.ColorAdapter;
import com.chartiq.chartiqsample.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StudyOptionsActivity extends AppCompatActivity {

    private static final int RESULT_STUDY_REMOVED = 4;

    TextView studyTitle;
    Toolbar toolbar;
    Study study;
    HashMap<String, Object> defaultInputs = new HashMap<>();
    HashMap<String, Object> defaultOutputs = new HashMap<>();
    LinearLayout optionsLayout;
    private PopupWindow colorPalette;
    private RecyclerView colorRecycler;
    private TextView currentColorView;
    private String colorOptionName;
    private StudyParameter[] inputs;
    private StudyParameter[] outputs;
    private StudyParameter[] parameters;
    private TextView selectView;

    HashMap<String, String> studyParameterColors = new HashMap<>();
    HashMap<String, String> studyParameterValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_options);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        studyTitle = (TextView) findViewById(R.id.study_title);
        optionsLayout = (LinearLayout) findViewById(R.id.options);

        // map.put("Show Zones", "studyOverZones");
        studyParameterColors.put("OverBought", "studyOverBoughtColor");
        studyParameterColors.put("OverSold", "studyOverSoldColor");
        studyParameterValues.put("OverBought", "studyOverBoughtValue");
        studyParameterValues.put("OverSold", "studyOverSoldValue");
        //map.put("""studyOverZonesEnabled")

        if (getIntent().hasExtra("study")) {
            study = (Study) getIntent().getSerializableExtra("study");
            studyTitle.setText(study.name);
            if (study.inputs != null) {
                defaultInputs = new HashMap<>(study.inputs);
            }
            if (study.outputs != null) {
                defaultOutputs = new HashMap<>(study.outputs);
            }
        }

        if (getIntent().hasExtra("inputs")) {
            try {
                inputs = new Gson().fromJson(getIntent().getStringExtra("inputs"), StudyParameter[].class);
            } catch(Exception exception){
                exception.printStackTrace();

            }
            if (study.inputs != null) {
                bindStudyOptions(inputs, study.inputs);
            }
        }

        if (getIntent().hasExtra("outputs")) {
            try {
                outputs = new Gson().fromJson(getIntent().getStringExtra("outputs"), StudyParameter[].class);
            } catch(Exception exception){
                exception.printStackTrace();

            }
            if (study.outputs != null || outputs != null) {
                bindStudyOptions(outputs, study.outputs);
            }
        }

        if (getIntent().hasExtra("parameters")) {
            try {
                parameters = new Gson().fromJson(getIntent().getStringExtra("parameters"), StudyParameter[].class);
            } catch(Exception exception){
                exception.printStackTrace();
            }
            if(study.parameters != null){
                bindStudyOptions(parameters, study.parameters);
            }
        }

        colorPalette = new PopupWindow(this);
        colorPalette.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        colorPalette.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        colorPalette.setContentView(getLayoutInflater().inflate(R.layout.color_palette, null));
        colorRecycler = (RecyclerView) colorPalette.getContentView().findViewById(R.id.recycler);
        colorRecycler.setLayoutManager(new GridLayoutManager(this, 5));
        colorRecycler.setAdapter(new ColorAdapter(this, R.array.colors, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setColor(v);
            }
        }));
    }

    private void showColorPalette(View view) {
        if (colorPalette.isShowing() && currentColorView == view) {
            currentColorView = null;
            colorPalette.dismiss();
        } else {
            colorPalette.dismiss();
            currentColorView = (TextView) view;
            int[] coord = {0, 0};
            view.getLocationOnScreen(coord);
            colorPalette.showAtLocation(view, Gravity.NO_GRAVITY, 0, coord[1]);
//            colorPalette.showAtLocation(view, Gravity.CENTER, 0, 0);
        }
    }

    private void setColor(View view) {
        colorPalette.dismiss();
        currentColorView.setBackgroundColor(Color.parseColor(String.valueOf(view.getTag())));
        String parameterValue = studyParameterColors.get(colorOptionName);
        if(parameterValue != null) { // parameter entry, need to drill down to properly set the value
            Map<String, Object> oldParameters = (Map<String, Object>) study.parameters.get("init");
            if(oldParameters == null) {
                oldParameters = study.parameters;
            }
            for (Map.Entry<String,Object> entry : oldParameters.entrySet()) {
                if (entry.getKey().equals(parameterValue)) {
                    //entry.setValue(String.valueOf(view.getTag()));
                    String value = String.valueOf(view.getTag());
                    oldParameters.put(entry.getKey(), value);
                    break;
                }
            }
            //study.parameters.put("init", oldParameters);
            study.parameters = oldParameters;
            study.modifiedParameters = oldParameters;
        } else if (study.outputs != null) {
            study.outputs.put(colorOptionName, String.valueOf(view.getTag()));
        }
    }

    private void bindStudyOptions(StudyParameter[] array, final Map<String, Object> studyParams) {
        for (final StudyParameter parameter : array) {
            String heading = parameter.heading;
            boolean isParameterValue = false;
            if(studyParameterColors.get(heading) != null || studyParameterValues.get(heading) != null) {
                isParameterValue = true;
            }
            if(isParameterValue) {
                String parameterValue = studyParameterColors.get(heading);
                HashMap<String, Object> oldParameters = (HashMap<String, Object>) study.parameters;
                for (Map.Entry<String,Object> entry : oldParameters.entrySet()) {
                    if (entry.getKey().equals(parameterValue)) {
                        parameter.color = String.valueOf(entry.getValue());
                        break;
                    }
                }
                bindColor(parameter);
            } else if (parameter.color != null || parameter.defaultOutput != null || parameter.defaultColor != null) {
                // get the color from the client-side Study object
                if (studyParams != null && studyParams.containsKey(parameter.name)) {
                    parameter.color = String.valueOf(studyParams.get(parameter.name));
                }
                // get the color from the study definition
                else if(parameter.color != null) {
                    parameter.color = String.valueOf(parameter.color);
                }
                // get the color from the study descriptor default value
                else if(parameter.defaultOutput != null) {
                    parameter.color = String.valueOf(parameter.defaultOutput);
                }
                // parameters have a defaultColor field
                else if(parameter.defaultColor != null) {
                    parameter.color = String.valueOf(parameter.defaultColor);
                }
                bindColor(parameter);
            }

            if (parameter.type != null) {
                switch (parameter.type) {
                    case "select":

                        if (studyParams.containsKey(parameter.name) && !"field".equals(studyParams.get(parameter.name))) {
                            parameter.value = studyParams.get(parameter.name);
                            if(parameter.value.getClass() == ArrayList.class){
                                ArrayList<String> test = (ArrayList<String>) parameter.value;
                                parameter.value = test.get(0);
                            }
                        }
                        bindSelect(parameter);
                        break;
                    case "number":
                        if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindNumber(studyParams, parameter);
                        break;
                    case "text":
                        if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindString(studyParams, parameter);
                        break;
                    case "checkbox":
                        if (studyParams.containsKey(parameter.name)) {
                            parameter.value = studyParams.get(parameter.name);
                        }
                        bindBoolean(studyParams, parameter);
                        break;
                }
            }
        }
    }

    private void bindSelect(final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.select_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final TextView textView = (TextView) v.findViewById(R.id.value);
        selectView = textView;
        textView.setText(String.valueOf(parameter.value));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudyOptionsActivity.this, StudySelectOptionActivity.class);
                intent.putExtra("parameter", parameter);
                startActivityForResult(intent, 0);
            }
        });
    }

    private void bindBoolean(final Map<String, Object> studyParams, final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.boolean_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final Switch switchView = (Switch) v.findViewById(R.id.value);
        switchView.setChecked(Boolean.valueOf(String.valueOf(parameter.value)));
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                studyParams.put(parameter.name, switchView.isChecked());
            }
        });
    }

    private void bindString(final Map<String, Object> studyParams, final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.edittext_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final EditText editText = (EditText) v.findViewById(R.id.value);
        editText.setText(String.valueOf(parameter.value));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String parameterValueName = studyParameterValues.get(parameter.name);
                String fieldName = parameterValueName != null ? parameterValueName : parameter.name;

                studyParams.put(fieldName, editText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void bindColor(final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.color_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final TextView color = (TextView) v.findViewById(R.id.value);
        color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPalette(v);
                colorOptionName = parameter.heading;
            }
        });
        if ("auto".equals(parameter.color)) {
            color.setBackgroundColor(Color.BLACK);
        } else if(parameter.color != null){
            color.setBackgroundColor(Color.parseColor(parameter.color));
        } else if(parameter.defaultColor != null) {
            if("auto".equals(parameter.defaultColor)) {
                color.setBackgroundColor(Color.BLACK);
            } else {
                color.setBackgroundColor(Color.parseColor(String.valueOf(parameter.defaultColor)));
            }
        } else {
            color.setBackgroundColor(Color.BLACK);
        }
    }

    private void bindNumber(final Map<String, Object> studyParams, final StudyParameter parameter) {
        View v = getLayoutInflater().inflate(R.layout.number_study_option, null);
        optionsLayout.addView(v);
        TextView optionName = (TextView) v.findViewById(R.id.option_name);
        optionName.setText(parameter.heading);
        final EditText editText = (EditText) v.findViewById(R.id.value);
        editText.setText(String.valueOf(parameter.value));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                studyParams.put(parameter.name, editText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void applyChanges(View view) {
        Intent result = new Intent();
        result.putExtra("study", study);
        setResult(RESULT_OK, result);
        finish();
    }

    public void resetToDefaults(View view) {
        study.inputs = new HashMap<>(defaultInputs);
        study.outputs = new HashMap<>(defaultOutputs);
        optionsLayout.removeAllViews();
        if (inputs != null) {
            bindStudyOptions(inputs, study.inputs);
        }
        if (outputs != null) {
            bindStudyOptions(outputs, study.outputs);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void removeStudy(View view) {
        Intent result = new Intent();
        result.putExtra("study", study);
        setResult(RESULT_STUDY_REMOVED, result);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (data.hasExtra("chosenValue") && data.hasExtra("parameter")) {
                StudyParameter parameter = (StudyParameter) data.getSerializableExtra("parameter");
                String value = data.getStringExtra("chosenValue");
                if (parameter.defaultInput != null) {
                    study.inputs.put(parameter.name, value);
                } else {
                    study.outputs.put(parameter.name, value);
                }
                selectView.setText(value);
            }
        }
    }
}

package com.zerodaedalus.www.clock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private boolean isClockRunning = false;
    private boolean isBreakRunning = false;
    private long clockCount;
    private long breakCount;
    Chronometer timerClock;
    Chronometer timerBreak;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    Snackbar snackbar;
    MyViewModel model;
    Button createProject;
    RadioGroup projectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Startup code
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        setActiveProject(sharedPref.getString("current_project", "No active project"));

        initializeTimerClock();
        initializeTimerBreak();

        Button toggleBreak = (Button) findViewById(R.id.toggleBreak);
        projectList = (RadioGroup) findViewById(R.id.projectList);

        projectList.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                setActiveProject(radioButton.getText().toString());
            }
        });

        rebuildProjectList();

        createProject = (Button) findViewById(R.id.createProject);
        createProject.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CreateProjectActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String newProject = intent.getStringExtra(CreateProjectActivity.EXTRA_PROJECT_NAME);
        if (null != newProject) {
            Toast.makeText(MainActivity.this, String.format("New Project Created: %s", newProject), Toast.LENGTH_SHORT).show();
            Set<String> projects = sharedPref.getStringSet("project_list", new HashSet<String>());
            projects.add(newProject);
            editor.putStringSet("project_list", projects);
            editor.commit();
            rebuildProjectList();
            setActiveProject(newProject);
        }
    }

    public void rebuildProjectList() {
        Set<String> list = sharedPref.getStringSet("project_list", new HashSet<String>());
        String[] arr = list.toArray(new String[0]);

        projectList.removeAllViews();
        for (int i = 0; i < list.size(); i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(i + 1000);
            radioButton.setText(arr[i]);
            radioButton.setOnLongClickListener(new RadioButton.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    removeProject(v);
                    return true;
                }
            });

            projectList.addView(radioButton);
        }
    }

    public void removeProject(View view) {
        RadioButton button = findViewById(view.getId());
        Set<String> list = sharedPref.getStringSet("project_list", new HashSet<String>());
        Boolean changed = list.remove(button.getText().toString());
        editor.putStringSet("project_list", list);
        editor.commit();
        rebuildProjectList();
    }

    public void setActiveProject(String projectName) {
        editor.putString("current_project", projectName);
        editor.commit();

        TextView projectNameDisplay = (TextView) findViewById(R.id.projectName);
        projectNameDisplay.setText(String.format(getResources().getString(R.string.project_display), projectName));
    }

    public void toggleBreak(View view) {
        if(!isClockRunning) {
            return;
        }

        timerClock.setText(String.format("Elapsed: \n%s", convertTime(clockCount)));
        timerBreak.setText(String.format("Time on Break: \n%s", convertTime(breakCount)));


        if(isBreakRunning) {
            timerBreak.stop();
            timerClock.start();
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.break_ended, Snackbar.LENGTH_SHORT).show();
            } else {
            timerBreak.start();
            timerClock.stop();

            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.break_started, Snackbar.LENGTH_SHORT).show();
        }
        isBreakRunning = !isBreakRunning;
    }

    public void toggleClock(View view) {
        Button toggleClock = (Button) findViewById(R.id.toggleClock);

        if (isBreakRunning) {
            return;
        }

        if (!isClockRunning) {
            clockCount = 0;
            breakCount = 0;

            timerClock.start();

            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.clock_started, Snackbar.LENGTH_SHORT).show();

            timerBreak.setText("");

            timerClock.setTextColor(getResources().getColor(R.color.colorWarning, getTheme()));
        } else {
            // Clock out if clock is running.
            timerClock.stop();
            timerBreak.stop();

            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.clock_ended, Snackbar.LENGTH_SHORT).show();

            timerBreak.setText(String.format("Total Break: \n%s", convertTime(breakCount)));
            timerClock.setText(String.format("Completed: \n%s", convertTime(clockCount)));

            timerClock.setTextColor(getResources().getColor(R.color.colorBlack, getTheme()));
        }
        isClockRunning = !isClockRunning;
    }

    private String convertTime(long time) {
        int h = (int)(time / 3600);
        int m = (int)(time - h * 3600) / 60;
        int s = (int)(time - h * 3600 - m * 60);
        String t = String.format("%02d hr : %02d min : %02d sec", h, m, s);
        return t;
    }

    private void initializeTimerClock() {
        timerClock = (Chronometer) findViewById(R.id.timerClock);
        timerClock.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener(){
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chronometer.setText(String.format("Elapsed: \n%s", convertTime(clockCount)));
                clockCount++;
            }
        });
        timerClock.setText("");
    }

    private void initializeTimerBreak() {
        timerBreak = (Chronometer) findViewById(R.id.timerBreak);
        timerBreak.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener(){
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chronometer.setText(String.format("Time on Break: \n%s", convertTime(breakCount)));
                breakCount++;
            }
        });
        timerBreak.setText("");
    }
}

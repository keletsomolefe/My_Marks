package com.software.portal.mymarks;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private String XML;
    private String userid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        XML = intent.getStringExtra("XML");
        UPPortal portal = new UPPortal();

        ArrayList<String[]> marks = portal.processXML(XML);

        LinearLayout layout = (LinearLayout) findViewById(R.id.mark_display);

        for (int i = 0; i < marks.size(); i++) {
            View mark_view = LayoutInflater.from(this).inflate(R.layout.mark_view, layout, false);

            TextView textSubject = (TextView) mark_view.findViewById(R.id.textSubject);
            TextView textMark = (TextView) mark_view.findViewById(R.id.textMark);

            textSubject.setText(marks.get(i)[0]);
            if (marks.get(i)[5].equals("Grade Outstanding")) {
                textMark.setText("XX");
            } else {
                textMark.setText(marks.get(i)[4]);
            }

            layout.addView(mark_view);
        }
    }
}

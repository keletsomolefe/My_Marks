package com.software.portal.mymarks;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

        LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
        TextView subject;

        for (int i = 0; i < marks.size(); i++) {
            subject = new TextView(layout.getContext());
            if (marks.get(i)[5].equals("Grade Outstanding")) {
                subject.setText(marks.get(i)[0] + "   [" + marks.get(i)[5] + "]");
            } else {
                subject.setText(marks.get(i)[0] + "   [" + marks.get(i)[4] + "]");
            }
            if (Build.VERSION.SDK_INT >= 23) {
                subject.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
            } else {
                subject.setTextAppearance(layout.getContext(), android.R.style.TextAppearance_Material_Medium);
            }

            layout.addView(subject);
        }



    }
}

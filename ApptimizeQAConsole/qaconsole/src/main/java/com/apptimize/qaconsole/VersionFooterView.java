package com.apptimize.qaconsole;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apptimize.Apptimize;

public class VersionFooterView extends LinearLayout {

    public VersionFooterView(Context context) {
        super(context);
        initView();
    }

    public VersionFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VersionFooterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public VersionFooterView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.apptimize_versions_footer, this);

        TextView apptimizeVersion = findViewById(R.id.apptimize_version_text);
        apptimizeVersion.setText(Apptimize.getVersion());

        TextView qaConsoleVersion = findViewById(R.id.qaconsole_version_text);
        qaConsoleVersion.setText(Version.VERSION);
    }
}

package com.apptimize.androidqaconsole;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.apptimize.Apptimize;
import com.apptimize.ApptimizeInstantUpdateOrWinnerInfo;
import com.apptimize.ApptimizeOptions;
import com.apptimize.qaconsole.QAConsole;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainActivity extends Activity {

    private class ListContent {
        public String title;
        public String detail;
        public ListContent(String title, String detail)
        {
            this.title = title;
            this.detail = detail;
        }
    }

    public class ListContentAdapter extends ArrayAdapter<ListContent> {
        public ListContentAdapter(Context context, AbstractList<ListContent> content) {
            super(context, 0, content);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListContent content = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.title_detail_template, parent, false);
            }

            TextView tvName = (TextView) convertView.findViewById(R.id.title);
            TextView tvHome = (TextView) convertView.findViewById(R.id.detail);

            tvName.setText(content.title);
            tvHome.setText(content.detail);

            return convertView;
        }
    }

    private QAConsole qaConsole = null;
    private AbstractList<ListContent> enrollmentList = new ArrayList<ListContent>();
    private ListContentAdapter enrollmentListAdapter;
    private ListView enrollmentListView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enrollmentListAdapter = new ListContentAdapter(this, enrollmentList);

        enrollmentListView = (ListView) findViewById(R.id.enrollment_list);
        enrollmentListView.setAdapter(enrollmentListAdapter);

        Apptimize.addOnExperimentsProcessedListener(new Apptimize.OnExperimentsProcessedListener() {
            @Override
            public void onExperimentsProcessed() {
                refresh();
            }
        });

        // Set your AppKey here
        final String appKey = "YourApptimizeApplicationKey";
        final ApptimizeOptions options = new ApptimizeOptions();
        options.setForceVariantsShowWinnersAndInstantUpdates(true);

        Apptimize.setup(this, appKey, options);
        qaConsole = new QAConsole(this);

        refresh();
    }

    public void onRefreshClicked(View view) {
        refresh();
    }

    public void onShowConsole(View view) {
        qaConsole.isShakeGestureEnabled = false;
        qaConsole.launchQAConsole();
    }

    private void refresh() {
        Stream<ListContent> testStream = Apptimize.getTestInfo()
                .values()
                .stream()
                .map(test -> new ListContent(
                        test.getTestName(),
                        String.format("%s (%d)", test.getEnrolledVariantName(), test.getEnrolledVariantId())))
                .sorted(Comparator.comparing(item -> item.title.toLowerCase()));

        Stream<ListContent> winnerStream = Apptimize.getInstantUpdateOrWinnerInfo()
                .values()
                .stream()
                .map(test -> {
                            if (test.getType() == ApptimizeInstantUpdateOrWinnerInfo.Type.INSTANT_UPDATE) {
                                return new ListContent(
                                        test.getInstantUpdateName(),
                                        String.format("Instant Update: %d", test.getInstantUpdateId()));

                            } else {
                                return new ListContent(
                                        test.getWinningTestName(),
                                        String.format("Winner: %s (%d)", test.getWinningVariantName(), test.getWinningVariantId()));

                            }
                })
                .sorted(Comparator.comparing(item -> item.title.toLowerCase()));

        Stream<ListContent> combined = Stream.concat(testStream, winnerStream);

        this.enrollmentList.clear();
        this.enrollmentList.addAll(0, combined.collect(Collectors.toList()));
        this.enrollmentListAdapter.notifyDataSetChanged();
    }
}

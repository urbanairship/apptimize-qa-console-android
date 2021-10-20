package com.apptimize.qaconsole;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toolbar;

import com.apptimize.Apptimize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.apptimize.qaconsole.R.id;
import static com.apptimize.qaconsole.R.layout;

public class ApptimizeQaActivity extends Activity implements SearchView.OnQueryTextListener {
    private List<Experiment> dataModels;
    private ListView listView;
    private CustomAdapter adapter;
    private ProgressBar progressIndicator;
    private MenuItem menuSearch;
    private SearchView searchView;
    private Apptimize.MetadataStateChangedListener metadataStateChangedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.apptimize_activity_qa);
        
        listView = findViewById(id.listView);
        
        progressIndicator = findViewById(id.pbHeaderProgress);
        progressIndicator
                .getIndeterminateDrawable()
                .setColorFilter(0xFF007FFF, android.graphics.PorterDuff.Mode.MULTIPLY);

        if (getActionBar() == null) {
            Toolbar toolbar = findViewById(id.toolbar);
            toolbar.setVisibility(View.VISIBLE);
            setActionBar(toolbar);

            // inflate the toolbar manually
            toolbar.inflateMenu(R.menu.apptimize_menu);
        }

        configureListView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        makeAdapter();
        startMetadataStatusMonitoring();
    }

    @Override
    protected void onPause() {
        if (metadataStateChangedListener != null) {
            Apptimize.removeMetadataStateChangedListener(metadataStateChangedListener);
        }

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!item.equals(menuSearch)) {
            System.out.println ( "Clearing all forced variants!" );
            Apptimize.clearAllForcedVariants();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        QAConsole.qaActivityLaunched = false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.getFilter().filter(newText);
        
        return true;
    }

    private void startMetadataStatusMonitoring() {
        metadataStateChangedListener = createMetadataStateListener();
        Apptimize.addMetadataStateChangedListener(metadataStateChangedListener);
    }

    private void configureListView() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                resetSearch();
                menuSearch.collapseActionView();

                adapter.selectVariantAtPosition(position);
                setSelectedVariants(adapter.getAllCheckedVariants());
            }
        });
    }

    private void makeAdapter() {
        populateDataModel();
        adapter = new CustomAdapter(this, dataModels);
        listView.setAdapter(adapter);
        adapter.setTestInfo(Apptimize.getTestInfo());
    }

    private void resetSearch() {
        adapter.reset();
        
        if (searchView == null) {
            Log.e(getClass().getSimpleName(), "SearchView is null");
            return;
        }

        searchView.setQuery ("",false );
        searchView.clearFocus();
    }

    private void setSelectedVariants(Set<Long> selectedVariants) {
        Apptimize.clearAllForcedVariants();
        for (Long id : selectedVariants) {
            Apptimize.forceVariant(id);
        }
    }

    private void populateDataModel() {
        Map<Long, Map<String, Object>> myVariants = Apptimize.getVariants();   // note getVariants can return empty or null.

        List<Experiment> experiments = new ArrayList<>();

        for (Map<String, Object> source : myVariants.values()){
            Experiment experiment = new Experiment(source);
            int index = experiments.indexOf(experiment);
            if (index < 0) {
                experiments.add(experiment);
            } else {
                experiment = experiments.get(index);
            }

            experiment.addVariant(new Variant(source));
        }

        Collections.sort(experiments);
        dataModels = experiments;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.apptimize_menu, menu);

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        menuSearch = menu.findItem(R.id.search);
        searchView = (SearchView) menuSearch.getActionView();

        searchView.setSearchableInfo(searchManager.
                getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getResources ().getString ( R.string.apptimize_search_hint ));
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();

        menuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.requestFocus();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                resetSearch();
                return true;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener () {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }
            }
        });

        return true;
    }

    private Apptimize.MetadataStateChangedListener createMetadataStateListener() {
        return new Apptimize.MetadataStateChangedListener() {
            @Override
            public void onMetadataStateChanged(EnumSet<Apptimize.ApptimizeMetadataStateFlags> stateFlags) {
                if (stateFlags.contains(Apptimize.ApptimizeMetadataStateFlags.REFRESHING)) {
                    progressIndicator.setVisibility(View.VISIBLE);
                } else if (stateFlags.contains(Apptimize.ApptimizeMetadataStateFlags.UP_TO_DATE)) {
                    progressIndicator.setVisibility(View.INVISIBLE);

                    if (adapter != null) {
                        adapter.setTestInfo(Apptimize.getTestInfo());
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onApptimizeForegrounded(boolean willRefreshMetadata) {
            }
        };
    }
}

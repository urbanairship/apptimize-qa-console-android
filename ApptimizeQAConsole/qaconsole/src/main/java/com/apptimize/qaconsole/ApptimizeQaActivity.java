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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toolbar;

import com.apptimize.Apptimize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.apptimize.qaconsole.R.id;
import static com.apptimize.qaconsole.R.layout;

public class ApptimizeQaActivity extends Activity implements SearchView.OnQueryTextListener {
    private ExperimentsDataSource dataSource;
    private ListView listView;
    private CustomAdapter adapter;
    private ProgressBar progressIndicator;
    private MenuItem menuSearch;
    private SearchView searchView;
    private Apptimize.MetadataStateChangedListener metadataStateChangedListener;
    private DisplayMode displayMode = DisplayMode.EXPERIMENTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.apptimize_activity_qa);
        
        listView = findViewById(id.listView);

        configureDisplayModeSpinner();
        
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

    private void configureDisplayModeSpinner() {

        String[] items = new String[DisplayMode.values().length];
        for (int index = 0; index < DisplayMode.values().length; index++) {
            items[index] = DisplayMode.values()[index].toString();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner displayModeSpinner = findViewById(id.display_mode_spinner);
        displayModeSpinner.setAdapter(adapter);

        displayModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ApptimizeQaActivity.this.adapter.setDisplayMode(DisplayMode.values()[position]);
                resetSearch();
                menuSearch.collapseActionView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
        this.dataSource = new ExperimentsDataSource(Apptimize.getVariants(),
                Apptimize.getInstantUpdateOrWinnerInfo().values());
        adapter = new CustomAdapter(this, dataSource, displayMode);
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

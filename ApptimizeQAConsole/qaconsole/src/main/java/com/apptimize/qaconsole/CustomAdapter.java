package com.apptimize.qaconsole;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filterable;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.apptimize.ApptimizeInstantUpdateOrWinnerInfo;
import com.apptimize.ApptimizeTestInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

enum DisplayMode {
    EXPERIMENTS("Running Experiments"),
    FEATURE_FLAGS("Feature Flags"),
    WINNERS("Winners");

    private final String name;

    DisplayMode(String s) {
        name = s;
    }

    public String toString() {
        return this.name;
    }
}

public class CustomAdapter extends ArrayAdapter<ListViewModel> implements Filterable {
    private ExperimentsDataSource dataSource;
    private List<ListViewModel> dataSetFiltered;
    private CustomFilter customFilter;
    private DisplayMode displayMode;

    // View lookup cache
    private static class ViewHolder {
        TextView txtName;
        CheckBox checkBox;
        Switch featureSwitch;
        Boolean isHeader;
    }

    public CustomAdapter(Context context, ExperimentsDataSource dataSource, DisplayMode displayMode) {
        super(context, R.layout.apptimize_row_item, new ArrayList<ListViewModel>());
        this.dataSource = dataSource;
        this.displayMode = displayMode;
        reset();
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
        reset();
    }

    @Override
    public int getCount() {
        return dataSetFiltered.size();
    }

    @Override
    public ListViewModel getItem(int position) {
        return dataSetFiltered.get(position);
    }

    public void setTestInfo(Map<String, ApptimizeTestInfo> testInfo,
                            Map<String, ApptimizeInstantUpdateOrWinnerInfo> winners) {
        List<Experiment> experiments = dataSource.getAllExperiments();
        for (Experiment ex : experiments) {
            ApptimizeTestInfo info = testInfo.get(ex.name);
            long variantId = -1;
            if (info != null) {
                variantId = info.getEnrolledVariantId();
            } else {
                ApptimizeInstantUpdateOrWinnerInfo winnerInfo = winners.get(ex.name);
                if (winnerInfo != null && winnerInfo.getWinningVariantId() != null) {
                    variantId = winnerInfo.getWinningVariantId();
                }
            }

            ex.selectVariant(variantId);
        }
    }

    public Set<Long> getAllCheckedVariants() {
        List<Experiment> experiments = dataSource.getAllExperiments();

        Set<Long> result = new HashSet<>();
        for (Experiment ex : experiments) {
            Long checked = ex.getCheckedVariantId();
            if (checked != null) {
                result.add(checked);
            }
        }
        return result;
    }

    public Experiment getExperimentFor(int position) {
        List<Experiment> experiments = getItemsForDisplayMode();
        ListViewModel viewModel = getItem(position);
        Experiment result = null;
        for (Experiment ex : experiments) {
            if (ex.hasVariant(viewModel.id)) {
                result = ex;
                break;
            }
        }
        return result;
    }

    public Variant getVariantFor(int position) {
        ListViewModel viewModel = getItem(position);
        Experiment experiment = getExperimentFor(position);
        return experiment.variantWith(viewModel.id);
    }

    public void selectVariantAtPosition(int position) {
        ListViewModel viewModel = getItem(position);
        Experiment experiment = getExperimentFor(position);
        if (displayMode == DisplayMode.EXPERIMENTS) {
            experiment.selectVariant(viewModel.id);
        } else {
            Variant variant = experiment.variantWith(viewModel.id);
            variant.toggle();
        }
    }

    public void reset() {
        List<ListViewModel> models = new ArrayList<>();
        List<Experiment> experiments = getItemsForDisplayMode();
        for (Experiment ex : experiments) {
            models.addAll(ex.generateListViewItems(null));
        }
        this.dataSetFiltered = models;
        notifyDataSetChanged();
    }

    private List<Experiment> getItemsForDisplayMode() {
        List<Experiment> result;
        switch (this.displayMode) {
            case EXPERIMENTS:
                result = dataSource.getRunningExperiments();
                break;
            case FEATURE_FLAGS:
                result = dataSource.getFeatureFlags();
                break;
            case WINNERS:
                result = dataSource.getWinners();
                break;
            default:
                result = new ArrayList<Experiment>();
                break;
        }
        return result;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.apptimize_row_item, parent, false);
            viewHolder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
            viewHolder.featureSwitch = convertView.findViewById(R.id.feature_switch);
            viewHolder.isHeader = false;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ListViewModel viewModel = getItem(position);
        if (viewModel.isHeader) {
            viewHolder.txtName.setText(viewModel.name);
            viewHolder.txtName.setTextColor(Color.parseColor("#007AFF"));
            viewHolder.txtName.setTextSize(18);
            viewHolder.txtName.setTypeface(null, Typeface.BOLD);
            viewHolder.checkBox.setVisibility(View.GONE);
            viewHolder.featureSwitch.setVisibility(View.GONE);
        } else {
            viewModel.setChecked(getVariantFor(position).isChecked());
            viewHolder.txtName.setText(viewModel.name);
            viewHolder.txtName.setTextColor(Color.parseColor("#000000"));
            viewHolder.txtName.setTextSize(17);
            viewHolder.txtName.setTypeface(null, Typeface.NORMAL);
            boolean showToggle = this.displayMode == DisplayMode.FEATURE_FLAGS;
            viewHolder.featureSwitch.setVisibility(showToggle ? View.VISIBLE : View.GONE);
            viewHolder.checkBox.setVisibility(showToggle ? View.GONE : View.VISIBLE);
            viewHolder.checkBox.setChecked(viewModel.isChecked());
            viewHolder.featureSwitch.setChecked(viewModel.isChecked());
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if(customFilter == null) {
            customFilter = new CustomFilter();
        }

        return customFilter;
    }

    private class CustomFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            List<ListViewModel> filtered = new ArrayList<>();
            List<Experiment> experiments = getItemsForDisplayMode();
            for (Experiment ex : experiments) {
                filtered.addAll(ex.generateListViewItems(constraint));
            }

            filterResults.count = filtered.size();
            filterResults.values = filtered;

            return filterResults;
        }

        /**
         * Notify about filtered list to ui
         * @param constraint text
         * @param results filtered result
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            dataSetFiltered = (List<ListViewModel>) results.values;
            notifyDataSetChanged();
        }
    }
}

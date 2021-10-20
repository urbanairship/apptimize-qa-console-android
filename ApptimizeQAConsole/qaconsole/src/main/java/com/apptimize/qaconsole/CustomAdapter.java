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
import android.widget.TextView;

import com.apptimize.ApptimizeTestInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomAdapter extends ArrayAdapter<ListViewModel> implements Filterable {
    private List<Experiment> experiments;
    private List<ListViewModel> dataSetFiltered;
    private CustomFilter customFilter;

    // View lookup cache
    private static class ViewHolder {
        TextView txtName;
        CheckBox checkBox;
        Boolean isHeader;
    }

    public CustomAdapter(Context context, List<Experiment> experiments) {
        super(context, R.layout.apptimize_row_item, new ArrayList<ListViewModel>());
        this.experiments = experiments;
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

    public void setTestInfo(Map<String, ApptimizeTestInfo> testInfo) {
        for (Experiment ex : experiments) {
            ApptimizeTestInfo info = testInfo.get(ex.name);
            if (info == null) {
                ex.selectVariant((long) -1);
                continue;
            }
            ex.selectVariant(info.getEnrolledVariantId());
        }
    }

    public Set<Long> getAllCheckedVariants() {
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
        experiment.selectVariant(viewModel.id);
    }

    public void reset() {
        List<ListViewModel> models = new ArrayList<>();
        for (Experiment ex : experiments) {
            models.addAll(ex.generateListViewItems(null));
        }
        this.dataSetFiltered = models;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.apptimize_row_item, parent, false);
            viewHolder.txtName = (TextView) convertView.findViewById(R.id.txtName);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
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
            viewHolder.checkBox.setVisibility(View.INVISIBLE);
        } else {
            viewModel.setChecked(getVariantFor(position).isChecked());
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            viewHolder.checkBox.setChecked(viewModel.isChecked());
            viewHolder.txtName.setText(viewModel.name);
            viewHolder.txtName.setTextColor(Color.parseColor("#000000"));
            viewHolder.txtName.setTextSize(17);
            viewHolder.txtName.setTypeface(null, Typeface.NORMAL);
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

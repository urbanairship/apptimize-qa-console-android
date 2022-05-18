package com.apptimize.qaconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Experiment implements Comparable<Experiment> {
    private static final String FEATURE_FLAG_VARIANT_NAME = "On State";

    public final String name;
    public final Long id;
    private List<Variant> variants;

    Experiment(Map<String, Object> aDataModel) {
        id = (Long) aDataModel.get("experimentId");
        name = (String) aDataModel.get("experimentName");
        variants = new ArrayList<>();
    }

    Experiment(String name, Long id) {
        this.id = id;
        this.name = name;
        variants = new ArrayList<>();
    }

    public void addVariant(Variant variant) {
        variants.add(variant);
        sortVariants();
    }

    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public List<ListViewModel> generateListViewItems(CharSequence filter) {
        String filterString;
        if (filter == null) {
            filterString = "";
        } else {
            filterString = filter.toString().toLowerCase();
        }

        if (!this.name.toLowerCase().contains(filterString)) {
            return new ArrayList<>();
        }

        List<ListViewModel> result = new ArrayList<>();
        result.add(new ListViewModel(this));

        for (Variant variant : variants) {
            result.add(new ListViewModel(variant));
        }

        return result;
    }

    public Long getCheckedVariantId() {
        for (Variant v : variants) {
            if (v.isChecked()) {
                return v.id;
            }
        }

        return null;
    }

    public boolean hasVariant(Long id) {
        return variantWith(id) != null;
    }

    public void selectVariant(Long id) {
        for (Variant v : variants) {
            v.setChecked(v.id.equals(id));
        }
    }

    public Variant variantWith(Long id) {
        Variant result = null;
        for (Variant v : variants) {
            if (v.id.equals(id)) {
                result = v;
                break;
            }
        }

        return result;
    }

    public boolean isFeatureFlag() {
        return variants.size() == 1 &&
                FEATURE_FLAG_VARIANT_NAME.equals(variants.get(0).name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Experiment)) {
            return false;
        }

        Experiment that = (Experiment) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private void sortVariants() {
        Collections.sort(variants, new VariantsNameComparator());
    }

    @Override
    public int compareTo(Experiment o) {
        return o.id.compareTo(this.id);
    }

    private static class VariantsNameComparator implements Comparator<Variant> {
        private static final String originalName = "original";

        @Override
        public int compare(Variant o1, Variant o2) {
            if (o1.name.equals(originalName)) {
                return -1;
            } else if (o2.name.equals(originalName)) {
                return  1;
            }

            return o1.name.compareTo(o2.name);
        }
    }
}

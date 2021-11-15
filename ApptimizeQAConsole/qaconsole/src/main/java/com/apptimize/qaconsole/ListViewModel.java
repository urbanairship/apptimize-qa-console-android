package com.apptimize.qaconsole;

public class ListViewModel {
    public final boolean isHeader;
    public final String name;
    public final Long id;
    private boolean isChecked = false;

    ListViewModel(Experiment experiment) {
        this.isHeader = true;
        this.id = experiment.id;
        this.name = experiment.name;
    }

    ListViewModel(Variant variant) {
        this.isHeader = false;
        this.id = variant.id;
        this.name = variant.name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}

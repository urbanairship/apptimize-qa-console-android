package com.apptimize.qaconsole;

import java.util.Map;
import java.util.Objects;

public class Variant {
    public final String name;
    public final Long id;
    private boolean checked;

    Variant(Map<String, Object> aDataModel) {
        id = (Long) aDataModel.get("variantId");
        name = (String) aDataModel.get("variantName");
        this.checked = false;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Variant)) {
            return false;
        }

        Variant variant = (Variant) o;
        return id.equals(variant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}




